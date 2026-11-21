# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Calendar Versioning](https://calver.org/) with the scheme `YYYY-0M-0D`.

## [Unreleased]

### Changed

- *Cancel* in the dialog of a new task keeps what was entered as a draft, with which the next new task starts; *Discard*, after Esc or the back button, removes it.

## [2026-11-20] - 2026-11-20

### Added

- An application for watches with Wear OS, such as the Galaxy Watch, shows the plan from now on, or the open tasks, and completes a task; the release carries it as `koppter-wear-<date>.apk`.
- The watch application has a tile with the next two tasks.
- On Android, a notification tells that a block of the plan starts; *Done* in it completes the task, and a connected watch shows it with that action.
- The Android widget has a *+* button that opens the dialog for a new task, as does the link `koppter://new`.
- The Android widget is placed at two cells by one and pulled larger for more of the plan.
- The edit dialog has the rows *Waits for* and *Before*: typing part of the description of another task offers it, and *Save* writes the tags `id:` and `p:` into both tasks.
- The fields of the settings that name projects and contexts, such as *Contexts of projects*, offer the projects and contexts of the file while `+` or `@` is typed.

### Changed

- On Android, *Share log* passes the entries of the last hour only, instead of the end of the log whatever its age.
- The labels *Outlook*, *LaTeX* and `cs:`, which open the item in another program, are shown in the color of links instead of that of projects.
- The settings are grouped in tabs: *File*, *Tasks*, *Colors* and, where the platform offers them, *Integrations*.
  The title, the tabs and *Save* stay in view while a tab scrolls.
- The plan dialog lists the time maps instead of showing them as text; *Edit* or a click on a map opens the painter, which now also edits the name and dates of the map, takes a typed definition and deletes the map, and *Add time map* adds one.
- The calendar in which a date of a task is chosen starts its weeks on Monday, as the other views do, instead of on the first day of the locale of the system.

### Fixed

- On Android, text shared to koppter right after its start is committed instead of failing with `NetworkOnMainThreadException`.
- A synchronization no longer fails with *Checkout conflict* when the todo.txt file changes while it fetches.

## [2026-11-19] - 2026-11-19

### Added

- A task can name its own time map with `map:`, such as `map:Evenings`; the plan takes it before the time map of its context.
- *Week* and *Today* show the appointments of the calendar of Outlook beside the blocks of the plan, in the colors of their categories in Outlook. A click on one opens it in Outlook or puts it on the sideline, as SkedPal does: it stays in the calendar, and tasks are planned into its time. The sideline is the category `Sideline` in Outlook, for a recurring appointment of the whole series.
- *Add holidays* in the plan dialog adds the public holidays of a German state to the first time map, so that no tasks are planned on them.
- On the desktop, *Send the projects and contexts of the filter to ContextSwitcher* in the settings lets koppter and ContextSwitcher share the projects and contexts of their filters: a change in one changes the filter of the other.
- A time map can have its own times on dates, such as `General 2026-12-24..2027-01-06 = 10:00-12:00` for a vacation or `General 2026-12-25 2026-12-26 = none` for holidays; on these dates they replace the times of the map.
- *Start* beside a task of today in the agenda and in *Today* starts a clock on it, written as `started:` into the task; a bar above every view shows the task and the time since its start, and *Stop* adds that time to `spent:`.
- *Today* shows the agenda of today beside the day as a calendar with a line at the current time and the sum of the planned time; Ctrl+7 opens it.
- The settings show the shared lists as rows with their context, *Check* for the connection of a repository and *Remove*; *Add folder* adds a folder on the desktop.
- The `ice_recur.txt` of a shared list schedules tasks into that list.
- *Edit…* in the menu of a project label edits its link, the contexts of its tasks and the main file of a LaTeX document.
- On the desktop, the TODOs of the LaTeX document of a project, from the main file and the files it takes in with `\input` and `\include`, are shown as tasks of the project with the hashtag `#latex`, without writing them to the file.
  `\todo` of `todonotes` and the commands of `fixme`, `zebra`, `snaptodo`, `fixmetodonotes` and `pdfcomment` count.
  Giving a TODO a priority or another change writes it to the file with the tag `tex:`; completing it in koppter hides it; the document is not changed.
- *Add image* attaches several images chosen at once, and on Android several images shared together become one task with all of them in its note.
- When the text of a TODO of a LaTeX document changes a little, its task in the file follows it with the new text and keeps its priority, labels and dates.
- The label *LaTeX* of a task opens its TODO, with the program that *Program that opens a TODO* in the settings names, such as `code --goto {file}:{line}`, or with the program of the system.
- A file in a git repository is synchronized every five minutes as well, so that an application left open receives the changes of other devices before a task is edited there.

### Changed

- The filter applies to *Today*, *Agenda* and *Week* as well: they show the planned blocks and due tasks that match it, and typing a search text there no longer switches to *List*.
- The switches of the views are in the order *Today*, *List*, *Agenda*, *Week*, *Board*, *Inbox*, *Overview*; Ctrl+1 to Ctrl+7 follow that order.
- The calendar of *Today* is 400 dp wide instead of 260 dp, and a handle between the agenda and the calendar changes its width, which is kept.
- On Windows, the title bar of the window follows the light or dark variant of the colors.
- The note of a task that moves between the main list and a shared list moves with its images and is deleted where it was.
- The settings keep the address of a cloned shared list and clone it again when the clone is gone; a list that cannot be read no longer keeps the other tasks from being shown.
- On Android, commits without a configured name are made in the name of the device and with the email address `koppter@android<version>`, such as `koppter@android14`, instead of `koppter` and `koppter@localhost`.

### Fixed

- After an image is attached, the note of the task has the focus with the cursor behind the image, instead of where a tap into the note puts it.
- The agenda lists the due items of the To-Do list of Outlook below today, as the list and the inbox do.

## [2026-11-18] - 2026-11-18

### Added

- *Sideline* in the plan dialog names words: an event of the calendars with busy times whose title contains one of them, such as a regular appointment of someone else, stays in the calendar, but tasks are planned into its time; a repeating event with all its occurrences.
- *Clone* below *Shared lists* in the settings clones the repository of a shared list, also on Android, and adds its todo.txt file to the shared lists; a clone of another repository of the same name is kept and the new one gets a number.
- *Shared lists* in the settings names further todo.txt files, such as clones of repositories shared with a household or a shared flat. Their tasks are shown with the name of their directory as context, such as `@household`; a task with that context is written into that file together with its note, any other task into the main file.
- The times of a time map can be green, yellow, red or black, as in SkedPal: tasks are planned in green times; yellow and then red times are taken only for a task that would otherwise be done after its due date or not before its barrier; black times are left to the tasks of their own map. *Paint* in the plan dialog paints a time map on the days of a week.
- On Windows with AnchorHub installed, the list shows the open items of the To-Do list of the classic Outlook, flagged mails and tasks of all accounts, without writing them to the file. An item carries the hashtag `#outlook`, and its label *Outlook* opens it in Outlook. Giving an item a context or a project, or another change, writes it to the file with the hashtag and the tag `ol:`; completing it in koppter marks it done in Outlook.
- Planning writes the blocks of the next three weeks into the main calendar of Outlook as private, tentative appointments whose subject ends with `[koppter]`, replaces them with each plan and takes the other appointments of that calendar as busy times; the settings name the account and the weeks. *Remove koppter's blocks from Outlook* in the settings deletes them after a confirmation of their number and leaves every other appointment alone.
- The last line of the settings shows the version and the commit of the build; a long press on it copies the hash of the commit.
- A widget for the Android home screen shows the plan from now on by day, or the open tasks of the list if nothing is planned.
- *Name of the commits* and *Email address of the commits* in the git section of the settings set the committer of the commits of this device; empty, the git configuration applies, and without one `koppter` and `koppter@localhost`, which the fields name.

### Changed

- The tail rotor in the icon shows its crossed blades instead of a single line.
- On a narrow screen, as on a phone, the field for the text of the filter lies in the filter panel; a search button to the left of the button for a new task opens the panel with the field ready for typing.
- A search in the list or the inbox also finds the tasks before their start date.
- The agenda shows tomorrow and the day after it also when nothing is planned on them.
- The switches of the views are in the order *Overview*, *Agenda*, *Week*, *Board*, *List*, *Inbox*; Ctrl+1 to Ctrl+6 follow that order.
- The tasks of a tile of the overview and the tasks of the inbox have a box that completes them.
- Planning moves a task that would be done after its due date before the tasks with a higher priority, the one due first first; the agenda and the week show it at its earlier times. A low priority thus counts until the due date comes close.
- The inbox shows the hashtags of a task next to its text and puts *Give it:* before the contexts to choose from, so that they no longer look like contexts of the task.
- The empty fields *Name of the commits* and *Email address of the commits* in the settings show their default in gray, also while they do not have the focus; their titles stand above them.
- The search fields are lower, and the field that searches labels in the filter panel stands at its top instead of below *Clear all filters*, where it was out of view.
- *Choose file* and *Choose folder* in the settings close the settings and show the tasks of the chosen file, as *Clone* does; what was typed into the settings before is not written.
- `just run-loop` prints `Running koppter...` when it starts the application and `koppter finished` when the loop ends.
- The Windows task manager names the desktop application *koppter* instead of *Tasks in a todo.txt file*.

### Fixed

- The settings keep the address of a cloned repository instead of the location of its clone, and a clone that is gone, as after a new installation, is cloned again at the start; a missing clone or a repository without a todo.txt file is reported instead of showing no tasks.
- The content of the desktop window fills it again after a change of the display scale, such as when a laptop leaves its docking station.

## [2026-11-17] - 2026-11-17

### Added

- The dialog of a task has a heading, *New task* or *Edit task*, and a handle at its right edge that makes it wider or narrower with the mouse; the width is kept.
- The calendar of the deadline in the dialog of a task has the button *Use due date*.
- *Add image* in the dialog of a task attaches an image to its note, and on Android *Take photo* does so with the camera; the dialog shows the images of the note. The image is scaled down and kept as a file next to the note.
- On Android, an image that another application shares to koppter becomes a task with that image in its note.
- *Contexts of projects* in the settings states the contexts of a project, as `+project = @context`; a project that is added to a task in its dialog brings them along.
- *Rename…* in the menu of a project, a context or a hashtag of the filter panel gives it another name in all tasks and scheduled tasks, in the links and in the contexts of projects.
- *Check connection* in the settings, next to *Choose folder*, tells whether the server of the git repository in the field of the file is reached and lets the device in, without cloning.
- `just run-loop` runs the desktop application from a checkout of the sources; the application then shows *Restart to update* when the repository has newer commits, and a click pulls, builds and starts it again.

### Changed

- The heading of a group of the list, such as *Next (33)*, stays at the top while its tasks are scrolled.
- *Clone* in the settings stands below the address, next to *Check connection*, and no longer takes the place of *Save*; after the clone, the settings close and the tasks of the repository show, and nothing that stood in the settings is written.
- A clone that fails is reported with the cause below the message, and it is recorded in the log, which *Share log* passes on.
- The repository moved to `contextswitcher/koppter`: a file is checked with `jbang lint@contextswitcher/koppter todo.txt`.
- A new task starts with the priority C and the due date of the next weekend; *New task* in the settings states another priority and day, or none.
- The dialog of a task offers all known projects, contexts and hashtags, not only the first eight.
- The search text of the filter stands above the list, in a narrow window as well, where the line that added a task stood; the other criteria stay in the filter panel.
- Ctrl+Shift+A opens the dialog of a new task.

### Removed

- *Copy link* in the menu of a label of the filter panel; the label of a task still offers it.
- The line above the list that added a task as it was typed, with its completion of projects, contexts and hashtags and its names of priorities at the start of a line; a task is added in its dialog, which the button with the plus and Ctrl+N open.

### Fixed

- On Android, a clone over SSH no longer fails for the lack of a home folder of the user, which the SSH library asks for; every attempt ended with "remote hung up unexpectedly".

## [2026-11-16] - 2026-11-16

### Added

- The filter panel of a narrow window, as on a phone, has a button that closes it.
- The fields of the dialog of a task for the dates, the times and the recurrence show a short help while the pointer rests on them.
- The application starts with the view that was shown last, and with the groups of the list, the columns of the board and the tiles of the overview that were chosen.
- In the dialog of a task, the due time and the fixed time are chosen on a clock face, and the duration, the travel time and the time spent on two wheels for hours and minutes; the fields still take typed text.
- A list without groups tells next to the switches of the groups how it is ordered: by priority, then by due date.
- Ctrl+F goes to the search text of the filter, and the field has a button that empties it.
- In the dialog of a task, the recurrence is chosen on two wheels for the amount and the unit, with the choice whether it counts from the completion or from the due date; the field still takes typed text.
- The filter panel has the field *Search labels*, which narrows the projects, contexts and hashtags that it offers to those that contain the typed text.
- The link to a page that OneNote copies, pasted into the note of a task, becomes a list of the two Markdown links *OneNote Desktop* and *OneNote Web*.
- The menu of the due date and the dialog of a task offer *in three days*.
- A hashtag may start with a currency sign: `#$$$`.
- The description in the dialog of a task reads dates in words, such as `tomorrow` or `by Fri`, as the line above the list does, and marks the words that become dates.
- *Choose folder* in the settings opens the `todo.txt` file of a folder. On Android, such a folder has the notes, the scheduled tasks, the configuration and the plan next to the file, which a single chosen file has not. On the desktop, the file need not exist yet.
- For a repository of GitHub, a button in the settings copies the public SSH key and opens the page of the repository that adds it as a deploy key.
- The settings tell that the files next to the `todo.txt` file belong to it, and on Android that a file chosen with *Choose file* comes without them.

### Changed

- On Android, the back gesture closes the filter panel and returns from another view to the list; from the list, it warns, and a second back leaves the application.
- The week shows all its hours without scrolling, also on a phone that is held upright.
- The heading of a group of a priority shows the colored label of the priority, so that a priority named by a number is not read as a count.
- The dialog of a task is wider, and its description starts with the cursor at the end.
- The switch of the inbox shows the number of its tasks in parentheses: *Inbox (4)*.
- A list without groups has the headings *Overdue*, *Next* and *Done* with the number of their tasks, in place of the line below the overdue tasks.
- The description of a task stands in one line with its labels, at the height of its checkbox, where both fit next to each other.
- The groups by due date are those of the menu of the due date, from *Today* to *In a month*, with *Overdue* before them and *Later* after them; a column of the board sets its day.
- The date of the tag `t:` is called the start date instead of the threshold date: the field of the dialog of a task is *Start date*, and the option of the filter panel *Show tasks before their start date*. The tag stays `t:`.
- The day that the menu of the due date and the dialog of a task called *next week* is called *next weekend*, as it is the Sunday of the next week.
- The dialogs that choose a file come from the library FileKit: on the desktop, they are those of the operating system, and on Linux that of the desktop portal.
- A device without a keyboard has no line for a new task above the list, and the dialog of a new task starts with the cursor in the description.
- A click on the public SSH key in the settings copies it.
- A clone that fails is reported in red, the settings scroll to the message, and on a device with a key of its own it asks whether the key is registered with the server.
- The address of a page of a repository of GitHub that is pasted into the settings becomes the address for SSH of that repository.

### Fixed

- On Windows, *Choose file* in the settings no longer ends with two error messages while the file dialog is open.

## [2026-11-15] - 2026-11-15

### Added

- A project, a context or a hashtag gets a link in the settings, such as one to the section of a notebook or to a category of ContextSwitcher; the menu of its label offers *Open link*.
- A task states the time that was worked on it in the tag `spent:`, and the plan gives it the rest of its duration; the edit dialog has a field for it, and a block of today in the agenda adds its length with one click.
- The check of a `todo.txt` file warns about a tag behind a `+`, `@` or `#`: `+rec:+1w` is a project, not a recurrence.
- The filter panel hides the tasks of a project, a context or a hashtag: a right click or a long press on its label offers *Hide these tasks*, and the list, the board, the overview and the inbox then leave them out.
- A link such as `koppter://filter?project=papers>survey`, `koppter://filter?context=phone` or `koppter://filter?hashtag=someday` in another program opens the application with these tasks, on Windows and Android; an open window shows them instead of a second one. A right click on the label of a project, a context or a hashtag offers *Copy link*, as does a long press on the label in the filter panel on a touch screen.
- While there is no `ice_recur.txt`, the editor of the scheduled tasks suggests a weekly review on every Friday; saving writes the entry to the file.
- *Only the next action of each project* in the filter panel shows of each project the first task that can be worked on: one that neither has `@waiting` or `#someday` nor a threshold date in the future nor waits for another task. The overview by project names the projects without such a task.
- The tag `deadline:` states the day that others set for a task next to its due date: the task is overdue, and its plan late, only after the deadline, and a task without a due date is due on its deadline. A deadline may state a time, as `deadline:2026-11-30T12:00`, and its offset from UTC, as `deadline:2026-11-30T23:59-12:00`, which is shown as the day and the time of the device.
- `jbang lint@koppor/koppter todo.txt` checks a todo.txt file, and the notes, schedules and configuration next to it, for what the application does not read as it was probably meant; `--output-format` writes the findings for an editor, for GitHub Actions or as a table of comma-separated values.
- The overview shows its tiles by project or by hashtag as well as by context.
- On Android, *Share log* in the settings passes the end of the log as text to another application.
- The Android application writes a log as the desktop application does, to the directory `logs` of the application in the shared storage of the device and to logcat.
- The desktop application writes a log to the directory `logs` of its data directory, at most three files of one megabyte; it holds what failed, such as a synchronization or the writing of the file, and the messages of the git library.
- A click on the project label of a task shows the tasks of that project, as a click on a context label does for its context.
- A project lies below another project, written as `+papers>survey`: the label shows the last level, a filter, a barrier and a limit for `+papers` include the projects below it, and the filter panel shows the projects as a tree.
- The line that adds a task completes a project, a context or a hashtag while it is typed: Tab takes the first known name offered.
- An address of the web in the text of a task is a link that opens in the browser; a long address is cut, and the whole one appears when the pointer of the mouse rests on it.
- The list is divided into groups with headings by priority, project, context, hashtag or due date range, with the switches below the views.
- A click with the right mouse button on a task, or a click on the label of its due date, opens a menu that moves the due date to today, tomorrow, this weekend, next week, in two weeks or in a month, or removes it; the edit dialog offers the same days as chips.

### Changed

- The texts have two sizes instead of nine: one for texts, fields, buttons and titles, and a smaller one for labels and remarks; a title is told apart by its weight.
- The filter of the task list is kept on the device and is there again after a restart; another `todo.txt` file starts without it.
- The time by which a task is due on its due date is written `dueby:14:00`; the former `by:14:00` is not read anymore and has to be renamed in a file that has it.
- The guide to organizing many tasks and the skill for agents describe projects as the outline of areas and outcomes, and contexts as the kinds of work and the places; the guide compares this with areas as contexts, and the skill follows the way that the file uses.
- The edit dialog suggests a known project, context or hashtag by any part of its name, so that `rea` finds `research>reading`.
- The settings open a chosen file and a cloned repository at once and then show the configuration that comes with it, so that it is not overwritten by the one of the file that was open before; the button reads *Clone* while the field holds an address, and *Cancel* returns to the file from before.
- Without columns by filter and share actions of the user, the columns *Today*, *Next*, *Waiting* and *Someday*, which are lists of Getting Things Done, and the share actions *inbox*, which takes the shared text as it is, and *reading*, which adds `+research @reading`, are in effect, and the settings show them; an emptied field means none.
- The settings tell below the field for the file what holds for a file in a git repository.
- Synchronization through git merges `ice_recur.txt` line by line as it does `todo.txt`, and of two plans made on two devices it takes the one made last, instead of stopping at a conflict.
- The views are one row of switches of which one is always selected, the list among them, and they stand above the field that adds a task.
- *Top* is the option *Only the first 10 tasks* of the filter panel, and *Clear all filters* turns it off.
- *Plan* is a button of the top bar, and the buttons of the top bar show what they do while the pointer rests on them.
- The projects, contexts and hashtags of a task have the same colors in the edit dialog as in the list.
- Overdue tasks stand first in the list whatever their priority is, above a line that separates them from the other tasks, and they are planned first.

### Fixed

- Open tasks with the same text, such as two occurrences of a recurring task, have their own times and their own state in the plan: the plan names them with their due date, or with a number.
- The desktop application no longer starts with warnings of Java about the loading of native libraries.
- The desktop application no longer starts with a complaint of SLF4J, and warnings of the git library appear on the console.
- An order of the tasks changed on one device is no longer lost in the synchronization through git when another device changed another file meanwhile.

## [2026-11-14] - 2026-11-14

### Added

- The field for the file in the settings takes the address of a git repository, also on the desktop: the application clones it into its data directory and uses its `todo.txt` file.
- Examples in the empty fields for board columns, names of priorities and share actions in the settings.
- Packages of the desktop application for Linux (deb), Windows (msi) and macOS (dmg) with the release of a day.

### Changed

- Colors chosen in the settings apply at once.
- Without a file in the settings and without `todo.txt` in the home directory, the desktop application keeps `todo.txt` in its data directory.

- Texts are a tenth smaller, and a field keeps its height when its first character is typed.
- The filter panel has the heading *Filter*, and its text field shows *Search text* instead of the label *Text*.
- The week stretches over the height of the window.

## [2026-11-13] - 2026-11-13

### Added

- Names for priorities, such as 1, 2, 3, 5 and 8 instead of A to E: the settings take them or fill in a scale at a click, the application shows and offers them, filters accept them, and the file keeps the letters.
- Source of each extension of the todo.txt format in the README.
- Comparison with other applications for tasks in plain text files in `docs/related-work.md`, starting with Beorg.
- Order by hand: a task is dragged to its place in the list or in its column of the board, or moved one step with the arrows at its right; among tasks of the same priority and due date this orders the lines in the file, and next to a task of another priority the task takes that priority.
- Labels of the plan dialog that say that its fields are optional, in a smaller size, and the times and the barrier that hold without an entry.
- Dates in words in the line typed above the list, such as `tomorrow`, `next week`, `by Wed` or `tomorrow 3pm`: the application shows and writes the tags they stand for.

## [2026-11-12] - 2026-11-12

### Added

- Desktop application that shows the tasks of a todo.txt file with projects, contexts, tags and priority as labels at the top right of each task.
- Adding a task by typing a line in todo.txt syntax.
- Edit dialog with the description separate from the metadata and a preview of the resulting todo.txt line.
- Filter panel for completion state, text, priorities, projects, contexts and threshold date; priorities, projects and contexts are selected as the labels that the tasks carry, and the completion state with the switches *Open* and *Done*.
- Setting for the location of the todo.txt file and for writing creation and completion dates, which are not written by default.
- Android application with the same screens; the todo.txt file is kept in the storage of the application or chosen with the document picker of the system.
- Button in the settings that opens the file dialog of the platform to choose the todo.txt file.
- Synchronization through git for a todo.txt file that lies in a git repository: one commit per added, completed, reopened, edited and deleted task, merge of remote changes without conflicts, and push.
- The Android application built by the continuous integration is signed with a fixed key, so that a newer build installs over an older one.
- On Android, the settings show the public SSH key of the device and clone a git repository whose todo.txt file is then used.
- Synchronization status and a button to synchronize in the top bar.
- The edit dialog shows when a task was added and completed, derived from the git history.
- Completing a task with a recurrence tag (`rec:`, such as `rec:2w`, `rec:m` or `rec:+1y`) adds its next occurrence.
- Notes for tasks, kept as Markdown files in the directory `notes` next to the todo.txt file and named by the tag `note:`.
- Tasks scheduled in `ice_recur.txt` next to the todo.txt file are added on their days; days on which the application was not open are caught up.
- Editor for the schedule of `ice_recur.txt`, which shows for each entry the next day on which it adds its task and marks lines that are not understood.
- Colors of the palettes Nord and Everforest, each light and dark; the settings choose the palette and whether the application follows the system.
- The tag `cs:` names a task of ContextSwitcher; its label switches to that task, a pasted link of ContextSwitcher becomes the tag, and the edit dialog has a field for it.
- Application icon, a K as the cabin of a helicopter in the colors of Nord, for the window on the desktop and the launcher on Android.
- *Top* switch that restricts the list to the first tasks; their number is a setting.
- Hashtags (`#tag`) as a third kind of label next to projects and contexts: shown on the task, offered in the filter panel, the edit dialog and the filters of board columns.
- Contexts below contexts, written as `@research>reading`: the label shows the last level, a click on it shows the tasks of the context, and a filter for a context includes the contexts below it.
- The tag `dur:` holds the time a task is expected to take, such as `dur:1h30m`; the edit dialog has a field for it.
- On Android, text shared from another application becomes a task without a dialog; share actions in the settings state the context, dates and duration it gets, and each action is a target of its own in the share sheet.
- Overview with one tile per context that tells whether tasks of the context are overdue or due today; a click on a tile lists the tasks of the context by urgency, each with a bar that fills up to its due date.
- Planning: *Plan* gives each open task the earliest free times of its time map and writes the plan to `calendar.txt` and, with tentative events, to `calendar.ics`; time maps may be restricted to other time maps and are given to contexts, a barrier keeps tasks from being planned behind the end of the quarter or another date, and a calendar icon on each task tells whether its plan meets its due date.
- Limits for planning per project or context: a budget per day and per week (`max 5h/week`), a minimum per week that is planned first (`min 1h/week`), and the shortest part a task is cut into (`block 2h`, an hour by default).
- Planning gives a task that waits for others its times after them; the tags `id:` and `p:` of topydo state the dependencies.
- Planning leaves out a task whose threshold date lies behind its barrier, instead of listing it as not fitting.
- Guide on organizing many tasks in `docs/organizing.md`.
- Skill `koppter-tasks` for agents that add tasks to the todo.txt file, with guidance on recurring tasks.
- A planned block starts on a quarter of an hour after an event of a calendar.
- Planning leaves out the times of the events of calendars that are named by the address of their iCalendar file, as Outlook and Google Calendar publish it.
- *Agenda* shows the planned blocks of today and the six days after it with the tasks that are due but not planned for today, and *Week* shows the plan of a week as a calendar.
- A task states the time that the way to it takes with the tag `tt:30m`; planning keeps that time free before and after the task.
- The line that the edit dialog shows can be edited as text; the fields follow what is typed.
- A task states the time of its due date by which it has to be done with the tag `by:14:00`; the task list orders by it, and the state of the plan tells whether the task is done by then.
- A task states the time of the day at which it takes place with the tag `at:12:00`; planning gives it that time, also on the later days of an entry of `ice_recur.txt`, and plans the other tasks around it.
- *Inbox* lists the tasks without context; a click on a context gives it to the task.
- Keyboard shortcuts: Ctrl+N for a new task, Ctrl+Enter to save the edit dialog, Ctrl+1 to Ctrl+6 for the views, Ctrl+Shift+A for the line that adds a task, Ctrl+E for a search through all tasks, Ctrl+Shift+F for the text of the filter, and Ctrl+Shift+L for the plan dialog.
- A dialog with changes asks before Esc, a click beside it or the back button closes it, and offers to return to it.
- In the inbox, typing narrows the contexts and Enter gives the first one to the marked task, or the typed text as a new context.
- Board view with one column per priority, project, context, hashtag or due date range.
- Board columns defined by filter expressions in the syntax of topydo, such as `Today = due:<=today -@waiting`; they are entered in the settings and kept in `koppter.yml` next to the todo.txt file.
- On the board, dragging a task to another column changes its priority, project, context or due date accordingly; for a column defined by a filter, the task is changed as little as possible so that it matches the filter.
- Changes that other programs make to the todo.txt file are picked up while the application runs.
- The release of a day on GitHub carries the signed Android application as an APK file.
- Project infrastructure: Gradle build with a Kotlin Multiplatform `core` module, requirements tracing, decision records and continuous integration.

[Unreleased]: https://github.com/contextswitcher/koppter/compare/v2026-11-20...HEAD
[2026-11-20]: https://github.com/contextswitcher/koppter/compare/v2026-11-19...v2026-11-20
[2026-11-19]: https://github.com/contextswitcher/koppter/compare/v2026-11-18...v2026-11-19
[2026-11-18]: https://github.com/contextswitcher/koppter/compare/v2026-11-17...v2026-11-18
[2026-11-17]: https://github.com/contextswitcher/koppter/compare/v2026-11-16...v2026-11-17
[2026-11-16]: https://github.com/contextswitcher/koppter/compare/v2026-11-15...v2026-11-16
[2026-11-15]: https://github.com/contextswitcher/koppter/compare/v2026-11-14...v2026-11-15
[2026-11-14]: https://github.com/contextswitcher/koppter/compare/v2026-11-13...v2026-11-14
[2026-11-13]: https://github.com/contextswitcher/koppter/compare/v2026-11-12...v2026-11-13
[2026-11-12]: https://github.com/contextswitcher/koppter/releases/tag/v2026-11-12
