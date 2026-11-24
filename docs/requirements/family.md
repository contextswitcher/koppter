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

### Absences from the calendars of a member
`req~absences~1`

A member names, on a device of their own, the addresses at which calendars of theirs are published as iCalendar files.
The family view of every device then shows the times of the events of these calendars in the column of the member as absences, without what the events are.

Covers:

* `feat~family~1`

Needs: dsn

### Busy times from the plan of a member
`req~busy-plan~1`

A member names, on a device of their own, that its plan is theirs.
The family view of every device then shows the times that this plan takes in the column of the member as busy.
It names a task of the list of the member, which the family has anyway, and does not tell what any other task is.

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
Below it stands the day as a calendar with a column per member under the name of the member, at least 120 dp wide, so that a phone shows three members and more columns than fit scroll sideways.
The calendar shares its height among its hours, at least from 07:00 to 21:00, as the week does, with a line at the current minute of today; in a window too low for the day, it scrolls and starts an hour before now.
The calendar of a member draws the blocks of the `calendar.txt` of the list first and the appointments over them (`DayColumn`), each with its start and its end, as the hours are too low to read the end from; in a column narrower than 180 dp, the title stands first and the times on the line below, so that a short block still says what it is and a long one shows its whole title; a click on an appointment opens its task.
The text of a block stays in view while the start of the block is scrolled out of it, so that a block that runs now says what it is.
The tasks of the day stand apart from the calendar, as a column of the calendar is too narrow for their texts: per member the name, a button that opens the dialog of a new task with the context of the member, and the tasks.
A task has a box to tick it off, its text, which opens its dialog, and its stars; an overdue task says since when in the color of an error, and a completed one is struck through.
In a window that is higher than wide, or narrower than 600 dp, the tasks stand below the calendar, as many members beside each other as have 240 dp each; they take at most two fifths of the height.
In a window that is wider than high, such as a tablet or a phone held across, the height is needed for the hours: the tasks stand beside the calendar in a pane of 320 dp, the members below each other.
Tasks that do not fit scroll, and *More tasks below* says so until the last one is in view.
The buttons that float above the other views, for a new task and for the search, are left out: they would cover the calendar, each member has a button for a new task, and there is nothing to search.
The answers to the flow: the day is looked at first, so the calendar stands first, and ticking a task is what is done most, so the tasks are in view without scrolling, below or beside it; it takes effect at once and is undone by the same box; a failure to write is shown as any other; the period is kept in the settings of the device as `rankingPeriod`, the day is not, as the view is to show today; on a phone, everything is reached by touch.
The filter does not apply, as the view shows the day of everybody: on a wide screen, the filter panel and the field of the search are left out, and the columns take the width.
With Ctrl, 8 shows the view.

Covers:

* `req~family-day~1`
* `req~stars~1`

Needs: impl, utest

### Absences in the calendar.txt of a member
`dsn~absences~1`

The setting `memberCalendars` of the device holds the calendars, one per line as `member = address` (`Family.calendars`).
The tab *File* of the settings dialog edits it below the shared lists, while the tasks have members and the platform reads calendars: one field per member for the address, or for several separated by blanks, so that neither a name nor a notation is typed; the text above the fields says first that the address stays on the device.
A line of a member that is no list at present is kept as it is.
`TodoController.updateAbsences` reads the calendars of each member with `busyTimes` for 28 days from today and passes their times to `CalendarTxt.withAbsences`, which replaces the blocks named `Absent` of the days from today on in the `calendar.txt` of the list of the member and leaves everything else of the file as it is.
`SharedLists.writeCompanionOf` writes the file if it changed and has the storage record it as `Update the absences of @member`, so that the other devices get it with the synchronization and draw it as any block of that file (`dsn~family-view~1`).
`App` does this at its start and every 30 minutes, whatever view is shown.
A member whose name is no list, and a member one of whose calendars cannot be read, are reported in the family view above the columns in the color of an error, the calendar by its host only, as its address is a secret; the absences of such a member stay as they are.
Events of whole days are left out, as `busyTimes` leaves them out.
The answers to the flow: the fields are set once per device and therefore stand last in their tab; Save keeps them and starts the reading, Cancel leaves them as they were; what the reading gave shows in the family view, as its absences or as its failure; the calendars are read off the main thread.

Covers:

* `req~absences~1`
* `adr~absences-in-calendar-txt~1`

Needs: impl, utest

### Busy times in the calendar.txt of a member
`dsn~busy-plan~1`

The setting `planMember` of the device holds the member whose plan the device keeps, and is blank on a shared device.
The tab *File* of the settings dialog chooses it below the calendars of the members, while the tasks have members: under *Busy times from the plan*, a text that says what the family sees and when it is written, and behind *This device belongs to* the choice of *Nobody* or one of the members, of which one is always selected.
`Family.busy` turns the blocks of the plan, those of the `calendar.txt` next to the main file, into what the family learns.
A block of a task of the list of the member becomes `Busy:` with the description of the task, as that list is in the repository of the family anyway; this is how the content of a task is shared, and a task of a child, which is in the list of the child, is therefore always named.
A block of a task of another member is left out.
Any other block, such as one of a task of the main file, becomes `Busy`, joined with those it touches, so that neither the text nor the number of the tasks leaves the device.
The family view shows such a block with the description alone, as `Busy:` is there for the file, to tell which blocks the plan wrote.
`TodoController.updateAbsences` passes them, for 28 days from today, to `CalendarTxt.withAbsences`, which replaces the blocks named `Busy`, or starting with `Busy:`, of the days from today on.
A device without the plan of the member leaves these blocks, and one without a calendar of the member leaves the absences, as two devices of a member may write the two.
The blocks are written with the absences, at the start and every 30 minutes (`dsn~absences~1`), and after the tasks were planned.
A member that is no list is reported as a calendar of such a member is.
`TodoController.updateSettings` remembers a member that the settings named before, and the next writing, which follows at once, takes the busy times of that member back, so that *Nobody* leaves none behind.
`DayColumn` leaves a gap below each block, as a busy time and a named task often follow each other at once and would read as one block.
The answers to the flow: the choice is set once per device and therefore stands last in its tab; Save keeps it and starts the writing, Cancel leaves it as it was; what was written shows in the family view, as does a failure.

Covers:

* `req~busy-plan~1`

Needs: impl, utest
