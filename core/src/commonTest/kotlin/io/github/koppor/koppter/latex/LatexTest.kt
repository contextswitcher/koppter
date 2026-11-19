package io.github.koppor.koppter.latex

import io.github.koppor.koppter.todotxt.Task
import io.github.koppor.koppter.todotxt.TodoFile
import kotlin.test.Test
import kotlin.test.assertEquals

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
                LatexTodo("main.tex", 4, "Check this claim"),
                LatexTodo("main.tex", 6, "Rephrase"),
                LatexTodo("sections/method.tex", 3, "Add the proof"),
                LatexTodo("main.tex", 10, "Answer reviewer A"),
            ),
            Latex.todos("paper/main.tex", files::get),
        )
    }

    // [utest->dsn~latex-todos~1]
    @Test
    fun aTodoThatALineNamesIsLeftOut() {
        val todos = Latex.todos("paper/main.tex", files::get)
        val first = Latex.task(todos[0], "paper")
        assertEquals("Check this claim +paper #latex tex:${Latex.tag(todos[0])}", first.body)
        val file = TodoFile.parse("x 2026-11-19 (A) ${first.body}\n")
        val entries = Latex.entries(mapOf("paper" to todos), file.entries, -1)
        assertEquals(listOf("Rephrase", "Add the proof", "Answer reviewer A"), entries.map { it.task.description.substringBefore(" +") })
        assertEquals(listOf(-1, -2, -3), entries.map { it.index })
        assertEquals(Latex.tag(todos[1]), entries[0].task.tag(Task.LATEX))
    }
}
