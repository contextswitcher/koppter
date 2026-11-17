package io.github.koppor.koppter.storage

import io.github.koppor.koppter.todotxt.BoardColumn
import io.github.koppor.koppter.todotxt.Query
import io.github.koppor.koppter.todotxt.Task
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

// [utest->dsn~config-file~1]
class ConfigFileTest {
    private val columns = listOf(BoardColumn("Now", "due:<=today -@waiting"), BoardColumn("Waiting", "@waiting"))

    @Test
    fun aNewConfigurationHoldsTheColumnsAsQuotedScalars() {
        val config = ConfigFile.withBoardColumns("", columns)

        assertEquals(
            """
            # Configuration of koppter for the todo.txt file in this directory
            board-columns:
              "Now": "due:<=today -@waiting"
              "Waiting": "@waiting"
            """.trimIndent() + "\n",
            config,
        )
        assertEquals(columns, ConfigFile.boardColumns(config))
    }

    @Test
    fun plainAndQuotedScalarsWrittenByHandAreRead() {
        val config = """
            other: value
            board-columns:
              # the day
              Now: due:<=today -@waiting   # comment
              'It''s late': "(>C) \"quoted\" back\\slash"
              a:b: +Project

              "Key: with colon": '@phone'
            more:
              Not a column: x
        """.trimIndent()

        assertEquals(
            listOf(
                BoardColumn("Now", "due:<=today -@waiting"),
                BoardColumn("It's late", "(>C) \"quoted\" back\\slash"),
                BoardColumn("a:b", "+Project"),
                BoardColumn("Key: with colon", "@phone"),
            ),
            ConfigFile.boardColumns(config),
        )
    }

    @Test
    fun replacingTheColumnsKeepsTheRestOfTheConfiguration() {
        val config = "other: value\nboard-columns:\n  Old: +Old\n\n# next\nmore: 1\n"

        assertEquals(
            "other: value\nboard-columns:\n  \"Now\": \"due:<=today -@waiting\"\n  \"Waiting\": \"@waiting\"\n\n# next\nmore: 1\n",
            ConfigFile.withBoardColumns(config, columns),
        )
        // Without columns, the key stays: the user wants none, not the default ones.
        assertEquals("other: value\nboard-columns:\n\n# next\nmore: 1\n", ConfigFile.withBoardColumns(config, emptyList()))
        assertEquals(
            "other: value\n\nboard-columns:\n  \"A \\\"b\\\"\": \"c\\\\d\"\n",
            ConfigFile.withBoardColumns("other: value\n", listOf(BoardColumn("A \"b\"", "c\\d"))),
        )
    }

    @Test
    fun aNumberSignStartsACommentUnlessTheScalarIsQuoted() {
        val config = "board-columns:\n  Doing: \"#doing\"\n  Comment: #doing\n  Both: +Work #comment\n"

        assertEquals(
            listOf(BoardColumn("Doing", "#doing"), BoardColumn("Comment", ""), BoardColumn("Both", "+Work")),
            ConfigFile.boardColumns(config),
        )
    }

    // [utest->dsn~project-contexts~1]
    @Test
    fun aProjectBringsItsContextsAndThoseOfTheLevelsAboveIt() {
        val config = ConfigFile.withEntries("", ConfigFile.PROJECT_CONTEXTS, ConfigFile.parseLines("+Fair = @errands\n+Fair>Booth = @weekend, tools @errands"))
        assertEquals(true, config.contains("project-contexts:\n  \"+Fair\": \"@errands\""))
        val entries = ConfigFile.entries(config, ConfigFile.PROJECT_CONTEXTS).toMap()

        assertEquals(listOf("errands"), ConfigFile.contextsOf(entries, "Fair"))
        assertEquals(listOf("errands", "weekend", "tools"), ConfigFile.contextsOf(entries, "Fair>Booth>Print"))
        assertEquals(emptyList(), ConfigFile.contextsOf(entries, "Fairness"))
    }

    // [utest->dsn~label-rename~1]
    @Test
    fun aRenamedLabelIsRenamedInTheLinksAndInTheContextsOfProjects() {
        val config = ConfigFile.withEntries(
            ConfigFile.withEntries("", ConfigFile.LINKS, ConfigFile.parseLines("+Fair>Booth = https://example.org/booth\n@Fair = https://example.org/place")),
            ConfigFile.PROJECT_CONTEXTS,
            ConfigFile.parseLines("+Fair = @errands, town\n+Fairness = @errands2"),
        )

        val project = ConfigFile.renamed(config, '+', "Fair", "Market")
        assertEquals(listOf("+Market>Booth" to "https://example.org/booth", "@Fair" to "https://example.org/place"), ConfigFile.entries(project, ConfigFile.LINKS))
        assertEquals(listOf("+Market" to "@errands, town", "+Fairness" to "@errands2"), ConfigFile.entries(project, ConfigFile.PROJECT_CONTEXTS))

        val context = ConfigFile.renamed(config, '@', "errands", "shopping")
        assertEquals(listOf("+Fair" to "@shopping @town", "+Fairness" to "@errands2"), ConfigFile.entries(context, ConfigFile.PROJECT_CONTEXTS))
        // A configuration without the label is not written anew.
        assertEquals(config, ConfigFile.renamed(config, '#', "Fair", "Market"))
        assertEquals("links:\n  +Fair: a # kept", ConfigFile.renamed("links:\n  +Fair: a # kept", '@', "Fair", "Market"))
    }

    // [utest->dsn~label-links~1]
    @Test
    fun aLabelHasItsOwnLinkOrThatOfTheNearestLevelAboveIt() {
        val config = ConfigFile.withEntries(
            "",
            ConfigFile.LINKS,
            ConfigFile.parseLines("+Fair = contextswitcher://switch-category/fair\n+Fair>Booth>Print = https://example.org/print?a=1&b=2\n@phone = onenote:https://example.org/Numbers.one#Calls\n#quick ="),
        )
        assertEquals(true, config.contains("links:\n  \"+Fair\": \"contextswitcher://switch-category/fair\""))
        val links = ConfigFile.entries(config, ConfigFile.LINKS).toMap()

        assertEquals("contextswitcher://switch-category/fair", ConfigFile.link(links, "+Fair"))
        assertEquals("contextswitcher://switch-category/fair", ConfigFile.link(links, "+Fair>Booth"))
        assertEquals("https://example.org/print?a=1&b=2", ConfigFile.link(links, "+Fair>Booth>Print"))
        assertEquals("onenote:https://example.org/Numbers.one#Calls", ConfigFile.link(links, "@phone"))
        // The sign belongs to the name: the context "Fair" is not the project, and a project that starts alike is not below it.
        assertEquals(null, ConfigFile.link(links, "@Fair"))
        assertEquals(null, ConfigFile.link(links, "+Fairness"))
        assertEquals(null, ConfigFile.link(links, "#quick"))
    }

    // [utest->dsn~config-defaults~1]
    @Test
    fun aConfigurationWithoutTheKeyHasTheDefaultColumnsAndOneWithTheKeyAloneHasNone() {
        assertEquals(ConfigFile.DEFAULT_BOARD_COLUMNS, ConfigFile.boardColumns(""))
        assertEquals(listOf("Today", "Next", "Waiting", "Someday"), ConfigFile.DEFAULT_BOARD_COLUMNS.map { it.title })
        // A task dropped on a default column changes sides: it loses what the filter excludes.
        assertEquals("Look into a new bike", Query.parse(ConfigFile.DEFAULT_BOARD_COLUMNS[1].expression).applyTo(Task.parse("Look into a new bike @waiting #someday"), LocalDate(2026, 11, 12))?.toLine())
        assertEquals(ConfigFile.DEFAULT_BOARD_COLUMNS, ConfigFile.boardColumns("other: value\n"))
        assertEquals(ConfigFile.DEFAULT_SHARE_ACTIONS, ConfigFile.shareActions("other: value\n"))

        val none = ConfigFile.withBoardColumns("", emptyList())
        assertEquals("# Configuration of koppter for the todo.txt file in this directory\nboard-columns:\n", none)
        assertEquals(emptyList(), ConfigFile.boardColumns(none))
        // The other key is still missing.
        assertEquals(ConfigFile.DEFAULT_SHARE_ACTIONS, ConfigFile.shareActions(none))
    }
}
