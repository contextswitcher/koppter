# User interface

Requirements and design for the screens shared by all platforms.

## Requirements

### Labels at the top right of a task
`req~labels-top-right~1`

Each task in the list shows its description and, in a row at the right, in the line of the description or above it, its projects, contexts, hashtags, tags such as the due date, and its priority as labels.
The description contains no metadata.

Covers:

* `feat~task-list~1`

Needs: dsn

### Dates in words
`req~dates-in-words~1`

The description that the user types for a task may name its dates in words at the end of its text, such as `tomorrow`, `next week`, `by Wed` or `tomorrow 3pm`.
The task gets the due date, the start date and the times that the words name.
The application shows the resulting line before the task is saved.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Complete, reopen and delete a task
`req~complete-and-delete~1`

The user completes and reopens a task in the list and deletes a task in its edit dialog.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Edit dialog
`req~edit-dialog~1`

The edit dialog shows the description without metadata in its first field.
Below it, the dialog has fields for priority, due date, start date, recurrence, duration, the task of ContextSwitcher, projects, contexts and hashtags.
Below these fields, the dialog shows the line as it will be written to the todo.txt file, in a monospaced font.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Names for priorities
`req~priority-names~1`

The user gives priorities names, such as the numbers 1, 2, 3, 5 and 8 for A to E.
The application shows the names where it shows a priority, offers the named priorities for a task, and accepts a name where a filter is typed.
The settings offer scales to choose with one click: numbers and steps of the Fibonacci sequence.
The file keeps the letters of the todo.txt format.

Covers:

* `feat~task-list~1`
* `feat~task-editing~1`

Needs: dsn

### Line of the edit dialog
`req~edit-line~1`

The line that the edit dialog shows can be edited as text, and the fields of the dialog follow what is typed.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Moving the due date
`req~move-due-date~1`

The user moves the due date of a task to a day nearby with one click: today, tomorrow, this weekend, next weekend, in two weeks or in a month.
In the list, this needs no opening of the task.

Covers:

* `feat~task-list~1`

Needs: dsn

### Failures in the log
`req~failure-log~1`

The application records what failed in its log, so that a problem can be looked up after the fact.

Covers:

* `feat~platforms~1`

Needs: dsn

### Sharing the log
`req~share-log~1`

On Android, where the log file is hard to reach on the device, the user passes the log to another application from the settings.

Covers:

* `feat~platforms~1`

Needs: dsn

### Version of the build
`req~build-version~1`

On Android, the settings show at their bottom the version and the commit the application was built from, so that the user tells which build runs on the device.

Covers:

* `feat~platforms~1`

Needs: dsn

### Links in the text of a task
`req~links~1`

An address of the web in the text of a task opens in the browser with a click.
A long address takes little room, and the user sees the whole one on request.

Covers:

* `feat~task-list~1`

Needs: dsn

### Groups in the list
`req~list-groups~1`

The user divides the list into groups with headings: by priority, project, context, hashtag or due date range.

Covers:

* `feat~task-list~1`

Needs: dsn

### Renaming a label
`req~label-rename~1`

The user gives a project, a context or a hashtag another name in all tasks at once.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Filter panel
`req~filter-panel~1`

A panel offers the filter criteria.
Priorities, projects, contexts and hashtags are offered as the values that occur in the file.
The panel stands beside the list where the window is wide enough and slides over the list otherwise.

Covers:

* `feat~filtering~1`

Needs: dsn

### Changes by other programs
`req~external-changes~1`

A change that another program makes to the todo.txt file appears in the application without a restart.

Covers:

* `feat~sync-local-folder~1`

Needs: dsn

### Choose the file
`req~choose-file~1`

The user sets the location of the todo.txt file.

Covers:

* `feat~sync-local-folder~1`

Needs: dsn

### Keyboard
`req~keyboard-shortcuts~1`

With a keyboard, the user adds a task, saves the edit dialog, changes the view and reaches the fields for a new task and for the text of the filter without the pointer.

Covers:

* `feat~task-editing~1`
* `feat~task-list~1`

Needs: dsn

### Changes that are not saved
`req~unsaved-changes~1`

A dialog that holds changes does not close by accident: before it closes without saving, the user is asked and may return to the dialog.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Color themes
`req~color-themes~1`

The user chooses the colors of the application: the palette Nord or Everforest, each light, dark or as the system is set.

Covers:

* `feat~task-list~1`

Needs: dsn

### Application icon
`req~application-icon~1`

The application has an icon by which the user tells it from other applications: on the window and in the task bar on the desktop, and in the launcher on Android.

Covers:

* `feat~platforms~1`

Needs: dsn

### Settings survive a restart
`req~persistent-settings~1`

The settings are kept between runs of the application.

Covers:

* `feat~sync-local-folder~1`
* `feat~task-editing~1`

Needs: dsn

### Links to the application
`req~app-links~1`

A link in another program, such as a page of a notebook, opens the application with the tasks of a project, a context or a hashtag.
If the application is open already, its window shows the tasks; no second window opens.
The user copies the link of a project, a context or a hashtag from its label.

Covers:

* `feat~filtering~1`
* `feat~platforms~1`

Needs: dsn

### Links of projects and contexts
`req~label-links~1`

The user gives a project, a context or a hashtag a link, such as one to the section of a notebook or to a category of ContextSwitcher, and opens it from the label.
The links are the same on every device that synchronizes the file.

Covers:

* `feat~task-list~1`
* `feat~filtering~1`

Needs: dsn

### What a new task starts with
`req~new-task-defaults~1`

A new task starts with a priority and a due date that the user states once, so that adding a task takes fewer steps.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Contexts of projects
`req~project-contexts~1`

The user states which contexts the tasks of a project have, and a task that is given the project in its dialog is given these contexts with it.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Restart to update
`req~restart-to-update~1`

A user who runs the desktop application from a checkout of its sources gets a newer version without leaving the application for a terminal: the application tells that there is one, and one click starts it.

Covers:

* `feat~platforms~1`

Needs: dsn

### Window after a change of the displays
`req~display-change~1`

When the desktop window moves to a display with another scale, such as when a laptop leaves its docking station, the content fills the window again.

Covers:

* `feat~platforms~1`

Needs: dsn

## Design

### Controller
`dsn~task-operations~1`

`TodoController` holds the file, the filter and the settings as Compose state.
Adding, completing, reopening, replacing and deleting a task write the whole file to the storage at once; there is no unsaved state.
Before an operation, the controller looks the task up in the current file, because the file may have been reloaded since the task was shown.
Whether dates are written is taken from the settings.

Covers:

* `req~complete-and-delete~1`
* `req~no-dates-by-default~1`

Needs: impl, utest

### Visible tasks
`dsn~visible-tasks~1`

`TodoController.visible` is the list of entries that match the filter, in list order.

Covers:

* `req~filter-criteria~1`
* `req~list-order~1`

Needs: impl, utest

### Task card
`dsn~task-card~1`

`TaskCard` shows a checkbox for the completion state and, to its right, the description and the labels in a right-aligned flow row.
Where both fit next to each other, they share one line, so that the description stands at the height of the checkbox; otherwise the labels stand above the description, each with the whole width.
A click on the card opens the edit dialog.

Covers:

* `req~labels-top-right~1`
* `req~complete-and-delete~1`

Needs: impl, utest

### Groups of the list
`dsn~list-groups~1`

`TodoController.listGroups` divides the visible tasks by the attribute `listGroupKey` with `groupEntries`, as the board does for its columns.
Without an attribute, `listParts` divides them into the overdue tasks, the other open tasks and the completed ones, with the titles *Overdue*, *Next* and *Done*; a part without tasks is left out.
`TaskList` shows the groups one after the other, each below a heading with its title and the number of its tasks.
The heading stays at the top of the list while the tasks of its group are scrolled, until the heading of the next group takes its place.
The heading of a priority, there and above a column of the board, shows the label of the priority as the tasks have it: a priority may be named by a number, which next to the number of the tasks reads as a second count.
A task with several values of the attribute stands in each of their groups.
A step and a drag stay within a group; a task is given another value on the board or in the edit dialog.

Covers:

* `req~list-groups~1`

Needs: impl, utest

### Log of failures
`dsn~failure-log~1`

`TodoController` passes what failed to the function `log` that the platform gives it: an operation whose failure it shows to the user, with the exception, a synchronization that failed, with the cause that `SyncStatus` carries, and a clone that failed in the settings dialog, with the exception.
For a calendar that was not read, it passes the host of the address and the kind of the exception only, as the address is a secret.
The desktop application and the Android application write these as errors to tinylog, and they do the same for an exception that no code caught.
`logToFile` names the log file for tinylog before anything logs: the desktop application passes the directory `logs` of its data directory, and `KoppterApplication` passes the directory `logs` of the application in the shared storage of the device, or in its private storage if there is none.
On Android, warnings and errors go to logcat as well.

Covers:

* `req~failure-log~1`

Needs: impl, utest

### Button that shares the log
`dsn~share-log~1`

`App` takes a function `shareLog`; if the platform gives one, `SettingsDialog` shows the button *Share log*, which calls it and leaves the dialog open.
`MainActivity` gives the function: it sends the last 60,000 characters of the newest log file as text to the application that the user chooses, with koppter itself left out of the choice.
As text, the log is shown by the receiving application before it is sent on, and it fits into what Android passes between applications.
The desktop application gives no function: its log directory is reached with the file manager.

Covers:

* `req~share-log~1`

Needs: impl, utest

### Line with the version of the build
`dsn~build-version~1`

`App` takes a text `version`; if the platform gives one, `SettingsDialog` shows it as its last line, in a container that lets the user select and copy it.
The build of `:android` writes the short hash and the date of the commit of `HEAD` into `BuildConfig.GIT_COMMIT_LINE`; `MainActivity` puts the version name of a release before it.
The desktop application gives no text.

Covers:

* `req~build-version~1`

Needs: impl, utest

### Links
`dsn~links~1`

`LinkedText` shows the parts of a description that start with `http://` or `https://` (`webAddresses`) as links, which the task card and the tasks of a context in the overview show in the primary color and underlined.
An address ends at a blank, and before a mark that ends a sentence or closes a bracket.
An address of more than 50 characters is shown with its first 49 characters and an ellipsis (`shortened`); the link and the file keep the whole address.
While the pointer of the mouse rests on a cut address, a tooltip shows the whole one.
The tooltip does not react to a long press, which starts the drag of a card.
A click on a link opens it through the `UriHandler` of the application and does not open the task.

Covers:

* `req~links~1`

Needs: impl, utest

### Links of projects and contexts
`dsn~label-links~1`

The key `links` of `koppter.yml` maps a project, a context or a hashtag, written with its sign as `+Fair`, `@phone` or `#quick`, to a link; the settings dialog edits it, one per line as `label = link`.
`ConfigFile.link` gives the link of a label: its own, or that of the nearest project or context above it.
The menu of a label (`WithLinkMenu`), at a task and in the filter panel, has *Open link* for a label with a link, which opens the link through the `UriHandler` of the application.
The link is not checked: what the platform cannot open is reported as for a link in a task.

Covers:

* `req~label-links~1`
* `adr~label-links~1`

Needs: impl, utest

### What a new task starts with
`dsn~new-task-defaults~1`

The key `new-task` of `koppter.yml` has the entries `priority` and `due`; the settings dialog edits it, one per line as `name = value`.
`ConfigFile.newTask` gives the task that the dialog of a new task starts with.
The priority is given by its name or by its letter.
The due date is given by a name of `dueDays`, which are the days of the menu of the due date under their names on every day, or as a filter reads a date: `tomorrow`, or an interval from today such as `10d`.
A value that is not understood is left out.
The dialog saves a new task only once it differs from what it started with, so that the priority and the due date alone are no task.
A storage that keeps no configuration file starts a new task empty.

Covers:

* `req~new-task-defaults~1`

Needs: impl, utest

### Contexts of projects
`dsn~project-contexts~1`

The key `project-contexts` of `koppter.yml` maps a project, written with its sign as `+Fair`, to its contexts, separated by blanks or commas; the settings dialog edits it, one project per line as `+project = @context`.
`ConfigFile.contextsOf` gives the contexts of a project: its own and those of each project above it.
`EditDialog` adds them to the contexts of the task when a project is added to its projects, for a new task and an existing one.
A project that the task already has adds nothing, so that a context that the user removes stays removed, and so does a project that is typed into the description.

Covers:

* `req~project-contexts~1`

Needs: impl, utest

### A file opened from the settings
`dsn~settings-open-file~1`

A file that the user chooses with *Choose file* and the todo.txt file of a folder chosen with *Choose folder* becomes the file of the application through `onOpenFile`, and the dialog is left, so that the tasks of the file show.
Nothing that stood in the dialog is written, neither to that file nor to the one from before: what was typed into the dialog is dropped.
A chooser that fails is reported at the field, in the color for errors, and the dialog and the open file stay; a chooser that the user leaves without a choice changes nothing.
The configuration of the dialog is thus written to the file it was read from, never to another one.
A path typed into the field takes effect with *Save*, which writes the fields of the dialog to the file that is still open and then opens the other one.
A repository that the dialog clones is opened the same way: `dsn~git-setup-dialog~1` says what *Clone* does.

Covers:

* `req~choose-file~1`

Needs: impl, utest

### Line below the overdue tasks
`dsn~overdue-separator~1`

`TaskList` draws a line between the last overdue task and the first task that is not overdue, in each group that has tasks of both kinds.
The list without groups has none, as its overdue tasks stand below a heading of their own.

Covers:

* `req~list-order~1`

Needs: impl, utest

### Menu of the due date
`dsn~due-menu~1`

A click with the secondary mouse button on a task card, or a click on the label of its due date, opens a menu with the days of `dueChoices`: today, tomorrow, this weekend, in three days, next weekend, in two weeks and in a month, each day once and in the order of the days; a weekend stands for its Sunday.
A weekend and a week end on their Sunday, as in the dates that are typed in words.
Choosing a day calls `TodoController.setDue`, which sets the `due:` tag and leaves the rest of the line as it is.
For a task with a due date, the menu ends with *No due date*, which removes the tag.
The cards of the board and of the agenda are the same `TaskCard` and have the menu as well.

Covers:

* `req~move-due-date~1`

Needs: impl, utest

### Days for the due date in the edit dialog
`dsn~due-chips~1`

Above its date fields, `EditDialog` shows the days of `dueChoices` as chips.
A click on a chip writes its day into the field of the due date, and a click on the chip of the day that the field holds empties the field.
A device without a mouse thereby reaches these days also for a task without due date, which has no label that opens the menu.

Covers:

* `req~move-due-date~1`

Needs: impl, utest

### Task labels
`dsn~task-labels~1`

`TaskLabels` emits one label per project, context, hashtag and tag and one for the priority, in this order.
A tag label shows its key in front of the value.
The colors are taken from the Material color scheme; a due date in the past uses the error colors.

Covers:

* `req~labels-top-right~1`

Needs: impl

### Dates in words
`dsn~dates-in-words~1`

`Task.withDates`, which `DatePhrase` implements, reads the words.
The phrases are those that SkedPal reads in the title of a task, in English and in any case of the letters; the README lists them.
A phrase is read at the end of the text only, where projects, contexts, hashtags and tags may follow it; the same words elsewhere stay text.
It is removed from the text and becomes the tags `t:`, `due:`, `at:` and `dueby:`.
A tag that the line already has stays.
A weekday or a day of a month names the first such day from today on, and a week runs from Monday to Sunday.

Covers:

* `req~dates-in-words~1`

Needs: impl, utest

### Dates in words in the edit dialog
`dsn~edit-dates~1`

`EditDialog` applies `Task.withDates` to the task that its fields give, once the description differs from that of the task: words that a task was written with stay words.
The dialog marks the words that become dates with a background and says below the field what the mark means; the resulting line shows the tags.
A date in a field of the dialog wins over the words.

Covers:

* `req~dates-in-words~1`

Needs: impl, utest

### Help at the fields of the edit dialog
`dsn~edit-help~1`

The fields of `EditDialog` for the dates, the times and the recurrence show a sentence on what they mean, with their tag, while the pointer rests on them for half a second (`WithHelp`).
The help is bound to the pointer alone: on a touch screen, a long press on a field belongs to its text.
The field for the tag `t:` is named *Start date*, as topydo names that date; Simpletask calls it the threshold date.

Covers:

* `req~edit-dialog~1`

Needs: impl, utest

### Choosing times and durations in the edit dialog
`dsn~edit-pickers~1`

The fields of `EditDialog` for the due time and the fixed time have a button that opens the clock face of Material with 24 hours (`TimeField`); without a time, it starts at noon.
The fields for the duration, the travel time and the time spent have a button that opens two wheels for hours and minutes, the minutes in steps of five (`DurationField`, [decision 0037](../decisions/0037-wheels-for-durations-from-compose-pickers.md)); without a duration, they start at half an hour.
The field for the recurrence has a button that opens a wheel for the amount, from 1 to 30, a wheel for the unit, and the choice whether the interval counts from the completion or from the due date (`RecurrenceField`); without a recurrence, they start at one week from the completion.
*OK* writes what was chosen into the field as text, and the other button of each dialog empties the field.
The fields still take typed text.

Covers:

* `req~edit-dialog~1`

Needs: impl, utest

### Edit dialog
`dsn~edit-dialog~1`

`EditDialog` keeps the edited values as dialog state and computes the resulting task with `Task.edit` and `Task.withTag` on every change.
The resulting line is shown below the fields in a monospaced font.
A heading names the dialog: *New task* or *Edit task*.
The description starts with the cursor at its end.
Projects, contexts and hashtags are chips: those of the task, then all known ones to add with a click, then a field of the size of a chip for another name.
Typing into that field narrows the known ones to those that contain the typed text, so that a context below another one is found by its last level.
A chip of the task has the colors of its label in the list (`projectColors`, `contextColors`, `hashtagColors`).
The calendar of the deadline has the button *Use due date* while the task has a due date, as the two are often the same day.
*Save* is disabled while a date, the recurrence or the duration is invalid or the task is empty.
The dialog also serves for a new task.

Covers:

* `req~edit-dialog~1`
* `req~complete-and-delete~1`
* `adr~metadata-after-description~1`

Needs: impl, utest

### Width of the edit dialog
`dsn~edit-resize~1`

The edit dialog is 760 dp wide where the window allows that, which is enough for the priorities and the days of the due date in one row each.
On a device with a keyboard, its right edge has a handle that is dragged: the dialog stays in the middle of the window, so its width changes by twice the way of the pointer, between 420 and 2400 dp.
The dialog is a layer of the window of the application and not a window of its own, so it has no frame of the operating system to drag.
The width is kept in the settings of the device as `editWidth` when the drag ends.

Covers:

* `req~edit-dialog~1`

Needs: impl, utest

### Names for priorities
`dsn~priority-names~1`

The key `priorities` of `koppter.yml` maps the letter of a priority to its name; the settings dialog edits it, one priority per line as `letter = name`.
`PriorityNames` gives the name of a priority, which is its letter if it has none, and the priorities to offer: those with a name, or A to F without names.
The label of a priority, the filter panel, the titles of the board columns by priority and the edit dialog show the name.
`TodoController.boardColumns` replaces a name by its letter in the priority terms of the filters, so that a filter names a priority by its name or by its letter, and the settings keep the filter as it was typed.
Below the field for the names, the settings dialog has one button per scale, which fills the field.

Covers:

* `req~priority-names~1`
* `adr~priority-names~1`

Needs: impl, utest

### Line of the edit dialog
`dsn~edit-line~1`

The line of `EditDialog` is a text field.
What is typed there is parsed as a task, from which the fields of the dialog start anew; a tag that has no field stays in the line.
While the field has the focus, it shows the text as it was typed; a field that is edited afterwards changes the line as before.

Covers:

* `req~edit-line~1`

Needs: impl, utest

### Filter panel
`dsn~filter-panel~1`

`FilterPanel` shows the criteria of `Filter` under the heading *Filter*: the start date as a checkbox at the top, and the completion state as the two switches *Open* and *Done* at the bottom, of which at least one is on.
The text of the filter is a field (`SearchField`) of the main screen above the list where the panel stands beside it; on a narrower screen, it is the first field of the panel in the drawer, and a search button to the left of the button for a new task opens the drawer and gives the field the focus.
While the field has a text, a button at its end empties it; a text typed in the agenda or the week, which the filter does not apply to, shows the list.
Priorities, projects, contexts and hashtags are shown as the labels that the task list uses for them.
The selected values stand beside the title of the criterion and the other values below it, both sorted; a click on a label moves it to the other group.
From a width of 840 dp on, the main screen places the panel beside the list; below that, the panel is the content of a navigation drawer and has a button that closes it.

Covers:

* `req~filter-panel~1`

Needs: impl, utest

### Excluding in the filter panel
`dsn~filter-exclusion~1`

`Filter` holds excluded projects, contexts and hashtags: `Filter.matches` leaves out a task that has one of them, a project or a context with those below it, whatever is selected.
The menu of a label of the filter panel, which a click with the secondary button or a long press opens, has the entry *Hide these tasks*; it excludes the value and ends its selection.
An excluded value stands beside the title of its criterion after the word *not*, and a click on it ends the exclusion; *Clear all filters* ends all of them.
The exclusion is part of the filter, so that the list, the board, the overview and the inbox follow it.

Covers:

* `req~filter-panel~1`
* `req~filter-criteria~1`

Needs: impl, utest

### Search for labels in the filter panel
`dsn~filter-label-search~1`

At its top, below the heading and the field for the text of the filter where the panel has one, `FilterPanel` has a field that narrows the projects, contexts and hashtags that the panel offers to those whose name contains the typed text, whatever the case of its letters.
A project or a context above a found one stays, so that the tree keeps its shape; the selected and the excluded values stay as well.
A text that starts with `+`, `@` or `#` finds projects, contexts or hashtags only.
The field does not change the filter and is not kept.
While it has a text, a button at its end empties it.

Covers:

* `req~filter-panel~1`

Needs: impl, utest

### Renaming a label in the filter panel
`dsn~label-rename~1`

The menu of a project, a context or a hashtag of the filter panel has the entry *Rename…*, which asks for the new name: one word, and for a hashtag one that a hashtag can have.
`TodoController.rename` writes the name with `TodoFile.rename` into every line that has the label, completed tasks included, and leaves the other lines as they are.
`renameLabel` replaces the label where it is a word of its own, and the start of each label below it, so that renaming `+papers` makes `+papers>survey` a project below the new name and leaves `+papers2` alone.
The scheduled tasks of `ice_recur.txt` are renamed in the same way, as they would bring the old name back, and the filter follows.
`ConfigFile.renamed` renames the label in the links and in the contexts of projects of `koppter.yml`, where the contexts of a project with a renamed context are then written with their signs and separated by blanks.
The limits and the board columns of `koppter.yml` keep the old name.

Covers:

* `req~label-rename~1`

Needs: impl, utest

### Filter after a restart
`dsn~filter-remembered~1`

`Filter.toLink` writes the whole filter as a link to the application, and `filterOfLink` reads it back.
`TodoController` keeps that link in the settings of the device with every change of the filter and starts with the filter it names; the filter that restricts nothing is kept as an empty value.
A change of the todo.txt file in the settings ends the filter, as it names projects and contexts of the former file.
The options of the first tasks and of the next actions are not kept.

Covers:

* `req~persistent-settings~1`
* `req~filter-panel~1`

Needs: impl, utest

### View after a restart
`dsn~view-remembered~1`

`TodoController` reads the view, the attribute of the groups of the list, that of the columns of the board and that of the tiles of the overview from the settings of the device and writes them there with every change.
The application therefore starts with the view that was shown last.
A name that this version does not know gives the list, the list without groups, the board by priority and the overview of the contexts.

Covers:

* `req~persistent-settings~1`

Needs: impl, utest

### Main screen
`dsn~main-screen~1`

The main screen consists of the top bar, the filter panel, the switches for the view, the field for the text of the filter below them where the window is wide, the task list and a button for a new task, which opens the edit dialog; a narrower window has a search button to its left instead of the field.
A button of the top bar shows its description while the pointer rests on it.
Errors of the storage are shown in a snackbar.
The texts use two sizes of the type scale of Material (`compactTypography`): the medium body size for texts, fields, buttons and titles, and the small body size for labels and remarks; a title differs from a text by its weight.
That suits a window on a desktop and keeps a field of one line at the same height whether it is empty or filled.
The search fields (`SearchField`) have less space above and below their line than a text field of Material, which is at least 56 dp high.

Covers:

* `req~filter-panel~1`

Needs: impl

### Keyboard shortcuts
`dsn~keyboard-shortcuts~1`

The main screen is focusable and takes the focus whenever none of its elements has it and no dialog is open, so that keys always reach it; it handles the keys before its elements do.
With Ctrl, or the command key: N opens the dialog for a new task, E searches as this key does in OneNote, F gives the focus to the text of the filter as this key searches in browsers and editors, and 1 to 6 show the list and the views in the order of their switches.
The search shows the list with every task, the completed ones and those before their start date included: it resets the filter with its option for the first tasks and gives the focus to the text of the filter.
The agenda and the week, which the filter does not apply to, give way to the list when the text of the filter gets the focus.
Where that field lies in the drawer, the drawer opens first.
With Ctrl and Shift: A opens the dialog for a new task, F gives the focus to the text of the filter, L opens the plan dialog, and O, Y and C show the list, the agenda and the week, as these keys do in SkedPal.
In the edit dialog, Ctrl and Enter save if *Save* is enabled.
`LocalKeyboard` tells whether the device has a keyboard; with one, the description of the edit dialog and the field of the inbox get the focus when they appear.
Without one, the description gets the focus in the dialog of a new task only, as the keyboard of the screen hides half of an existing task.
On Android, it is the keyboard of the configuration of the device.

Covers:

* `req~keyboard-shortcuts~1`

Needs: impl, utest

### Changes that are not saved
`dsn~unsaved-changes~1`

`GuardedDialog` is the dialog of the edit dialog, the settings, the plan dialog and the editor of the schedule.
On Esc, on a click beside the dialog and on the back button, it closes only if the values of the fields are those the dialog started with, or those that the plan dialog saved last.
Otherwise it asks whether to discard the changes; *Keep editing* returns to the dialog as it was.
The buttons of a dialog that close it without saving do so without the question.

Covers:

* `req~unsaved-changes~1`

Needs: impl, utest

### Reload on external change
`dsn~reload-on-external-change~1`

The main screen asks the storage for its version every two seconds and reloads the file when the version differs from the one last read or written.

Covers:

* `req~external-changes~1`

Needs: impl, utest

### File storage
`dsn~file-storage~1`

`FileStorage` reads and writes a file in the file system.
It writes to a temporary file in the same directory and moves it over the target.
The version is the modification time together with the size.

Covers:

* `req~choose-file~1`
* `req~external-changes~1`

Needs: impl, utest

### Settings file
`dsn~settings-file~1`

The settings are stored as a Java properties file.
Unknown keys are ignored and missing keys take their default.

Covers:

* `req~persistent-settings~1`

Needs: impl, utest

### Color themes
`dsn~color-themes~1`

`colorScheme` fills the roles of the Material color scheme with the colors of the chosen palette in its light or dark variant.
A container color is the color of its role mixed into the color of the cards; the text on an accent color is the end of the palette with the higher contrast.
The palette and the variant are settings; `App` asks the system for the variant if the setting leaves it to the system.
A choice in the settings dialog is saved and applied at once, so that the user sees the colors behind the dialog.

Covers:

* `req~color-themes~1`
* `adr~colors-from-published-palettes~1`

Needs: impl, utest

### Application icon
`dsn~application-icon~1`

`docs/logo.svg` is the drawing of the icon: a K as the cabin of a helicopter, in colors of the palette Nord.
The desktop window shows `koppter.png`, an export of that drawing in the resources of the desktop target.
The Android application has an adaptive icon whose foreground repeats the paths of the drawing as a vector drawable.

Covers:

* `req~application-icon~1`

Needs: impl

### Settings dialog
`dsn~settings-dialog~1`

`SettingsDialog` edits the location of the todo.txt file, the switch for writing dates, the number of top tasks, the board columns defined by filters, the names of the priorities, the links of projects and contexts and the colors.
A changed location makes the controller load the other file.
The fields for the board columns and the share actions hold what is in effect, which without a definition of the user are the defaults of the configuration.
A field for a block of the configuration file shows an example in a pale color while it is empty.
The help of the field for the file tells that the files next to the todo.txt file belong to it, and a note that the platform passes stands below the button that chooses a file (`dsn~choose-file~1`).
Below the field for the file stands what holds for a file in a git repository.

Covers:

* `req~choose-file~1`
* `req~no-dates-by-default~1`

Needs: impl, utest

### Locations on the desktop
`dsn~desktop-locations~1`

On the desktop, the settings file is `settings.properties` in `%APPDATA%\koppter` on Windows and in `$XDG_CONFIG_HOME/koppter` or `~/.config/koppter` on Linux.
The files that the application creates are in its data directory: `%APPDATA%\koppter` on Windows and `$XDG_DATA_HOME/koppter` or `~/.local/share/koppter` on Linux.
Its directory `logs` holds the log files `koppter-0.log` and following, the highest number being the newest; a file is closed at one megabyte, and three files are kept.
Without a configured location, the todo.txt file is `todo.txt` in the home directory if that file exists, as earlier versions kept it there, and `todo.txt` in the data directory otherwise.

Covers:

* `req~persistent-settings~1`
* `req~choose-file~1`

Needs: impl

### Links to the application
`dsn~app-links~1`

A link has the form `koppter://filter?project=papers>survey&context=phone&hashtag=someday`: each of the keys `project`, `context` and `hashtag` may be given, also several times.
The other criteria of the filter have keys as well: `not-project`, `not-context` and `not-hashtag` for what is excluded, `priority` with a letter, `text`, `show` with `all` or `done` for completed tasks, and `future=1` for tasks before their start date.
`filterOfLink` reads it into a filter; it decodes characters that a program wrote as `%3E`, takes a name with or without its sign, and accepts the forms `koppter:filter?` and `koppter://filter/?`, which programs make of the link.
`TodoController.openLink` replaces the filter by that of the link, turns off the options of the first tasks and of the next actions, and shows the list; a text that is no such link changes nothing.

Covers:

* `req~app-links~1`
* `adr~links-with-query~1`

Needs: impl, utest

### One window for links on the desktop
`dsn~link-single-instance~1`

The desktop application takes a link as its argument.
The running application listens on a port of the computer itself and writes its number to the file `link.port` in the data directory.
A start with a link sends it to that port and ends if the answer is that of the application; otherwise it starts as usual and opens the link itself.
The running application opens a received link and brings its window to the front.
A start without a link opens a window of its own, as before.

Covers:

* `req~app-links~1`

Needs: impl, utest

### Scheme of the links on Windows
`dsn~link-scheme-windows~1`

On Windows, the installed program writes at each start to the registry of the current user that links of the scheme `koppter` are opened with it: the key `Software\Classes\koppter` with the value `URL Protocol`, and the command of the program with the link as its argument.
Writing at each start keeps the entry right after an update, and needs neither the installer nor rights of an administrator.
A start from the build registers nothing.

Covers:

* `req~app-links~1`

Needs: impl, utest

### Copying a link
`dsn~app-link-copy~1`

`linkTo` writes the link to a project, a context or a hashtag and encodes every character of the name that is not a letter, a digit or one of `-._~`, so that a program that finds links in a text takes the whole link.
`WithLinkMenu` wraps the labels of projects, contexts and hashtags of a task and of the filter panel: a click with the secondary button opens a menu.
At the label of a task, its entry *Copy link* puts the link on the clipboard; the menu of the filter panel has no such entry, as it is there to change the labels.
The click is taken before the card of the task sees it, so that the card does not open its menu of due dates.
In the filter panel, a finger that stays on a label opens the menu as well, and lifting it does not select the label; the label of a task has no such menu, as a long press there picks up the card.

Covers:

* `req~app-links~1`

Needs: impl, utest

### Restart to update
`dsn~restart-to-update~1`

A running application cannot replace its own classes, so the update is a restart.
`just run-loop`, also `just loop`, runs `scripts/run-loop.sh` or, on Windows, `scripts/run-loop.cmd`: in the checkout, it pulls, builds the application with its runtime (`:app:createDistributable`) and starts it, with the commit it built in the environment variable `KOPPTER_RUN_LOOP`.
A pull that fails, such as without a network, does not stop the start.
When the application ends with the exit code 55, the script does the same again; every other exit code ends it, so that closing the window stays a quit.
The script prints `Running koppter...` before each start and `koppter finished` when it ends, as the window may open on another desktop.
Started that way, the desktop application fetches at its start and every half hour and counts the commits of the upstream branch that the commit it runs lacks (`commitsBehind`).
It counts from that commit and not from `HEAD`, as a pull outside the application moves `HEAD` while the old build keeps running.
If there is one, the top bar shows the button *Restart to update*, which ends the application with the exit code 55.
Whenever git cannot tell, nothing is shown.
Without the variable, the application neither fetches nor shows the button, as nothing would start it again.

Covers:

* `req~restart-to-update~1`

Needs: impl, utest

### Window after a change of the displays
`dsn~display-change~1`

On Windows, after a change of the scale of the display, the drawing surface of the window may keep the size of the old scale, so that the content covers only part of the window.
Restoring and maximizing the window again, or resizing it, gives the surface its size.
The desktop application does that itself: when the graphics configuration of the window changes, it waits half a second for the display change to settle, then restores and maximizes a maximized window again, or makes any other window one pixel wider and back.

Covers:

* `req~display-change~1`

Needs: impl
