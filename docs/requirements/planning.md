# Planning

Requirements and design of planning: when the open tasks are worked on.
The decision on where the plan is kept is [decision 0019](../decisions/0019-the-plan-is-derived-and-kept-in-calendar-txt.md).

## Requirements

### Plan of the open tasks
`req~plan~1`

On request, the application gives each open task times in which it is to be worked on.
A task with a higher priority gets earlier times, and every task gets the earliest times that are free.
A task takes its duration and is not planned before its start date.
The todo.txt file is not changed by planning.

Covers:

* `feat~planning~1`

Needs: dsn

### Time maps
`req~time-maps~1`

The user defines time maps: named times of the week.
A time map may be restricted to another one, so that general working hours are defined once.
The user gives contexts a time map; the tasks of a context and of the contexts below it are planned in its times.
A task may name a time map of its own, which comes before that of its contexts.

Covers:

* `feat~planning~1`

Needs: dsn

### Colors of the times of a time map
`req~time-map-colors~1`

The times of a time map are green, yellow, red or black.
A task is planned in green times; a task that would otherwise be done after its due date or not before its barrier is planned in yellow times as well, and then in red ones.
Black times are green for the tasks of their map and are not used for the tasks of other maps.

Covers:

* `feat~planning~1`

Needs: dsn

### Painting a time map
`req~time-map-editor~1`

The user paints a time map on the days of a week, from 00:00 to 24:00 in half hours, with green, yellow, red, black or the eraser, and saves or reverts what was painted.
A stroke that starts on its own color erases.
The user picks the dates of a time map from a calendar.

Covers:

* `feat~planning~1`

Needs: dsn

### Barriers
`req~barriers~1`

A task is not planned behind its barrier.
The barrier is the end of the quarter unless the user chooses the end of the week, month or year or a date, in general or for a project or a context.
Of several barriers of a task, the earliest counts.
A task that does not fit before its barrier is reported instead of being planned later.

Covers:

* `feat~planning~1`

Needs: dsn

### Limits
`req~plan-limits~1`

The user limits the time that is planned for the tasks of a project or a context: not more than a time per day or per week, and at least a time per week, which is then planned before tasks with a higher priority.
The user states the shortest part a task of a project or a context is cut into; without that, it is an hour.

Covers:

* `feat~planning~1`

Needs: dsn

### Due dates before priority
`req~deadline-first~1`

A task that would be done after its due date in the order of the priorities is planned before the tasks with a higher priority, so that it is done by its due date.
Of several such tasks, the one due first is planned first.

Covers:

* `feat~planning~1`

Needs: dsn

### Dependencies
`req~dependencies~1`

A task that waits for other tasks is planned after them.

Covers:

* `feat~planning~1`

Needs: dsn

### Fixed times
`req~fixed-time~1`

A task can state the time of the day at which it takes place.
Planning gives it that time and plans the other tasks around it, also on the later days of a task that a schedule adds.

Covers:

* `feat~planning~1`

Needs: dsn

### Time by which a task is due
`req~due-time~1`

A task can state the time of its due date by which it has to be done.
Among the tasks of a due date, the one with the earlier time comes first, and the state of the plan tells whether the task is done by that time.

Covers:

* `feat~planning~1`

Needs: dsn

### Travel time
`req~travel-time~1`

A task can state the time that the way to it takes.
Planning keeps that time free before and after the task.

Covers:

* `feat~planning~1`

Needs: dsn

### Time spent
`req~time-spent~1`

A task can state the time that was worked on it.
Planning gives the task the rest of its duration, and the estimate stays on the task.

Covers:

* `feat~planning~1`

Needs: dsn

### Time tracking
`req~time-tracking~1`

The user starts a clock on a task when beginning to work on it and stops it when pausing or done; the time in between adds to the time spent.
One clock runs at a time, and the task whose clock runs is always visible.

Covers:

* `feat~planning~1`

Needs: dsn

### Time log
`req~time-log~1`

The user can tell afterwards on which day and at which time a task was worked on, and when they were away.
They mark the time away when they leave and end it when they are back.

Covers:

* `feat~planning~1`

Needs: dsn

### Plan as a file
`req~plan-file~1`

The plan is a text file next to the todo.txt file, so that the history of a git repository tells what a new plan changed.
The plan is also written as an iCalendar file that calendar programs import; its events are tentative.

Covers:

* `feat~planning~1`

Needs: dsn

### Tasks with the same text in the plan
`req~plan-names~1`

Open tasks with the same text, such as the two occurrences of a recurring task in a week, each have their own times and their own state in the plan.
What tells them apart is part of the plan only: the todo.txt file is not changed for it.

Covers:

* `feat~planning~1`

Needs: dsn

### Busy times of calendars
`req~busy-times~1`

Nothing is planned into the times of the events of the calendars that the user names.
A calendar is named by the address at which its service publishes it as an iCalendar file.
The application only reads the calendars.

Covers:

* `feat~planning~1`

Needs: dsn

### Sideline events of the calendars
`req~sideline~1`

The user names words; an event of the calendars whose title contains one of them stays in the calendar, but tasks are planned into its time.
For a repeating event, this holds for all its occurrences.

Covers:

* `feat~planning~1`

Needs: dsn

### Plan in the calendar of Outlook
`req~outlook-calendar~1`

The plan of the next weeks, three unless configured otherwise, is written into the main calendar of an account of the classic desktop Outlook, the default account unless configured otherwise.
The appointments are private and tentative, and their subjects end with `[koppter]`.
A new plan replaces the appointments that koppter wrote before; appointments that koppter did not write are never changed or deleted.
Planning takes the appointments of that calendar as busy times, and those of the main calendars of further accounts that the user names, such as a second employer; the plan is written to none of these.
The user removes all appointments that koppter wrote with one action, after confirming their number.

Covers:

* `feat~planning~1`
* `feat~outlook~1`

Needs: dsn

### Appointments of Outlook in the calendar
`req~outlook-appointments~1`

The calendar views show the appointments of the calendar of Outlook next to the blocks of the plan, in the colors that their categories have in Outlook.
A click on an appointment opens it in Outlook or puts it on the sideline, so that it stays in the calendar and tasks are planned into its time; for a recurring appointment, this holds for the whole series.

Covers:

* `feat~planning~1`
* `feat~outlook~1`

Needs: dsn

### Events of the calendars in the calendar views
`req~calendar-events~1`

The calendar views show the events of the calendars with busy times next to the appointments of Outlook and the blocks of the plan.
Entries that take the same time stand side by side within the width of the day, as in the day view of Outlook.
The user hides a calendar in the calendar views and shows it again; the choice is kept on the device, and planning keeps the times of a hidden calendar free all the same.

Covers:

* `feat~planning~1`

Needs: dsn

### Appointments of Outlook on the devices without Outlook
`req~outlook-mirror~1`

A device without Outlook, such as a phone, shows the appointments of the calendar of Outlook in the calendar views and plans around them, as far as a device with Outlook copied them.
The copy is made when the device with Outlook plans, if its user chose so in the settings, as it puts the subjects of the appointments into a file next to the todo.txt file, which the synchronization passes on.
The copy holds the times and the subjects of the appointments that planning counts as busy, for a year from that plan; on the device without Outlook they are shown read-only, without the colors of their categories.

Covers:

* `feat~planning~1`
* `feat~outlook~1`

Needs: dsn

### Views of the plan
`req~agenda~1`

The user sees what is planned for today and the days after it, together with the tasks that are due today or overdue but not planned for today.
The user sees the plan of a week as a calendar.

Covers:

* `feat~planning~1`

Needs: dsn

### State of the plan on the task
`req~plan-state~1`

Once there is a plan, each open task tells by the color of a calendar icon how its plan relates to its due date: green if it ends before, yellow if it ends on the due date, red if it ends later or the task is not planned.

Covers:

* `feat~planning~1`

Needs: dsn

## Design

### Time maps
`dsn~time-maps~1`

`TimeMaps` reads the definitions: a definition is a list of rules separated by commas, and a rule is days (`Mon`, `Mon-Fri`) followed by times (`08:00-12:00`).
A rule without days holds on every day, a rule without times for the whole day.
A definition that starts with the name of another map gives the times that both maps have.
The first map is the one for tasks without a map; without any map, the times are Monday to Friday from 09:00 to 17:00.
What is neither days nor times is reported and otherwise ignored.

Covers:

* `req~time-maps~1`

Needs: impl, utest

### Time maps on dates
`dsn~time-maps-on-dates~1`

A name of a time map followed by dates, such as `General 2026-12-24..2027-01-06 2027-04-03`, gives a definition of that map on these dates, for example for a vacation or for holidays.
On such a date it replaces the definition without dates; of two entries with the date, the later one holds.
The definition `none` leaves no time on these dates.
A map that has only entries with dates has no time on the other dates.
What is not a date is reported and otherwise ignored.
*Paint* edits each entry by itself.

Covers:

* `req~time-maps~1`

Needs: impl, utest

### Holidays of a state
`dsn~holidays~1`

*Add holidays* in the plan dialog lists the German states.
Choosing one adds an entry of the first map with the public holidays of that state from today to the end of the next year and the definition `none`.
`Holidays` computes them from the date of Easter; holidays of single communities only are left out.

Covers:

* `req~time-maps~1`

Needs: impl, utest

### Configuration of planning
`dsn~plan-configuration~1`

`koppter.yml` holds the time maps under the key `time-maps`, the time maps of contexts under `context-time-maps`, the barriers under `barriers` and the limits under `limits`, each as a map of names to values.
`PlanConfig.mapOf` gives the time map of the first context of a task that has one, where a context has the map of the nearest context above it unless it has its own.
`PlanConfig.barrierOf` gives the earliest barrier among those of the projects and contexts of a task, else the one of the key `default`, else the end of the quarter.
A barrier is `week`, `month`, `quarter`, `year` or a date; the first four name the end of the current one.

Covers:

* `req~time-maps~1`
* `req~barriers~1`

Needs: impl, utest

### Time map of a task
`dsn~task-time-map~1`

The tag `map:` names the time map of a task, such as `map:Evenings`.
`PlanConfig.mapOf` gives it before the time map of the contexts of the task; a name that is no time map is left out, so that the task is planned in the map of its context.

Covers:

* `req~time-maps~1`
* `adr~task-time-map~1`

Needs: impl, utest

### Planner
`dsn~planner~1`

`plan` goes through the open tasks in the order of the task list and gives each the earliest free times of its time map, from now or from its start date on.
A task takes its duration, half an hour without one.
A free time that is too short for the task takes a part of it; no part is shorter than the block of the task, so that a task shorter than two blocks is not cut.
Times that are passed in as busy and the blocks of the tasks planned before are not free.
After a busy time, a block starts on a quarter of an hour; after another block, it starts at once.
A task whose duration does not fit up to its barrier gets no block and is returned as not planned.
A task whose start date lies behind its barrier is not due for planning: it and the tasks that wait for it are left out and not returned.

Covers:

* `req~plan~1`
* `req~barriers~1`

Needs: impl, utest

### Colors of the times of a time map
`dsn~time-map-colors~1`

A rule of a time map definition may name a color with the word `yellow`, `red`, `black` or `green`; without one, its times are green, so that the definitions before the colors keep their meaning.
A later rule paints over the times of an earlier one on the days that both name.
`TimeMaps.spans` gives the times up to a color: green and black, then yellow as well, then red as well; it leaves out the black times of all maps other than the map and the maps it is restricted to.
A map restricted to another one takes the times of that map in all its colors.
`plan` plans with green times first; a task that ends after its due date although it was moved to the front, or a task that is not planned, gets the yellow times as well and the plan is made again, and then the red times.

Covers:

* `req~time-map-colors~1`
* `adr~time-map-colors~1`

Needs: impl, utest

### Painting a time map
`dsn~time-map-editor~1`

The plan dialog lists the time maps, each with its name, dates and definition; a click on one, or on *Edit*, which shows while the pointer is over it, opens `TimeMapEditor`, and *Add time map* opens it for a new map.
The editor edits the name, the dates and the definition, which can be typed or painted, and deletes the map.
`TimeMapEditor` shows the cells of `TimeMaps.cells`, a quarter of an hour each, one column per day from Monday on, and paints the half hours that a press or a drag touches with the chosen color, or clears them with the eraser; quarters of an hour are typed.
A press on a half hour that has the chosen color already clears what the stroke touches, so that the same color undoes a stroke without a change to the eraser.
While the mouse rests on a painted cell, a label beside the pointer names the day and the begin and end of the block of that color around it, such as `Sat 09:00-11:00`; a phone has no hover and shows no label.
The button with the calendar in the field of the dates opens a calendar whose weeks start on Monday; *OK* puts the chosen date into the field at the cursor, or in place of the selected text, and leaves the cursor behind it, so that `..` or a space for a range or a list is typed next; *Cancel* leaves the field as it was.
`TimeMaps.definition` writes the cells: one rule per color and per group of days with the same times, the days as ranges such as `Mon-Fri`, and without days if all seven have these times.
A typed definition is shown on the cells, and painting writes the definition.
A map without any painted cell has no definition, as a definition without rules holds all the time, and cannot be saved, except on dates, where it is `none`; a map restricted to another one keeps the name of that map in front.
A map needs a name without spaces, and a name with dates that no other map has; what `TimeMaps` does not understand is shown, but does not prevent saving, as the map that a definition names may follow.
Saving or deleting a map writes the time maps to `koppter.yml` at once, without planning; the line of the map is replaced and the other lines stay as they were typed.

Covers:

* `req~time-map-editor~1`

Needs: impl, utest

### Limits
`dsn~plan-limits~1`

A limit is a line of `koppter.yml` under the key `limits`: `default`, `+project` or `@context`, which includes the contexts below it, to rules separated by commas.
`block 1h` states the shortest part: `PlanConfig.blockOf` gives the longest of the projects and contexts of a task, else the one of `default`, else an hour.
`max 2h/day` and `max 5h/week` state a budget: the planner counts the minutes it plans for the tasks of the key per day and per week from Monday and gives a task on a day what all its limits still allow.
`min 1h/week` states a minimum: before the tasks get their times in their order, the tasks of the key get that time in each week, the highest in the order first.
So that the minimum does not spread the tasks over more weeks than they need, `plan` first plans without minimums and reserves the minimum only up to the last day of the tasks of the key in that plan, or up to their barrier if one of them does not fit.
A rule that is not understood is reported.

Covers:

* `req~plan-limits~1`

Needs: impl, utest

### Dependencies
`dsn~dependencies~1`

As topydo writes it, a task with the tag `id:` waits for the open tasks whose tag `p:` has the same value.
The planner takes the tasks in the order of the task list, but each after the tasks it waits for, which thereby come as early as the task that waits.
A task starts at the end of the last block of the tasks it waits for, and is not planned if one of them is not.
Tasks that wait for each other are reported and planned as if the one that closes the circle did not wait.

Covers:

* `req~dependencies~1`
* `adr~dependency-tags~1`

Needs: impl, utest

### Fixed times
`dsn~fixed-time~1`

The tag `at:` holds a time of the day as `H:MM` or `HH:MM`; `Task.at` reads it, and the edit dialog has a field for it that rejects what is no time.
The planner gives an open task with that tag one block before it plans the others: on the due date, else on the start date, else today, from that time on for the duration of the task.
The block ignores the time maps, the busy times, the barrier and the limits, but counts for the budgets of its project and contexts; a block whose end has passed is left out, and the task is not reported as not planned.
`iceRecurOccurrences` gives the tasks that the entries of `ice_recur.txt` add on the days of a year after today; those with the tag get their block on their day, unless the list has a task with that text at that time already.
The plan holds these blocks up to the last day on which a task is planned, and for four weeks at least.

Covers:

* `req~fixed-time~1`
* `adr~fixed-time-tag~1`

Needs: impl, utest

### Time before which a task is not planned
`dsn~not-before-time~1`

The tag `after:` holds a time of the day as `H:MM` or `HH:MM`; `Task.after` reads it.
The planner starts a task on its threshold date at that time; on a later day, and for a task without a threshold date, the tag has no effect, so that a postponed task is planned from the start of its time map again the next day.
The next occurrence of a recurring task drops the tag, as it holds a time of a day that has passed.

Covers:

* `req~android-block-notification~3`
* `adr~not-before-tag~1`

Needs: impl, utest

### Time by which a task is due
`dsn~due-time~1`

The tag `dueby:` holds a time of the day as `H:MM` or `HH:MM`; `Task.by` reads it, and the edit dialog has a field for it that rejects what is no time.
`listOrder` puts, among the tasks of a due date, the one with the earlier time first and those without a time last, which is the order in which the planner gives the tasks their times.
`CalendarTxt.state` counts a task as late if its last block lies on the due date and ends after that time.

Covers:

* `req~due-time~1`
* `adr~due-time-tag~1`
* `adr~due-time-tag-name~1`

Needs: impl, utest

### Due dates before priority
`dsn~deadline-first~1`

`plan` plans in the order of the task list first.
A task with a due date that the plan ends after its due date and the time of the tag `dueby:`, or does not plan, is moved to the front of the order, and `plan` plans again.
The moved tasks stand in the order of their due dates, and the tasks they wait for are planned before them.
This repeats until no further task ends late; a task that ends late although it was moved stays where it is, and its state of the plan shows that it is late.
A task with a fixed time is not moved.

Covers:

* `req~deadline-first~1`

Needs: impl, utest

### Travel time
`dsn~travel-time~1`

The tag `tt:` holds a time in the form of the tag `dur:`; `Task.travel` reads it, and the edit dialog has a field for it that rejects what is no time.
The planner gives a task with that tag a free time that holds the way there, the task and the way back, and does not cut the task into parts.
A task with a fixed time gets the ways before and after its block, whatever is planned or busy there.
The ways are blocks of their own, marked as travel: the plan shows them as `Travel:` followed by the text of the task, and they do not count for the limits of the task.

Covers:

* `req~travel-time~1`
* `adr~travel-time-tag~1`

Needs: impl, utest

### Time spent
`dsn~time-spent~1`

The tag `spent:` holds a time in the form of the tag `dur:`; `Task.spent` reads it, and the edit dialog has a field for it that rejects what is no time.
The planner gives a task its duration without that time, and the default duration if nothing is left, as an open task is not done.
A task with a fixed time keeps its length.
In the agenda, a block of today has a button that adds the length of the block to the tag through `Task.withSpent`, once per block.
The next occurrence of a recurring task has no time spent, and the check of a todo.txt file reads the tag as it reads `dur:`.

Covers:

* `req~time-spent~1`
* `adr~time-spent-tag~1`

Needs: impl, utest

### calendar.txt
`dsn~calendar-txt~1`

`CalendarTxt.write` writes the plan in the format of calendar.txt: one line per day from today to the last planned day, with the date, the ISO week, the weekday and, after two blanks, the blocks of the day as `HH:MM-HH:MM text`, separated by a full stop and a blank.
The text of a block is the description of the task with its projects and contexts, or the name of `CalendarTxt.names` for a task that shares that text with another one.
Lines of days before today and lines that are no day stay as they are.
`CalendarTxt.planned` reads the blocks from today on; `CalendarTxt.state` compares the day of the last block of a task with its due date.

Covers:

* `req~plan-file~1`
* `adr~plan-in-calendar-txt~1`

Needs: impl, utest

### iCalendar export
`dsn~ics-export~1`

`Ics.write` writes the blocks of the plan as events of an iCalendar file, `calendar.ics` next to the todo.txt file.
An event is tentative (`STATUS` and the busy status that Outlook reads), its times are local times without a time zone, and its id is derived from the day, the start and the text of the block, so that the same plan gives the same file.

Covers:

* `req~plan-file~1`

Needs: impl, utest

### Busy times
`dsn~busy-times~1`

`busyTimes` reads the events of an iCalendar file and gives the times they take by day, in the time zone of the device.
Events of whole days, canceled events, events marked as free and the blocks that the application exported itself are left out.
Repeating events are followed for the frequencies daily, weekly, monthly and yearly with interval, end, count, days of the week, days of the month and excluded dates.
A time zone that the platform does not know by name counts as that of the device.
An occurrence of a repeating event that was moved counts at its old and at its new time, as the entries that move occurrences are not matched to their series.
The addresses of the calendars are a setting of the device, as they are secrets; the plan dialog edits them, one per line, and a `#` at the start of a line or after a blank starts a comment.
`fetchCalendar` reads an address over HTTP or HTTPS; `TodoController.planWithCalendars` reads all calendars for a year from today, reports those it could not read by their host and plans with the others.

Covers:

* `req~busy-times~1`

Needs: impl, utest

### Sideline
`dsn~sideline~1`

`koppter.yml` lists the words under the key `sideline`, each as a key with an empty value; the plan dialog edits them one per line.
`sidelined` compares them with the title of an event ignoring case; `busyTimes` leaves out an event whose `SUMMARY` matches, and with it all occurrences of its rule.
`OutlookCalendar.busyTimes` does the same with the subject of an appointment, as far as AnchorHub passes it in the field `subject`.

Covers:

* `req~sideline~1`

Needs: impl, utest

### Appointments of Outlook in the calendar views
`dsn~outlook-appointments~1`

`Week` and `Today` ask through `CalendarAppointments.onShow` for the appointments of the days they show; `TodoController.showAppointments` reads them with `ankd outlook-busy` and `App` reads them again every two minutes.
`DayColumn` draws those with a start time (`OutlookCalendar.onDay`, cut at midnight) at their times, side by side with what overlaps them (`dsn~calendar-lanes~1`).
`AppointmentBox` fills an appointment with the color of its first category that AnchorHub passes as `#RRGGBB`, and without one with `secondaryContainer`; the text takes `onSurface` or `inverseOnSurface`, whichever stands out more from that color.
The color of a category is data of Outlook, by which the user knows the appointment, and therefore the one color that does not come from the theme.
A click opens a menu with *Open in Outlook*, which runs `ankd outlook-open` with the EntryID of the appointment, for a recurring one that of its series.
Whole-day appointments are not shown.

Covers:

* `req~outlook-appointments~1`

Needs: impl, utest

### Events of the calendars in the calendar views
`dsn~calendar-events~1`

`TodoController.showAppointments` also reads the events of the calendars with busy times on the days of the view (`events`, without sideline words); `refreshCalendarEvents` keeps each iCalendar file in memory, so that another week does not read the network again, and `App` reads them again every 30 minutes.
`planWithCalendars` puts the files it reads into the same memory, so that the view shows what was planned with.
A calendar that cannot be read is logged by its host and left out of the view; the plan reports it.
`EventBox` draws an event in `tertiaryContainer` with a bar in `tertiary` on its left, apart from the appointments of Outlook and the blocks; one whose title holds a word of `dsn~sideline~1` is pale with a border and its text struck through.
A click on an event does nothing, as the application only reads these calendars.
The button *Calendars* of `Week` opens a menu, headed *Shown in Week and Today*, with a check box and the color of the entries for Outlook, where it is read, and for each calendar with busy times, named by the comment after its address or on the line above it (`calendarAddresses`), else by `X-WR-CALNAME` of its file (`calendarName`), else by its host.
The menu stays open while the user switches; `Today` shows the same calendars.
The setting `hiddenCalendars` of the device lists the hidden ones, one per line, by address or as `outlook`; an address is a secret and stays on the device as `calendars` does.
Planning reads all calendars, hidden or not.

Covers:

* `req~calendar-events~1`

Needs: impl, utest

### Entries side by side
`dsn~calendar-lanes~1`

`lanes` gives each entry of a day a column and the number of columns that its group shares.
A group are the entries that overlap, directly or through others; an entry takes the first column that is free at its start, the earlier and then the longer entry first.
`DayColumn` passes the appointments, the events and the blocks in this order, so that of entries with the same start and length the appointment stands left; an entry that overlaps nothing takes the whole width.

Covers:

* `req~calendar-events~1`

Needs: impl, utest

### Mirror of Outlook
`dsn~outlook-mirror~1`

With the setting `outlookMirror`, which the settings dialog offers where Outlook is reached and which is off by default, `TodoController.planWithCalendars` writes the appointments of all accounts that it read for a year as `calendar-outlook.txt` (`OutlookCalendar.mirror`) next to the todo.txt file: in the format of calendar.txt, a line per day that has one, the subject without line breaks and `Busy` for one without subject.
A device without `OutlookBridge` reads that file (`OutlookCalendar.mirrored`) for the busy times of planning and for `CalendarAppointments`, and *Calendars* of `Week` lists it as *Outlook*; an appointment without EntryID does not react to a click, as Outlook cannot open it.
`GitStorage` merges the file as it merges the plan: the version of the plan made last is taken.
A failure to read the file leaves the views without these appointments and is logged; the file is as current as the last plan of the device with Outlook.

Covers:

* `req~outlook-mirror~1`
* `adr~outlook-on-other-devices~1`

Needs: impl, utest

### Sideline in Outlook
`dsn~outlook-sideline~1`

The menu of an appointment puts it on the sideline or takes it off with `ankd outlook-sideline <link> on|off`, which adds or removes the category `Sideline` in Outlook, for a recurring appointment at its series.
AnchorHub passes the category as the field `sideline` of `outlook-busy`; `OutlookCalendar.isSidelined` holds for that field or for a word of `dsn~sideline~1` in the subject, and `OutlookCalendar.busyTimes` leaves such an appointment out.
An appointment on the sideline is drawn pale with a border and its text struck through; one that a word sidelines offers no switch, as the category would not change it.

Covers:

* `req~outlook-appointments~1`
* `req~sideline~1`

Needs: impl, utest

### Blocks in the calendar of Outlook
`dsn~outlook-blocks~1`

`TodoController.planWithCalendars` passes the blocks of the plan that start between now and the end of the day before today plus `outlookWeeks` weeks (`OutlookCalendar.window`, `OutlookCalendar.blocks`) to `OutlookBridge.syncBlocks`, for the account `outlookCalendar` of the settings; 0 weeks writes nothing.
A block has the id of its event in `calendar.ics` (`Ics.id`), its subject is its text followed by a blank and `[koppter]`, and its text starts with `[koppter]`.
On the desktop, `AnchorHubOutlook` passes them as JSON on the standard input of `ankd outlook-blocks-sync`, which creates them as private, tentative appointments without reminder and replaces those of its own in the window.
The summary of the plan tells what changed in Outlook; a failure is shown as a problem of the plan.
The settings dialog has the fields for the account and the weeks where Outlook is reached.

Covers:

* `req~outlook-calendar~1`
* `adr~plan-in-outlook~1`

Needs: impl, utest

### Busy times of Outlook
`dsn~outlook-busy~1`

Before planning, `TodoController.planWithCalendars` reads the appointments of the same calendar for a year through `ankd outlook-busy`, which leaves out the blocks of koppter, and those of the accounts of the setting `outlookBusyCalendars`, one call with `--calendar` each.
`Settings.outlookBusyAccounts` lists the account of the plan first and then the further ones, separated by blanks, commas or semicolons, each once regardless of case.
An account that is not read is named in the problems of the plan, and the others are still used.
The appointments of Today and Week (`TodoController.refreshAppointments`) come from the same accounts.
The settings dialog has the field *Further accounts with busy times* below the fields for the plan in Outlook, before the button that removes the blocks.
`OutlookCalendar.busyTimes` takes those that are not free and not whole-day appointments, split at midnight, as busy times in addition to those of the iCalendar files.

Covers:

* `req~outlook-calendar~1`

Needs: impl, utest

### Removing the blocks from Outlook
`dsn~outlook-clear~1`

The button *Remove koppter's blocks from Outlook* of the settings dialog asks `ankd outlook-blocks-clear --dry-run` for the number of blocks in the calendar of the account in the field and shows it in a confirmation; only after the confirmation does `ankd outlook-blocks-clear` delete them.
AnchorHub counts an appointment as a block of koppter only if it has both the user property `KoppterBlockId`, which only AnchorHub sets, and the category `koppter`, and is neither recurring nor a meeting; the subject is not considered.
Deleted appointments go to the deleted items of Outlook.

Covers:

* `req~outlook-calendar~1`
* `adr~plan-in-outlook~1`

Needs: impl, utest

### Plan dialog
`dsn~plan-dialog~1`

`PlanDialog`, opened with the button *Plan* of the top bar, edits the time maps, the time maps of contexts, the barriers and the limits as lines of the form `name = value`.
Each field shows what the configuration holds; all of them may stay empty, which their labels say, in the small style so that the label of an empty field does not read as its content.
*Save and plan* makes `TodoController.plan` save them, plan from the current time rounded up to a quarter of an hour, and write `calendar.txt` and `calendar.ics` as one change of the storage.
The dialog then tells how many tasks were planned and until when, what of the configuration and of the dependencies was not understood, and the tasks that do not fit before their barrier.
A switch above the tasks opens the dialog if the storage keeps files next to the todo.txt file.

Covers:

* `req~plan~1`
* `req~barriers~1`

Needs: impl, utest

### Plan again on completion
`dsn~replan-on-completion~1`

Completing a task that has a block today plans again with the saved configuration and calendars, as *Save and plan* does, so that the freed time goes to the tasks that are due and have no block today.
Completing any other task does not plan again, as planning may move every block from now on.

Covers:

* `req~plan~1`

Needs: impl, utest

### Agenda and week
`dsn~agenda~1`

`CalendarTxt.blocks` reads the blocks of `calendar.txt`; `TodoController.entryOf` finds the open task of a block by its text.
`Agenda` shows today and the two days after it, and of the four days after them each day that has blocks, each day with its blocks in the order of time; a block shows its task as in the list, or its text if the task is no longer open under that text.
Below the blocks of today, it lists the open tasks that are due today or earlier and have no block today (`TodoController.dueUnplanned`).
`Week` shows the seven days of a week from Monday as columns with the hours running down, at least from 08:00 to 18:00, and each block at its time; the hours share the height of the window, so that the whole week is seen without scrolling also on a phone that is held upright, and are at least 20 dp high; buttons move by a week, and a click on a block opens its task.
Both views show what the file says and do not plan.

Covers:

* `req~agenda~1`

Needs: impl, utest

### Clock of a task
`dsn~time-tracking~1`

The tag `started:` holds the local date and minute of the start of the clock, such as `started:2026-11-19T16:50`; `Task.started` reads it.
`Task.stop` adds the minutes since then to `spent:` through `Task.withSpent` and removes the tag; a start after the current time adds nothing.
`TodoController.start` stops the clock that runs, in the same write, and `TodoController.toggle` stops the clock of a task that is completed; the next occurrence of a recurring task has no clock.
The agenda and today have a play button (*Start*) or *Stop* beside each task of today, and a bar above every view shows the task whose clock runs, its start and the time since it, with *Stop*.

Covers:

* `req~time-tracking~1`
* `adr~clock-tag~1`

Needs: impl, utest

### Time log in a file
`dsn~time-log~1`

`timelog.txt` next to todo.txt holds a line per time, such as `2026-11-21 16:50-17:20 Write the report +Work`, with the text of the task as `calendar.txt` has it; a time over midnight is a line per day, the first one ending at `24:00`.
`TimeLog.append` adds a line whenever a clock stops: by *Stop*, by the start of another task and by the completion of the task.
*Away*, in the bar of the clock that runs and, while no clock runs, at the top of *Today*, shown while the user is not away, stops the clock that runs and appends an open line such as `2026-11-21 17:20- Away`; a bar above every view then shows the time away with *Back*, which, like starting a clock, replaces the open line by a closed one.
The answers to the flow: *Away* and *Back* take effect at once and are undone by the other; a failure to write the file is shown like any other; the time away survives a restart and is seen on another device, as it is in the file; on a phone, both are buttons that need no keyboard; the top bar has no room for *Away* there, and the agenda puts the time and the buttons of a block in two lines beside its card.
`GitStorage` merges the file line by line, as todo.txt, since both devices only add lines.

Covers:

* `req~time-log~1`
* `adr~time-log-file~1`

Needs: impl, utest

### Today
`dsn~today~1`

`Today` shows the agenda of today on the left and the blocks of today as a calendar on the right, as a column of `Week`, with a line at the current minute, which moves on once a minute.
Above them, it shows the sum of the planned time of the blocks of today whose task is open.
The calendar is 400 dp wide by default; below a width of 760 dp, it is left out.

Covers:

* `req~agenda~1`

Needs: impl, utest

### Filter of the plan
`dsn~plan-filter~1`

`Agenda`, `Week` and `Today` apply the filter as the list does: with a filter, they show the blocks whose open task matches it, and below the blocks of today the tasks due without a block that match it (`TodoController.shownBlocks`, `TodoController.dueUnplanned`).
Without a filter, they show every block, also that of a task that is no longer open.
The calendars of `Week` and `Today` keep the blocks that do not match the filter, in gray, as the appointments of Outlook stay as well: the time they take is still taken (`TodoController.blockMatches`).
The options for the first tasks and the next actions do not apply, as they would hide planned tasks behind a cut that the plan does not know.

Covers:

* `req~agenda~1`

Needs: impl, utest

### Width of the calendar of today
`dsn~today-resize~1`

Between the agenda and the calendar of `Today`, a handle is dragged sideways to change the width of the calendar, between 260 dp and the width that leaves the agenda 400 dp.
A line and a grip mark the handle, with a gap to the agenda and to the calendar on either side.
The width is kept in the settings of the device as `todayCalendarWidth` when the drag ends.

Covers:

* `req~agenda~1`

Needs: impl, utest

### Groups of today
`dsn~today-groups~1`

`Today` shows the switches of the groups of the list and shares the choice `listGroupKey` with it.
With an attribute, `TodoController.listGroupsOf` divides the tasks of the blocks of today with `groupEntries`, and the agenda shows each group below a heading, its blocks in the order of the day.
A block whose task has changed since planning stands after the groups, and the tasks that are due without a block stay below them.

Covers:

* `req~agenda~1`
* `req~list-groups~1`

Needs: impl, utest

### State of the plan in the labels
`dsn~plan-state~1`

`TodoController.planOf` looks a task up in the blocks of `calendar.txt` by the text of its block.
`TaskLabels` shows a calendar icon in front of the labels of an open task once that file exists: green, yellow or red by the state, with the start of the plan or "Not planned" as a tooltip.
A task without a block whose start date lies after today shows no icon.
Green is the green of the palette, which `Theme.kt` provides beside the Material color scheme.

Covers:

* `req~plan-state~1`

Needs: impl, utest

### Names of tasks in the plan
`dsn~plan-names~1`

The plan file holds texts, not lines of the todo.txt file, so that a task is found again by its text.
`CalendarTxt.names` gives each open task its name in the plan: the text, and for tasks that share the text also ` (due 2026-11-16)` with their due date, and a number as ` (2)` from the second of those on that share the due date as well.
The due date, unlike a number alone, stays with its task when another one of that text is completed while the plan still holds the blocks of both.
`plan` sets the name of the blocks of such tasks, which `CalendarTxt.write` and `Ics.write` use as the text.
`TodoController` finds the task of a block, the state of the plan of a task and the tasks without a block today by these names, and the week shows the addition after the description.

Covers:

* `req~plan-names~1`

Needs: impl, utest
