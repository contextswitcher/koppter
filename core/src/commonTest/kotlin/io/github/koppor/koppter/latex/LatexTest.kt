package io.github.koppor.koppter.latex

import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.TodoFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LatexTest {
    private val files = mapOf(
        "paper/main.tex" to """
            \documentclass{article}
            \usepackage{todonotes}
            \begin{document}
            Intro\todo[inline, color={red!20}]{Check this
              claim}. % \todo{commented out}
            50\% done\fxnote*{Rephrase}{this sentence}.
            \input{sections/method}
            \include{sections/method}
            \include{missing}
            \commentontext{marked}{Answer reviewer A}
            \end{document}
        """.trimIndent(),
        "paper/sections/method.tex" to "\\section{Method}\n\n\\TODO{Add the proof}\n\\input{../main}\n",
    )

    // [utest->dsn~latex-todos~1]
    @Test
    fun theTodosOfTheMainFileAndOfTheFilesItTakesInAreRead() {
        assertEquals(
            listOf(
                LatexTodo("main.tex", 4, "Check this claim", "paper/main.tex"),
                LatexTodo("main.tex", 6, "Rephrase", "paper/main.tex"),
                LatexTodo("sections/method.tex", 3, "Add the proof", "paper/sections/method.tex"),
                LatexTodo("main.tex", 10, "Answer reviewer A", "paper/main.tex"),
            ),
            Latex.todos("paper/main.tex", files::get),
        )
        assertNull(Latex.todos("elsewhere/main.tex", files::get))
    }

    // [utest->dsn~latex-rematch~1]
    @Test
    fun aLineFollowsItsTodoWhenTheTextChangesALittle() {
        val old = LatexTodo("main.tex", 4, "Check this claim")
        val gone = LatexTodo("main.tex", 9, "Remove the table")
        val file = TodoFile.parse(
            "(A) ${Latex.task(old, "paper").body} @desk due:2026-12-01\n" +
                "${Latex.task(gone, "paper").body}\n" +
                "x ${Latex.task(LatexTodo("main.tex", 1, "Check that claim"), "paper").body}\n",
        )
        val changed = LatexTodo("main.tex", 5, "Check this claim again")
        val other = LatexTodo("main.tex", 7, "Write the conclusion")
        val result = Latex.rematched(mapOf("paper" to listOf(changed, other)), file.entries)
        assertEquals(1, result.size)
        val (line, task) = result.single()
        assertEquals(0, line.index)
        assertEquals('A', task.priority)
        assertEquals("Check this claim again +paper #latex tex:${Latex.tag(changed)} @desk due:2026-12-01", task.body)
        // A document that was not read leaves its lines alone.
        assertEquals(emptyList(), Latex.rematched(emptyMap(), file.entries))
    }

    // [utest->dsn~latex-open~1]
    @Test
    fun theTemplateOfTheProgramNamesFileAndLine() {
        assertEquals(listOf("code", "--goto", "/my papers/main.tex:12"), Latex.command(" code  --goto {file}:{line} ", "/my papers/main.tex", 12))
    }

    // [utest->dsn~latex-todos~1]
    @Test
    fun aTodoThatALineNamesIsLeftOut() {
        val todos = Latex.todos("paper/main.tex", files::get)!!
        val first = Latex.task(todos[0], "paper")
        assertEquals("Check this claim +paper #latex tex:${Latex.tag(todos[0])}", first.body)
        val file = TodoFile.parse("x 2026-11-19 (A) ${first.body}\n")
        val entries = Latex.entries(mapOf("paper" to todos), file.entries, -1)
        assertEquals(listOf("Rephrase", "Add the proof", "Answer reviewer A"), entries.map { it.task.description.substringBefore(" +") })
        assertEquals(listOf(-1, -2, -3), entries.map { it.index })
        assertEquals(Latex.tag(todos[1]), entries[0].task.tag(Task.LATEX))
    }
}
