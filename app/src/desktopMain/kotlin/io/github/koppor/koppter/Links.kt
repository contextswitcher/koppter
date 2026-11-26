package io.github.koppor.koppter

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import org.tinylog.Logger
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.exists
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
                        // A connection that came in while the port was being closed is not answered.
                        if (server.isClosed) return@use
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
    if (System.getProperty("os.name").startsWith("Linux")) return registerLinkSchemeOnLinux()
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

/** The name of the entry that opens the links on Linux; it is hidden from the menu of applications. */
const val LINUX_LINK_ENTRY = "koppter-links.desktop"

/** The desktop entry that makes Linux open the links of the scheme with [program]. */
// [impl->dsn~link-scheme-linux~1]
fun linuxLinkEntry(program: String): String = """
    |[Desktop Entry]
    |Type=Application
    |Name=koppter
    |Exec=${execArgument(program)} %u
    |MimeType=x-scheme-handler/koppter;
    |NoDisplay=true
    |Terminal=false
    |""".trimMargin()

/**
 * [argument] for the `Exec` key of a desktop entry.
 * A plain path stays unquoted: the fallback of `xdg-open` without a desktop environment takes the first
 * word of `Exec` as it stands, quotes included, and then does not find the program.
 * Any other is quoted: a backslash before `"`, `` ` ``, `$` and `\`, whose backslash the string value
 * doubles once more, and `%%` for `%`.
 */
private fun execArgument(argument: String): String =
    if (argument.matches(Regex("[A-Za-z0-9/._+-]+"))) argument
    else argument.map { c ->
    when (c) {
        '"', '`', '$' -> "\\$c"
        '\\' -> "\\\\\\\\"
        '%' -> "%%"
        else -> c.toString()
    }
}.joinToString("", prefix = "\"", postfix = "\"")

/**
 * Makes Linux open the links of the scheme with this program, for the current user: a desktop entry in the
 * applications of the user, and the entry as the handler of the scheme for `xdg-open`.
 * As on Windows, it is written at every start of the installed program, and only when it changed.
 */
// [impl->dsn~link-scheme-linux~1]
private fun registerLinkSchemeOnLinux() {
    // The installed program only (the launcher of jpackage); a start from the build runs `java`.
    val program = ProcessHandle.current().info().command().orElse(null)?.takeIf { it.endsWith("/bin/koppter") } ?: return
    try {
        val dataHome = System.getenv("XDG_DATA_HOME")?.ifBlank { null } ?: "${System.getProperty("user.home")}/.local/share"
        val entry = Path(dataHome, "applications", LINUX_LINK_ENTRY)
        val content = linuxLinkEntry(program)
        if (entry.exists() && entry.readText() == content) return
        entry.createParentDirectories()
        entry.writeText(content)
        for (command in listOf(
            listOf("xdg-mime", "default", LINUX_LINK_ENTRY, "x-scheme-handler/koppter"),
            listOf("update-desktop-database", entry.parent.toString()),
        )) {
            try {
                ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor(10, TimeUnit.SECONDS)
            } catch (e: java.io.IOException) {
                // A desktop without the program still finds the entry by its MIME type.
                Logger.info("${command.first()} is not available: ${e.message}" as Any)
            }
        }
    } catch (e: Exception) {
        Logger.error(e, "The scheme of the links was not registered")
    }
}
