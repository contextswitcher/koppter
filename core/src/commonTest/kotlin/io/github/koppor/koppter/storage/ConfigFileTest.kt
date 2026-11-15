package io.github.koppor.koppter.storage

import io.github.koppor.koppter.todotxt.BoardColumn
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

    // [utest->dsn~config-defaults~1]
    @Test
    fun aConfigurationWithoutTheKeyHasTheDefaultColumnsAndOneWithTheKeyAloneHasNone() {
        assertEquals(ConfigFile.DEFAULT_BOARD_COLUMNS, ConfigFile.boardColumns(""))
        assertEquals(ConfigFile.DEFAULT_BOARD_COLUMNS, ConfigFile.boardColumns("other: value\n"))
        assertEquals(ConfigFile.DEFAULT_SHARE_ACTIONS, ConfigFile.shareActions("other: value\n"))

        val none = ConfigFile.withBoardColumns("", emptyList())
        assertEquals("# Configuration of koppter for the todo.txt file in this directory\nboard-columns:\n", none)
        assertEquals(emptyList(), ConfigFile.boardColumns(none))
        // The other key is still missing.
        assertEquals(ConfigFile.DEFAULT_SHARE_ACTIONS, ConfigFile.shareActions(none))
    }
}
