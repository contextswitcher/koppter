package io.github.koppor.koppter

import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class LinksTest {
    // [utest->dsn~link-single-instance~1]
    @Test
    fun aSecondStartHandsItsLinkToTheRunningApplication() {
        val port = LinkPort(Files.createTempDirectory("koppter-links").resolve("data/link.port"))
        // No application runs.
        assertEquals(false, port.pass("koppter://filter?project=Fair"))

        val received = LinkedBlockingQueue<String>()
        val listening = port.listen(received::add)
        assertEquals(true, port.pass("koppter://filter?project=Fair"))
        assertEquals("koppter://filter?project=Fair", received.poll(5, TimeUnit.SECONDS))
        assertEquals(true, port.pass("koppter://filter?context=phone"))
        assertEquals("koppter://filter?context=phone", received.poll(5, TimeUnit.SECONDS))

        listening.close()
    }

    // [utest->dsn~link-single-instance~1]
    @Test
    fun theFileOfAnApplicationThatEndedIsNoRunningApplication() {
        val file = Files.createTempDirectory("koppter-links").resolve("link.port")
        // A port that was listened on and is closed, as after the end of the application.
        file.writeText(ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { it.localPort }.toString())
        assertEquals(false, LinkPort(file).pass("koppter://filter?project=Fair"))
    }

    // [utest->dsn~link-single-instance~1]
    @Test
    fun aFileThatNamesNoPortIsNoRunningApplication() {
        val file = Files.createTempDirectory("koppter-links").resolve("link.port")
        file.writeText("no number")
        assertEquals(false, LinkPort(file).pass("koppter://filter?project=Fair"))
    }

    // [utest->dsn~link-scheme-windows~1]
    @Test
    fun theSchemeIsRegisteredWithTheProgramAndTheLinkAsItsArgument() {
        assertEquals(
            listOf(
                Triple("Software\\Classes\\koppter", "", "URL:koppter"),
                Triple("Software\\Classes\\koppter", "URL Protocol", ""),
                Triple("Software\\Classes\\koppter\\shell\\open\\command", "", "\"C:\\Users\\Demo User\\AppData\\Local\\koppter\\koppter.exe\" \"%1\""),
            ),
            schemeEntries("C:\\Users\\Demo User\\AppData\\Local\\koppter\\koppter.exe"),
        )
    }
}
