package io.github.koppor.koppter

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UpdateTest {
    // [utest->dsn~restart-to-update~1]
    @Test
    fun aCommitOfTheUpstreamBranchThatTheRunningCommitLacksIsAnUpdate() {
        val origin = Files.createTempDirectory("koppter-origin").toFile()
        val checkout = Files.createTempDirectory("koppter-checkout").toFile()
        // Independent of the git settings of the machine.
        fun commit() = assertNotNull(
            git(origin, "-c", "user.name=Test", "-c", "user.email=test@example.org", "-c", "commit.gpgsign=false", "commit", "--quiet", "--allow-empty", "-m", "Change"),
        )
        assertNotNull(git(origin, "init", "--quiet"))
        commit()
        assertNotNull(git(checkout, "clone", "--quiet", origin.path, "."))
        val running = assertNotNull(git(checkout, "rev-parse", "HEAD"))
        assertEquals(0, commitsBehind(checkout, running))

        commit()
        assertEquals(1, commitsBehind(checkout, running))

        // A pull outside the application leaves the old build running, so the offer stays.
        assertNotNull(git(checkout, "pull", "--quiet", "--no-rebase"))
        assertEquals(1, commitsBehind(checkout, running))

        // Without git or a repository, nothing is offered.
        assertEquals(0, commitsBehind(Files.createTempDirectory("koppter-no-repository").toFile(), running))
    }
}
