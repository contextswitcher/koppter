package io.github.koppor.koppter.storage

import java.io.File
import kotlin.io.path.createDirectory
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import io.github.koppor.koppter.todotxt.LintFormat
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LintFilesTest {
    // [utest->dsn~lint-files~1]
    @Test
    fun theFilesNextToTheTasksAreCheckedAsWell() {
        val directory = createTempDirectory("lint")
        val todo = directory.resolve("tasks.txt")
        todo.writeText("Write the outline @writing note:k3f.md\nRead the survey @reading note:lost.md\n")
        directory.resolve("notes").createDirectory().resolve("k3f.md").writeText("Start with the related work.\n")
        directory.resolve("ice_recur.txt").writeText("# Schedules\nweekly ; day Monday - (B) Plan the week @home\nsometimes - Water the plants @home\n")
        directory.resolve("koppter.yml").writeText("time-maps:\n  \"General\": \"Mon-Fri 08:00-12:00, noon\"\nlimits:\n  \"@reading\": \"max 2h/year\"\n")

        assertEquals(
            listOf(
                "tasks.txt:2: error: the note lost.md does not exist",
                "ice_recur.txt:3: error: the line is not understood",
                "koppter.yml: error: General: \"noon\" is neither days nor times",
                "koppter.yml: error: @reading: \"max 2h/year\" is no limit",
            // A message is not a path: Windows does not allow the colon in one.
            ).map { "$directory${java.io.File.separator}$it" },
            lintFiles(todo).map { it.toString() },
        )
    }

    // [utest->dsn~lint-formats~1]
    @Test
    fun theCommandLineNamesTheFormatAndTheFile() {
        assertEquals(LintFormat.ERRORFORMAT to "todo.txt", lintOptions(emptyList()))
        assertEquals(LintFormat.GITHUB_ACTIONS to "tasks/todo.txt", lintOptions(listOf("--output-format", "github-actions", "tasks/todo.txt")))
        assertEquals(LintFormat.CSV to "todo.txt", lintOptions(listOf("todo.txt", "--output-format=CSV")))
        assertNull(lintOptions(listOf("--output-format", "xml")))
        assertNull(lintOptions(listOf("--output-format")))
        assertNull(lintOptions(listOf("--verbose")))
        assertNull(lintOptions(listOf("todo.txt", "done.txt")))
    }

    // The launcher is not built by Gradle: without this, a new source file or a new version is noticed by its users only.
    // [utest->dsn~lint-jbang~1]
    @Test
    fun theLauncherForJbangNamesAllSourcesAndTheVersionsOfTheBuild() {
        val launcher = File("../.jbang/KoppterLint.kt").readLines()
        val common = File("src/commonMain/kotlin").walk().filter { it.extension == "kt" }.map { "//SOURCES ../core/" + it.invariantSeparatorsPath }.sorted().toList()
        assertEquals(common + "//SOURCES ../core/src/jvmMain/kotlin/io/github/koppor/koppter/storage/LintFiles.kt", launcher.filter { it.startsWith("//SOURCES") })

        val versions = File("../gradle/libs.versions.toml").readLines()
        fun version(name: String) = versions.first { it.startsWith("$name = ") }.substringAfter('"').substringBefore('"')
        assertTrue("//KOTLIN ${version("kotlin")}" in launcher)
        assertTrue("//DEPS org.jetbrains.kotlinx:kotlinx-datetime-jvm:${version("kotlinx-datetime")}" in launcher)
    }
}
