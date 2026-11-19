package io.github.koppor.koppter.storage

/** The palette the colors of the application come from. */
enum class ColorTheme(val title: String) { NORD("Nord"), EVERFOREST("Everforest") }

/** Whether the colors are light or dark. */
enum class Appearance(val title: String) { SYSTEM("As the system"), LIGHT("Light"), DARK("Dark") }

/**
 * The name of the commits where neither the settings nor the git configuration name one, as on a phone.
 * The Android application replaces both at its start by the device, so that the history tells the phones apart.
 */
var defaultCommitterName = "koppter"
var defaultCommitterEmail = "koppter@localhost"

data class Settings(
    /** Location of the todo.txt file; blank means the platform default. */
    val todoFile: String = "",
    /** The todo.txt files of the shared lists, one per line; see [SharedLists]. */
    val sharedLists: String = "",
    /** Write the creation date when a task is added and the completion date when it is completed. */
    val writeDates: Boolean = false,
    /** The number of tasks the *Top* view shows. */
    val topCount: Int = 10,
    /** The day (`YYYY-MM-DD`) on which this device last added the tasks of `ice_recur.txt`. */
    val iceRecurLastRun: String = "",
    val theme: ColorTheme = ColorTheme.NORD,
    val appearance: Appearance = Appearance.SYSTEM,
    /**
     * The addresses of the iCalendar files whose events are busy times for planning, one per line.
     * Such an address is a secret, which is why it is a setting of the device and not kept next to the todo.txt file.
     */
    val calendars: String = "",
    /** The filter of the task list as the link that names it, so that it is there again after a restart; blank for the filter that restricts nothing. */
    val filter: String = "",
    /** The name of the view that was shown last, so that it is there again after a restart. */
    val view: String = "LIST",
    /** The name of the attribute of the groups of the list; blank for a list without groups. */
    val listGroup: String = "",
    /** The name of the attribute of the columns of the board; blank for the columns that the user defined. */
    val boardGroup: String = "PRIORITY",
    /** The name of the attribute of the tiles of the overview. */
    val overviewGroup: String = "CONTEXT",
    /** The width of the dialog of a task in dp, where the window allows it. */
    val editWidth: Int = 760,
    /** The name in which git commits are made on this device; blank for that of the git configuration, or [defaultCommitterName]. */
    val committerName: String = "",
    /** The email address in which git commits are made on this device; blank for that of the git configuration, or [defaultCommitterEmail]. */
    val committerEmail: String = "",
    /** The account whose main calendar takes the plan, by its name in Outlook; blank for the default account. */
    val outlookCalendar: String = "",
    /** For how many weeks from today the plan is written to the calendar of Outlook; 0 writes nothing. */
    val outlookWeeks: Int = 3,
    /** The program that opens a TODO of a LaTeX document, as a template of [io.github.koppor.koppter.latex.Latex.command]; blank for the program of the system. */
    val latexEditor: String = "",
) {
    // [impl->dsn~settings-file~1]
    fun toProperties(): Map<String, String> = mapOf(
        TODO_FILE to todoFile,
        SHARED_LISTS to sharedLists,
        WRITE_DATES to writeDates.toString(),
        TOP_COUNT to topCount.toString(),
        ICE_RECUR_LAST_RUN to iceRecurLastRun,
        THEME to theme.name,
        APPEARANCE to appearance.name,
        CALENDARS to calendars,
        FILTER to filter,
        VIEW to view,
        LIST_GROUP to listGroup,
        BOARD_GROUP to boardGroup,
        OVERVIEW_GROUP to overviewGroup,
        EDIT_WIDTH to editWidth.toString(),
        COMMITTER_NAME to committerName,
        COMMITTER_EMAIL to committerEmail,
        OUTLOOK_CALENDAR to outlookCalendar,
        OUTLOOK_WEEKS to outlookWeeks.toString(),
        LATEX_EDITOR to latexEditor,
    )

    companion object {
        private const val TODO_FILE = "todoFile"
        private const val SHARED_LISTS = "sharedLists"
        private const val WRITE_DATES = "writeDates"
        private const val TOP_COUNT = "topCount"
        private const val ICE_RECUR_LAST_RUN = "iceRecurLastRun"
        private const val THEME = "theme"
        private const val APPEARANCE = "appearance"
        private const val CALENDARS = "calendars"
        private const val FILTER = "filter"
        private const val VIEW = "view"
        private const val LIST_GROUP = "listGroup"
        private const val BOARD_GROUP = "boardGroup"
        private const val OVERVIEW_GROUP = "overviewGroup"
        private const val EDIT_WIDTH = "editWidth"
        private const val COMMITTER_NAME = "committerName"
        private const val COMMITTER_EMAIL = "committerEmail"
        private const val OUTLOOK_CALENDAR = "outlookCalendar"
        private const val OUTLOOK_WEEKS = "outlookWeeks"
        private const val LATEX_EDITOR = "latexEditor"

        /** Unknown keys are ignored and missing keys take their default, so that settings files of other versions load. */
        fun fromProperties(properties: Map<String, String>): Settings = Settings(
            todoFile = properties[TODO_FILE].orEmpty(),
            sharedLists = properties[SHARED_LISTS].orEmpty(),
            writeDates = properties[WRITE_DATES].toBoolean(),
            topCount = properties[TOP_COUNT]?.toIntOrNull()?.takeIf { it > 0 } ?: Settings().topCount,
            iceRecurLastRun = properties[ICE_RECUR_LAST_RUN].orEmpty(),
            theme = ColorTheme.entries.firstOrNull { it.name == properties[THEME] } ?: ColorTheme.NORD,
            appearance = Appearance.entries.firstOrNull { it.name == properties[APPEARANCE] } ?: Appearance.SYSTEM,
            calendars = properties[CALENDARS].orEmpty(),
            filter = properties[FILTER].orEmpty(),
            view = properties[VIEW] ?: Settings().view,
            listGroup = properties[LIST_GROUP].orEmpty(),
            boardGroup = properties[BOARD_GROUP] ?: Settings().boardGroup,
            overviewGroup = properties[OVERVIEW_GROUP] ?: Settings().overviewGroup,
            editWidth = properties[EDIT_WIDTH]?.toIntOrNull()?.takeIf { it > 0 } ?: Settings().editWidth,
            committerName = properties[COMMITTER_NAME].orEmpty(),
            committerEmail = properties[COMMITTER_EMAIL].orEmpty(),
            outlookCalendar = properties[OUTLOOK_CALENDAR].orEmpty(),
            outlookWeeks = properties[OUTLOOK_WEEKS]?.toIntOrNull()?.takeIf { it >= 0 } ?: Settings().outlookWeeks,
            latexEditor = properties[LATEX_EDITOR].orEmpty(),
        )
    }
}
