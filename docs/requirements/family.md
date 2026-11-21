# Family

Requirements and design for the use of the application by a family.

## Requirements

### Day of the members
`req~family-day~1`

The user sees a day with one column per member of the family: the tasks of the member for that day, to be ticked off there, and the day as a calendar with the tasks of the member that have a time and with the calendar that is kept next to the list of the member.
A member is a list, such as one in a directory of the repository of the family, and a task belongs to the member whose context it has.
The view shows today and moves to the days before and after it; it is operated by touch.

Covers:

* `feat~family~1`

Needs: dsn

### Stars for completed tasks
`req~stars~1`

A member who completes a task gets its stars: one, or the number that the task states.
A ranking compares the stars that the members got on a day, in a week, a month, a quarter or a year.

Covers:

* `feat~family~1`

Needs: dsn

## Design

### Stars and ranking
`dsn~stars~1`

The tag `stars:` holds the stars of a task as a number from zero on; `Task.stars` reads it and gives one without the tag and for any other value.
`Family.ranking` adds up, per member, the stars of the completed tasks whose completion date lies in the period that holds a day (`RankingPeriod`: the day, the week from Monday, the month, the quarter or the year), and orders the members by them; members with the same stars share a place.
The completed tasks stay in the file, so the ranking is derived from it and needs no file of its own; a completed task without a completion date counts for no period.
`TodoController.toggle` therefore writes the completion date of a task of a member also where the settings write no dates.
The next occurrence of a recurring task keeps the tag.

Covers:

* `req~stars~1`
* `adr~stars-tag~1`

Needs: impl, utest

### Members, their tasks and appointments
`dsn~family-day~1`

The members are the names of the lists of `SharedLists` in their order (`SharedLists.names`), which `TodoController.members` holds; `Family.memberOf` gives the first member whose context, or a level below it, a task has.
`Family.appointments` gives the tasks of a member with a fixed time (`at:`) whose due date is the day, completed or not, each for its duration or for 30 minutes.
`Family.todos` gives the other tasks of a member for a day in the order of the list: the open ones that are due on the day, on the current day also the overdue ones and those without a due date, without those before their start date; behind them the tasks that were completed on the day.
`SharedLists.readCompanionOf` reads the `calendar.txt` next to the list of a member, whose blocks `TodoController.blocksOf` holds; they are read with the configuration, so that a synchronization updates them.

Covers:

* `req~family-day~1`

Needs: impl, utest

### Family view
`dsn~family-view~1`

`FamilyCalendar` is the view *Family*, whose switch stands last and is there only while the tasks have members (`TodoController.views`); settings that name the view without members show the list.
Its first row has the buttons for the day before and the day after, the day, which says so when it is today, *Today* while another day is shown, the periods of the ranking, and the ranking: the members by their place, each with its stars, the first place in the primary color unless nobody has a star.
On a narrow screen, the periods and the ranking wrap below the day, so that all members are seen there, of whose columns a phone shows two.
Below it, each member has a column, at least 200 dp wide, so that more columns than fit scroll sideways: the name and a button that opens the dialog of a new task with the context of the member.
Below the names stand the tasks of the day, each with a box to tick it off, its text, which opens its dialog, and its stars; an overdue task says since when in the color of an error, and a completed one is struck through.
The tasks take at most two fifths of the height and scroll beyond that; the calendar below shares the rest among its hours, at least from 07:00 to 21:00, as the week does, with a line at the current minute of today.
The calendar of a member draws the blocks of the `calendar.txt` of the list first and the appointments over them (`DayColumn`), each with its start and its end, as the hours are too low to read the end from; a click on an appointment opens its task.
The buttons that float above the other views, for a new task and for the search, are left out: they would cover the calendar, each member has a button for a new task, and there is nothing to search.
The answers to the flow: ticking a task is what is done most, so the tasks stand above the calendar; it takes effect at once and is undone by the same box; a failure to write is shown as any other; the period is kept in the settings of the device as `rankingPeriod`, the day is not, as the view is to show today; on a phone, everything is reached by touch.
The filter does not apply, as the view shows the day of everybody: on a wide screen, the filter panel and the field of the search are left out, and the columns take the width.
With Ctrl, 8 shows the view.

Covers:

* `req~family-day~1`
* `req~stars~1`

Needs: impl, utest
