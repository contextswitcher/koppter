package io.github.koppor.koppter.storage

/**
 * The palette the colors of the application come from.
 * [dark] is the variant of a palette that has only one, which the setting of the variant does not change; `null` for a palette with both.
 */
enum class ColorTheme(val title: String, val dark: Boolean? = null) {
    NORD("Nord"),
    EVERFOREST("Everforest"),
    MUTED_SLATE("Muted Slate", dark = true),
    DAWN("Dawn", dark = false),
    MATRIX("Matrix", dark = true),
}

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
    /** The width of the calendar of `Today` in dp. */
    val todayCalendarWidth: Int = 400,
    /** The width of the filter panel in dp where it stands beside the list. */
    val filterWidth: Int = 280,
    /** The name in which git commits are made on this device; blank for that of the git configuration, or [defaultCommitterName]. */
    val committerName: String = "",
    /** The email address in which git commits are made on this device; blank for that of the git configuration, or [defaultCommitterEmail]. */
    val committerEmail: String = "",
    /** The account whose main calendar takes the plan, by its name in Outlook; blank for the default account. */
    val outlookCalendar: String = "",
    /** Further accounts of Outlook, separated by blanks, whose main calendars count as busy and are shown, without the plan; see [outlookBusyAccounts]. */
    val outlookBusyCalendars: String = "",
    /** For how many weeks from today the plan is written to the calendar of Outlook; 0 writes nothing. */
    val outlookWeeks: Int = 3,
    /** The program that opens a TODO of a LaTeX document, as a template of [io.github.koppor.koppter.latex.Latex.command]; blank for the program of the system. */
    val latexEditor: String = "",
    /** Whether the projects and contexts of the filter are sent to ContextSwitcher, on the desktop; see [io.github.koppor.koppter.todotxt.SyncSelection]. */
    val contextSwitcherSync: Boolean = false,
    /** The name of the period of the ranking of the family view. */
    val rankingPeriod: String = "WEEK",
    /**
     * The calendars of the members of a family, one per line as `member = address` of an iCalendar file; see [io.github.koppor.koppter.plan.Family.calendars].
     * A setting of the device, as such an address is a secret.
     */
    val memberCalendars: String = "",
    /** How often each application of the device was started from the list of the applications, one per line as `application=number`. */
    val appStarts: String = "",
    /** The member of a family whose plan this device keeps, see [io.github.koppor.koppter.plan.Family.busy]; blank on a device that is shared. */
    val planMember: String = "",
) {
    // [impl->dsn~settings-file~1]
    /**
     * The accounts of Outlook whose main calendars count as busy: that of the plan, `null` for the default account, then the further ones.
     */
    // [impl->dsn~outlook-busy~1]
    val outlookBusyAccounts: List<String?>
        get() {
            val plan = outlookCalendar.trim().ifEmpty { null }
            val further = outlookBusyCalendars.split(Regex("[\\s,;]+")).filter { it.isNotEmpty() && !it.equals(plan, ignoreCase = true) }.distinctBy { it.lowercase() }
            return listOf(plan) + further
        }

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
        TODAY_CALENDAR_WIDTH to todayCalendarWidth.toString(),
        FILTER_WIDTH to filterWidth.toString(),
        COMMITTER_NAME to committerName,
        COMMITTER_EMAIL to committerEmail,
        OUTLOOK_CALENDAR to outlookCalendar,
        OUTLOOK_BUSY_CALENDARS to outlookBusyCalendars,
        OUTLOOK_WEEKS to outlookWeeks.toString(),
        LATEX_EDITOR to latexEditor,
        CONTEXT_SWITCHER_SYNC to contextSwitcherSync.toString(),
        RANKING_PERIOD to rankingPeriod,
        MEMBER_CALENDARS to memberCalendars,
        APP_STARTS to appStarts,
        PLAN_MEMBER to planMember,
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
        private const val TODAY_CALENDAR_WIDTH = "todayCalendarWidth"
        private const val FILTER_WIDTH = "filterWidth"
        private const val COMMITTER_NAME = "committerName"
        private const val COMMITTER_EMAIL = "committerEmail"
        private const val OUTLOOK_CALENDAR = "outlookCalendar"
        private const val OUTLOOK_BUSY_CALENDARS = "outlookBusyCalendars"
        private const val OUTLOOK_WEEKS = "outlookWeeks"
        private const val LATEX_EDITOR = "latexEditor"
        private const val CONTEXT_SWITCHER_SYNC = "contextSwitcherSync"
        private const val RANKING_PERIOD = "rankingPeriod"
        private const val MEMBER_CALENDARS = "memberCalendars"
        private const val APP_STARTS = "appStarts"
        private const val PLAN_MEMBER = "planMember"

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
            todayCalendarWidth = properties[TODAY_CALENDAR_WIDTH]?.toIntOrNull()?.takeIf { it > 0 } ?: Settings().todayCalendarWidth,
            filterWidth = properties[FILTER_WIDTH]?.toIntOrNull()?.takeIf { it > 0 } ?: Settings().filterWidth,
            committerName = properties[COMMITTER_NAME].orEmpty(),
            committerEmail = properties[COMMITTER_EMAIL].orEmpty(),
            outlookCalendar = properties[OUTLOOK_CALENDAR].orEmpty(),
            outlookBusyCalendars = properties[OUTLOOK_BUSY_CALENDARS].orEmpty(),
            outlookWeeks = properties[OUTLOOK_WEEKS]?.toIntOrNull()?.takeIf { it >= 0 } ?: Settings().outlookWeeks,
            latexEditor = properties[LATEX_EDITOR].orEmpty(),
            contextSwitcherSync = properties[CONTEXT_SWITCHER_SYNC].toBoolean(),
            rankingPeriod = properties[RANKING_PERIOD] ?: Settings().rankingPeriod,
            memberCalendars = properties[MEMBER_CALENDARS].orEmpty(),
            appStarts = properties[APP_STARTS].orEmpty(),
            planMember = properties[PLAN_MEMBER].orEmpty(),
        )
    }
}
