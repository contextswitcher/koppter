package io.github.koppor.koppter.todotxt

import kotlin.test.Test
import kotlin.test.assertEquals

class LintTest {
    private fun findings(text: String, noteExists: (String) -> Boolean = { true }) = lint(TodoFile.parse(text), noteExists).map(Finding::toString)

    // [utest->dsn~lint~1]
    @Test
    fun aFileThatIsReadAsItIsMeantHasNoFindings() {
        assertEquals(
            emptyList(),
            findings(
                """
                (A) Call Mom @phone due:2026-11-20 by:18:00 t:2026-11-18 rec:+1w dur:30m tt:15m at:9:00 deadline:2026-11-27T12:00
                Write the outline +papers>survey @writing id:1 note:k3f.md
                Collect the sources +papers>survey @writing p:1

                x 2026-11-10 Sweep the floor due:tomorrow
                """.trimIndent() + "\n",
            ),
        )
    }

    // [utest->dsn~lint~1]
    @Test
    fun valuesThatAreNotReadAreErrors() {
        assertEquals(
            listOf(
                "todo.txt:1: error: due:tomorrow is not a date as YYYY-MM-DD",
                "todo.txt:1: error: dur:long is not a duration such as 30m or 1h30m",
                "todo.txt:2: error: at:25 is not a time as HH:MM",
                "todo.txt:2: error: rec:weekly is not a recurrence such as 2w or +1m",
                "todo.txt:3: error: p:7 names no task: no line has id:7",
                "todo.txt:3: error: the note lost.md does not exist",
                "todo.txt:5: error: id:1 is the id of line 4 as well",
                "todo.txt:6: error: deadline:soon is not a date as YYYY-MM-DD, or with a time as YYYY-MM-DDTHH:MM",
            ),
            findings(
                """
                Call Mom @phone due:tomorrow dur:long
                Water the plants @home at:25 rec:weekly
                Collect the sources @writing p:7 note:lost.md
                Write the outline @writing id:1
                Write the introduction @writing id:1
                File the tax return @home deadline:soon
                """.trimIndent(),
            ) { it != "lost.md" },
        )
    }

    // [utest->dsn~lint~1]
    @Test
    fun whatIsProbablyMeantOtherwiseIsAWarning() {
        assertEquals(
            listOf(
                "todo.txt: info: open tasks without context, which are in the inbox: lines 5",
                "todo.txt:1: warning: by: has no effect without due:",
                "todo.txt:2: warning: the threshold date 2026-11-20 is after the due date 2026-11-18",
                "todo.txt:3: warning: due: is given more than once; the first one counts",
                "todo.txt:4: warning: due: has no value; a tag has no space after the colon",
                "todo.txt:5: warning: a change of this task writes the line as: Buy a present +Family",
                "todo.txt:6: warning: @Phone is written @phone elsewhere",
            ),
            findings(
                """
                Call Mom @phone by:18:00
                Call Dad @phone t:2026-11-20 due:2026-11-18
                Call the landlord @phone due:2026-11-18 due:2026-11-19
                Call the bank @phone due: 2026-11-18
                 Buy a present +Family
                Call the school @Phone
                """.trimIndent(),
            ),
        )
    }

    // [utest->dsn~lint-formats~1]
    @Test
    fun findingsAreWrittenForAnEditorForGitHubActionsOrAsATable() {
        val findings = listOf(
            Finding(3, Severity.ERROR, "due:tomorrow is not a date as YYYY-MM-DD", "tasks\\todo.txt"),
            Finding(4, Severity.WARNING, "a change of this task writes the line as: Buy tea, 100% \"green\"", "todo.txt"),
            Finding(0, Severity.INFO, "open tasks without context, which are in the inbox: lines 5", "todo.txt"),
        )
        assertEquals(
            listOf(
                "tasks\\todo.txt:3: error: due:tomorrow is not a date as YYYY-MM-DD",
                "todo.txt:4: warning: a change of this task writes the line as: Buy tea, 100% \"green\"",
                "todo.txt: info: open tasks without context, which are in the inbox: lines 5",
            ),
            findings.written(LintFormat.ERRORFORMAT),
        )
        assertEquals(
            listOf(
                "::error file=tasks/todo.txt,line=3::due:tomorrow is not a date as YYYY-MM-DD",
                "::warning file=todo.txt,line=4::a change of this task writes the line as: Buy tea, 100%25 \"green\"",
                "::notice file=todo.txt::open tasks without context, which are in the inbox: lines 5",
            ),
            findings.written(LintFormat.GITHUB_ACTIONS),
        )
        assertEquals(
            listOf(
                "file,line,severity,message",
                "tasks\\todo.txt,3,error,due:tomorrow is not a date as YYYY-MM-DD",
                "todo.txt,4,warning,\"a change of this task writes the line as: Buy tea, 100% \"\"green\"\"\"",
                "todo.txt,,info,\"open tasks without context, which are in the inbox: lines 5\"",
            ),
            findings.written(LintFormat.CSV),
        )
    }
}
