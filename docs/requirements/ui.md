# User interface

Requirements and design for the screens shared by all platforms.

## Requirements

### Labels at the top right of a task
`req~labels-top-right~1`

Each task in the list shows its description and, in a row at the top right, its projects, contexts, hashtags, tags such as the due date, and its priority as labels.
The description contains no metadata.

Covers:

* `feat~task-list~1`

Needs: dsn

### Add a task by typing a line
`req~quick-add~1`

The user adds a task by typing a line in todo.txt syntax into a field above the list.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Completion when adding a task
`req~quick-add-completion~1`

While the user types a project, a context or a hashtag into the line that adds a task, the application offers the known ones that fit.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Dates in words when adding a task
`req~quick-add-dates~1`

A typed line may name its dates in words at the end of its text, such as `tomorrow`, `next week`, `by Wed` or `tomorrow 3pm`.
The task gets the due date, the threshold date and the times that the words name.
The application shows the resulting line before the task is added.

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
Below it, the dialog has fields for priority, due date, threshold date, recurrence, duration, the task of ContextSwitcher, projects, contexts and hashtags.
Below these fields, the dialog shows the line as it will be written to the todo.txt file, in a monospaced font.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Names for priorities
`req~priority-names~1`

The user gives priorities names, such as the numbers 1, 2, 3, 5 and 8 for A to E.
The application shows the names where it shows a priority, offers the named priorities for a task, and accepts a name where a line or a filter is typed.
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

The user moves the due date of a task to a day nearby with one click: today, tomorrow, this weekend, next week, in two weeks or in a month.
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

`TaskCard` shows a checkbox for the completion state and, to its right, a column with the labels in a right-aligned flow row and the description below it.
A click on the card opens the edit dialog.

Covers:

* `req~labels-top-right~1`
* `req~complete-and-delete~1`

Needs: impl, utest

### Groups of the list
`dsn~list-groups~1`

`TodoController.listGroups` divides the visible tasks by the attribute `listGroupKey` with `groupEntries`, as the board does for its columns; without an attribute, the list is one group.
`TaskList` shows the groups one after the other, each below a heading with its title and the number of its tasks.
A task with several values of the attribute stands in each of their groups.
A step and a drag stay within a group; a task is given another value on the board or in the edit dialog.

Covers:

* `req~list-groups~1`

Needs: impl, utest

### Log of failures
`dsn~failure-log~1`

`TodoController` passes what failed to the function `log` that the platform gives it: an operation whose failure it shows to the user, with the exception, and a synchronization that failed, with the cause that `SyncStatus` carries.
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

### A file opened from the settings
`dsn~settings-open-file~1`

A file that the user chooses with *Choose file*, and the todo.txt file of a repository that the dialog cloned, is opened at once through `onOpenFile`, and the dialog stays open.
The fields for the board columns, the names of the priorities and the share actions then start anew with the configuration that this file brings along, and what was typed into them for the file before is dropped.
The configuration of the dialog is thus written to the file it was read from, never to another one.
A path typed into the field takes effect with *Save*, which writes these fields to the file that is still open and then opens the other one.
Leaving the dialog without *Save* opens the file again that was open when the dialog was opened; a clone stays in the data directory.

Covers:

* `req~choose-file~1`

Needs: impl, utest

### Line below the overdue tasks
`dsn~overdue-separator~1`

`TaskList` draws a line between the last overdue task and the first task that is not overdue, in each group that has tasks of both kinds.

Covers:

* `req~list-order~1`

Needs: impl, utest

### Menu of the due date
`dsn~due-menu~1`

A click with the secondary mouse button on a task card, or a click on the label of its due date, opens a menu with the days of `dueChoices`: today, tomorrow, this weekend, next week, in two weeks and in a month, each day once.
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

### Quick add
`dsn~quick-add~1`

A text field above the list passes its content to `TodoController.add` on Enter and clears itself.

Covers:

* `req~quick-add~1`

Needs: impl, utest

### Completion in the quick add line
`dsn~quick-add-completion~1`

`TodoController.completions` takes the word at the end of the typed line.
If it starts with `+`, `@` or `#`, it returns at most eight known projects, contexts or hashtags that contain the rest of the word, in any case of the letters, so that a name below another one is found by its last level.
The line shows them in a menu below the field, which does not take the focus.
Tab takes the first one, a click any of them: the word is replaced by the name, followed by a blank.
Esc closes the menu until the text changes.

Covers:

* `req~quick-add-completion~1`

Needs: impl, utest

### Dates in words in the quick add line
`dsn~quick-add-dates~1`

`TodoController.add` applies `Task.withDates` to the typed line, which `DatePhrase` implements.
The phrases are those that SkedPal reads in the title of a task, in English and in any case of the letters; the README lists them.
A phrase is read at the end of the text only, where projects, contexts, hashtags and tags may follow it; the same words elsewhere stay text.
It is removed from the text and becomes the tags `t:`, `due:`, `at:` and `dueby:`.
A tag that the line already has stays.
A weekday or a day of a month names the first such day from today on, and a week runs from Monday to Sunday.
The quick add field shows the line that `TodoController.datesPreview` returns below the typed one, as there is no other sign that words become a date.

Covers:

* `req~quick-add-dates~1`

Needs: impl, utest

### Edit dialog
`dsn~edit-dialog~1`

`EditDialog` keeps the edited values as dialog state and computes the resulting task with `Task.edit` and `Task.withTag` on every change.
The resulting line is shown below the fields in a monospaced font.
Projects, contexts and hashtags are chips: those of the task, then known ones to add with a click, then a field of the size of a chip for another name.
Typing into that field narrows the known ones to those that contain the typed text, so that a context below another one is found by its last level.
A chip of the task has the colors of its label in the list (`projectColors`, `contextColors`, `hashtagColors`).
*Save* is disabled while a date, the recurrence or the duration is invalid or the task is empty.
The dialog also serves for a new task.

Covers:

* `req~edit-dialog~1`
* `req~complete-and-delete~1`
* `adr~metadata-after-description~1`

Needs: impl, utest

### Names for priorities
`dsn~priority-names~1`

The key `priorities` of `koppter.yml` maps the letter of a priority to its name; the settings dialog edits it, one priority per line as `letter = name`.
`PriorityNames` gives the name of a priority, which is its letter if it has none, and the priorities to offer: those with a name, or A to F without names.
The label of a priority, the filter panel, the titles of the board columns by priority and the edit dialog show the name.
`TodoController.add` replaces a name in parentheses at the start of a typed line by its letter.
`TodoController.boardColumns` does the same for the priority terms of the filters, so that a filter names a priority by its name or by its letter, and the settings keep the filter as it was typed.
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

`FilterPanel` shows the criteria of `Filter` under the heading *Filter*: the text as a text field and the threshold date as a checkbox at the top, and the completion state as the two switches *Open* and *Done* at the bottom, of which at least one is on.
Priorities, projects, contexts and hashtags are shown as the labels that the task list uses for them.
The selected values stand beside the title of the criterion and the other values below it, both sorted; a click on a label moves it to the other group.
From a width of 840 dp on, the main screen places the panel beside the list; below that, the panel is the content of a navigation drawer.

Covers:

* `req~filter-panel~1`

Needs: impl, utest

### Main screen
`dsn~main-screen~1`

The main screen consists of the top bar, the filter panel, the switches for the view, the quick add field below them, the task list and a button for a new task.
The switches stand above the field, as a task is looked up more often than added.
A button of the top bar shows its description while the pointer rests on it.
Errors of the storage are shown in a snackbar.
The texts use the type scale of Material reduced by a tenth (`compactTypography`), which suits a window on a desktop and keeps a field of one line at the same height whether it is empty or filled.

Covers:

* `req~filter-panel~1`
* `req~quick-add~1`

Needs: impl

### Keyboard shortcuts
`dsn~keyboard-shortcuts~1`

The main screen is focusable and takes the focus whenever none of its elements has it and no dialog is open, so that keys always reach it; it handles the keys before its elements do.
With Ctrl, or the command key: N opens the dialog for a new task, E searches as this key does in OneNote, and 1 to 6 show the list and the views in the order of their switches.
The search shows the list with every task, the completed ones and those before their threshold date included: it resets the filter with its option for the first tasks and gives the focus to the text of the filter.
The agenda and the week, which the filter does not apply to, give way to the list when the text of the filter gets the focus.
With Ctrl and Shift: A gives the focus to the quick add field, F to the text of the filter, L opens the plan dialog, and O, Y and C show the list, the agenda and the week, as these keys do in SkedPal.
In the edit dialog, Ctrl and Enter save if *Save* is enabled.
`LocalKeyboard` tells whether the device has a keyboard; with one, the description of the edit dialog and the field of the inbox get the focus when they appear.
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

`SettingsDialog` edits the location of the todo.txt file, the switch for writing dates, the number of top tasks, the board columns defined by filters, the names of the priorities and the colors.
A changed location makes the controller load the other file.
The fields for the board columns and the share actions hold what is in effect, which without a definition of the user are the defaults of the configuration.
A field for a block of the configuration file shows an example in a pale color while it is empty.
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
