package io.github.koppor.koppter

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import org.tinylog.Logger
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.createParentDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

private const val ANSWER = "koppter"
private const val SCHEME_KEY = "Software\\Classes\\koppter"

/**
 * Hands a link from a second start of the application to the one that runs, so that a click on a link shows the tasks in the open window.
 * The running application listens on a port of this computer and writes its number to [file].
 */
// [impl->dsn~link-single-instance~1]
class LinkPort(private val file: Path) {
    /** Hands the link to the running application; `false` if none takes it, which also holds for a file left by an application that ended. */
    fun pass(link: String): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), file.readText().trim().toInt()), 1000)
            socket.soTimeout = 1000
            socket.getOutputStream().apply { write("$link\n".toByteArray()) }.flush()
            // The answer tells that the port is one of this application and not of a program that got the number later.
            socket.getInputStream().bufferedReader().readLine() == ANSWER
        }
    } catch (_: Exception) {
        false
    }

    /** Takes the links of later starts until the returned handle is closed. */
    fun listen(onLink: (String) -> Unit): AutoCloseable {
        // Only this computer reaches the port.
        val server = ServerSocket(0, 5, InetAddress.getLoopbackAddress())
        file.createParentDirectories()
        file.writeText(server.localPort.toString())
        thread(isDaemon = true, name = "links") {
            while (!server.isClosed) {
                try {
                    server.accept().use { socket ->
                        socket.soTimeout = 1000
                        val line = socket.getInputStream().bufferedReader().readLine() ?: return@use
                        onLink(line)
                        socket.getOutputStream().apply { write("$ANSWER\n".toByteArray()) }.flush()
                    }
                } catch (_: Exception) {
                    // A start that sent nothing in time; the next one is served.
                }
            }
        }
        return AutoCloseable { server.close() }
    }
}

/** What the registry of Windows holds for the scheme of the links: key, name of the value, value. */
// [impl->dsn~link-scheme-windows~1]
fun schemeEntries(program: String): List<Triple<String, String, String>> = listOf(
    Triple(SCHEME_KEY, "", "URL:koppter"),
    Triple(SCHEME_KEY, "URL Protocol", ""),
    Triple("$SCHEME_KEY\\shell\\open\\command", "", "\"$program\" \"%1\""),
)

/**
 * Makes Windows open the links of the scheme with this program, for the current user.
 * It is written at every start, so that it follows the program to where an update installs it.
 */
// [impl->dsn~link-scheme-windows~1]
fun registerLinkScheme() {
    if (!System.getProperty("os.name").startsWith("Windows")) return
    // The installed program only: a start from the build would register the launcher of Java.
    val program = ProcessHandle.current().info().command().orElse(null)?.takeIf { it.endsWith("koppter.exe", ignoreCase = true) } ?: return
    try {
        for ((key, name, value) in schemeEntries(program)) {
            Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, key)
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, key, name, value)
        }
    } catch (e: Throwable) {
        Logger.error(e, "The scheme of the links was not registered")
    }
}
