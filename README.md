# koppter

![Logo of koppter: a K as the cabin of a helicopter](docs/logo.svg)

koppter manages tasks in a plain text file with one task per line.
It is an application for Android and for Windows and Linux desktops.

```text
(A) Call the dentist @phone +Health due:2026-11-20
```

This line is a task of priority A with the context *phone*, the project *Health* and a due date.
The notation is the [todo.txt format](https://github.com/todotxt/todo.txt): the file is read and changed with any text editor, and other tools for the format work on the same file.

## Goals

* The `todo.txt` file is the only data store, and it stays a file that other todo.txt tools can read and write.
* One code base for phone and desktop: Kotlin Multiplatform with Compose Multiplatform ([decision 0001](docs/decisions/0001-kotlin-multiplatform-with-compose-multiplatform.md)).
* Synchronization through git, with one commit per added and per completed task.
  The dates a task was added and completed are then read from the history instead of being written into the file.
* Synchronization through a local folder and through OneDrive.
* A task list that shows projects, contexts, hashtags, due date and priority as labels at the right of each task, divided into groups by one of them if wanted.
* An edit dialog that separates the description from the metadata and shows the resulting todo.txt line.
* A filter panel for completion state, text, priorities, projects, contexts and hashtags.
* A Kanban board.
* Support for the [extensions](#extensions-of-the-todotxt-format) `note`, `top` and `ice_recur`, for the `rec:` and `t:` tags, and for hashtags.

The planned scope is tracked as requirements in [docs/requirements](docs/requirements/README.md).

## Skills for agents

[skills/koppter-tasks](skills/koppter-tasks/SKILL.md) is a skill for Claude and other agents that read skills: it tells how to write a task into the `todo.txt` file so that it fits what the application does with it, and how to choose between `rec:` and `ice_recur.txt` for a recurring task.
To use it in Claude Code, link the directory into the skills of the user:

```sh
ln -s "$PWD/skills/koppter-tasks" ~/.claude/skills/koppter-tasks
```

## Overview

The task list with the filter panel.
The field above the list searches the text of the tasks.
Projects, contexts, tags and the priority are labels at the right of each task: in the line of its description where both fit, above it otherwise.
An address of the web in the text of a task is a link that opens in the browser.
A long address is cut after 50 characters; the whole one appears when the pointer of the mouse rests on it, and the edit dialog shows it as well.

![Task list with the filter panel on the left](docs/screenshots/list.png)

The filter panel offers priorities, projects, contexts and hashtags as the labels that the tasks carry.
The field *Search labels* below *Clear all filters* narrows the projects, contexts and hashtags that are offered to those that contain the typed text; `@` at its start shows contexts only, `+` projects and `#` hashtags.
A right click on a project, a context or a hashtag there, or a long press on a touch screen, offers *Hide these tasks*: the tasks with that label are left out, such as those of an import that carry `#import`, until the label, which then stands after *not*, is clicked.
*Rename…* in the same menu gives the label another name in all tasks, the completed ones and the scheduled tasks included; a project or a context below it follows, and so do the [links](#links-of-projects-and-contexts) and the [contexts of projects](#contexts-of-projects); the limits and board columns of `koppter.yml` are changed by hand.
The filter and the view are kept on the device: after a restart, the application shows what it showed before.
A click moves a label to the selected ones beside the title; here, the list shows the tasks of the projects *GarageSale* or *Work* that have the context *phone*.

![Task list filtered by two projects and a context](docs/screenshots/filter.png)

The board with one column per priority, while a task is dragged from column B to column A.

![Board by priority with a task being dragged to another column](docs/screenshots/board-priority.png)

The board with one column per project.

![Board with one column per project](docs/screenshots/board-project.png)

The board with [columns defined by filters](#board): *Now* is `due:<=today -@waiting`, *Waiting* is `@waiting`, *This week* is `due:<1w`, *Important* is `(>C)`.

![Board with the columns Now, Waiting, This week and Important](docs/screenshots/board-filter.png)

The overview tells how each context is doing: red if a task is overdue, yellow if one is due today.

![Overview with one tile per context](docs/screenshots/overview.png)

A click on a context shows its tasks, the most urgent first; the bar of a task fills from its start to its due date.

![Tasks of a context with bars that show their urgency](docs/screenshots/overview-context.png)

*Plan* with the time maps, the time maps of contexts and the barriers; the calendar icons of the tasks tell the state of their plan.

![Plan dialog with time maps and barriers](docs/screenshots/plan.png)

The edit dialog: the description without metadata at the top, the metadata below, then the note, the resulting line of the `todo.txt` file, and the date the task was added according to the git history.
The line can be edited as text as well: the fields follow what is typed there.
The dialog offers all known projects, contexts and hashtags, and typing into *Add project* and the like narrows them.
With a mouse, the handle at its right edge makes the dialog wider or narrower; the width is kept.
The calendar of the deadline has the button *Use due date*.
The link to a page that OneNote copies, pasted into the note, becomes a list of two Markdown links: *OneNote Desktop* and *OneNote Web*.

![Edit dialog of a task](docs/screenshots/edit.png)

A date, a time, a duration and a recurrence are typed or chosen: the button of a field opens a calendar, a clock face, two wheels for hours and minutes, or two wheels for the amount and the unit of the interval.

![Clock face that chooses the due time](docs/screenshots/time-picker.png)

![Wheels that choose the duration](docs/screenshots/duration-picker.png)

![Wheels that choose the recurrence](docs/screenshots/recurrence-picker.png)

The settings, with the board columns by filter and the public SSH key for the git repository.

![Settings dialog](docs/screenshots/settings.png)

The editor of the scheduled tasks tells for each entry on which day it adds its task next.

![Editor of the scheduled tasks](docs/screenshots/schedule.png)

In a narrow window, as on a phone, the filter panel is a drawer.
On Android, the back gesture closes the drawer, then returns from another view to the list, and from the list warns first: a second back leaves the application.
The button with the plus opens the dialog of a new task, with the cursor in the description.

![Task list in a narrow window](docs/screenshots/narrow.png)

The colors are those of the palette [Nord](https://www.nordtheme.com/) or [Everforest](https://github.com/sainnhe/everforest), light or dark; the settings choose, and by default the application follows the system ([decision 0015](docs/decisions/0015-colors-from-published-palettes.md)).
The screenshots above show Nord in its light variant.

![Task list in the dark variant of Nord](docs/screenshots/nord-dark.png)

## Organizing many tasks

[docs/organizing.md](docs/organizing.md) describes a way to keep hundreds of tasks findable with what the application offers:
projects for the outline of areas and outcomes, contexts for the kinds of work and the places, start dates for what is not due yet, and a rhythm of inbox, agenda and plan.
It compares this with the other way, in which the areas are the contexts, so that each file can decide.
It also maps the terms of SkedPal to those of the application.

## Getting Things Done

The application covers the lists and steps of Getting Things Done (GTD), the method of David Allen, with the todo.txt format and its extensions.

| In the method | In the application |
| --- | --- |
| Capture | The line that adds a task, and [sharing](#sharing-to-koppter-on-android) on Android: a task needs nothing but its text |
| Clarify the inbox | The [inbox](#inbox) lists the open tasks without context; a few keys give each its context |
| Next actions by context | Contexts as `@phone`, and the option *Only the next action of each project* of the filter panel; the list in [groups](#groups-in-the-list) by context, the [overview](#overview-of-the-contexts) and the board column *Next* |
| Projects | Projects as `+Thesis`, with levels as `+papers>survey`; the list in groups by project |
| Waiting for | The context `@waiting` and the board column *Waiting* |
| Someday/maybe | The hashtag `#someday` and the board column *Someday* |
| Calendar | The dates `due:` and `deadline:` and the time `at:`; the [agenda and the week](#agenda-and-week) |
| Tickler file | The start date `t:`, which hides a task until its day, and [recurring tasks](#recurring-tasks) |
| Reference material | The note of a task, a Markdown file next to the `todo.txt` file |
| Weekly review | A task on every Friday, which the editor of the [schedule](#by-the-calendar-the-file-ice_recurtxt) suggests for a new `ice_recur.txt`; the overview by project names the projects without next action, and the [plan](#planning) lists what does not fit |

The [board](#board) by filter starts with the lists of the method, until other columns are entered in the settings:

```text
Today = due:<=today -@waiting
Next = -@waiting -#someday
Waiting = @waiting
Someday = #someday
```

Dragging a task to *Someday* adds `#someday`, and dragging it to *Next* removes `#someday` and `@waiting`.

The next action of a project is its first task in the [order of the list](#order-of-the-list) that can be worked on ([decision 0032](docs/decisions/0032-next-actions-derived-from-the-tasks.md)).
A task cannot be worked on if it has the context `@waiting`, the hashtag `#someday` or a start date in the future, or if it waits for another open task through `id:` and `p:`.

* *Only the next action of each project* in the filter panel shows one task per project, and each task without project.
  Together with a context in the filter, it shows what is next at the phone or at home.
* The overview by project marks a tile with *No next action* if the open tasks of the project all wait or are for someday.
  For a project below the one of the tile, it names the project: *No next action: survey*.
* A project whose tasks are all completed is no longer in the file as a project and is not marked.

## Files

| File | Content |
| --- | --- |
| `todo.txt` | The tasks |
| `notes/<name>.md` | The note of the task with the tag `note:<name>.md`, as Markdown |
| `notes/<name>-<number>.jpg` | An [image](#images-of-a-task) that the note shows |
| `koppter.yml` | Configuration of this application that belongs to the `todo.txt` file: the [board columns defined by filters](#board), the [share actions](#sharing-to-koppter-on-android), the [names of the priorities](#names-for-priorities), the [links of projects and contexts](#links-of-projects-and-contexts), [what a new task starts with](#what-a-new-task-starts-with), the [contexts of projects](#contexts-of-projects), and the time maps and barriers for [planning](#planning) |
| `ice_recur.txt` | Schedules of [recurring tasks](#recurring-tasks) in the format of the ice_recur add-on, such as `weekly ; day Monday - (B) Plan the week` |
| `calendar.txt` | The [plan](#planning): when the open tasks are worked on, one line per day |
| `calendar.ics` | The plan as tentative events, for import into a calendar program |

If these files lie in a git repository, the application commits every change and synchronizes with the remote repository.
Changes of two devices are merged without a conflict to resolve: `todo.txt` and `ice_recur.txt` line by line, where a line that both devices changed differently is kept in both versions, and of two plans the one made last is taken.
A note or `koppter.yml` that two devices changed at the same place stops the synchronization, which then names the file.
This holds for the synchronization of the application; a `git pull` on the command line merges as git does.
A repository that is not cloned yet is given by its address, such as `git@github.com:user/todo.git`, in the field for the file in the settings: the application clones it into its own directory and uses the `todo.txt` file of the clone.
A cloned repository, a file chosen with *Choose file* and the `todo.txt` file of a folder chosen with *Choose folder* are opened at once, and the settings then show the board columns, names of priorities and share actions that come with them.
*Cancel* returns to the file that was open before.
On the desktop, it uses the SSH configuration of the user for that; on Android, the key that the settings show.
A click on that key copies it.
With the address of a repository of GitHub in the field, the button below the key copies the key and opens the page of the repository that adds a deploy key: paste the key there and tick *Allow write access*.
The address of a page of the repository on GitHub, such as `https://github.com/user/todo`, is taken as well: pasted into the field, it becomes the address for SSH.
With an address in the field, *Check connection* tells whether the server is reached and lets the device read the repository, without cloning it; if not, it shows the reason.
On Android, a file chosen with *Choose file* is opened alone, as the document picker gives no access to the files next to it; a folder chosen with *Choose folder* has them.
On the desktop, *Choose folder* takes the `todo.txt` file of the folder, also where there is none yet: a folder that is the clone of a git repository is committed and synchronized.
The dialogs are those of the platform, shown through [FileKit](https://github.com/vinceglb/FileKit) ([decision 0036](docs/decisions/0036-file-dialogs-and-document-access-through-filekit.md)).

A file in OneDrive, or in another storage service, is used through the platform ([decision 0011](docs/decisions/0011-onedrive-through-the-platform.md)): on Android, choose it with *Choose file*, or its folder with *Choose folder*, in the settings; on the desktop, choose it in the synchronized folder.

## Checking a file

A file that is edited by hand, by a script or by an agent is checked on the command line with [jbang](https://www.jbang.dev/), without the application:

```shell
jbang lint@contextswitcher/koppter todo.txt
```

Before its first run from the web, jbang asks whether it trusts the address; `jbang trust add https://github.com/contextswitcher/koppter/` answers that beforehand, which a run without a terminal needs.
In a clone of this repository, `jbang .jbang/KoppterLint.kt todo.txt` checks with the sources of the clone.
Each finding is one line, such as `todo.txt:12: error: due:tomorrow is not a date as YYYY-MM-DD`, and the command ends with 1 if an error is among them, so that it serves as a check of a repository of tasks.
The option `--output-format` before the file selects another form of the findings:

| Format | Findings |
| --- | --- |
| `errorformat` | `file:line: severity: message`, as above; the default |
| `github-actions` | Annotations at the lines of a pull request, for a workflow of GitHub Actions |
| `csv` | A table with the columns file, line, severity and message |

| Severity | Findings |
| --- | --- |
| error | A value of `due:`, `deadline:`, `t:`, `rec:`, `dur:`, `tt:`, `at:` or `dueby:` that is not read; `p:` without a task of that `id:`; an `id:` of two tasks; a `note:` whose file does not exist; a line of `ice_recur.txt` that is not understood; a time map or limit in `koppter.yml` that is not understood |
| warning | A tag given twice; a tag with a space after its colon; a tag behind a `+`, `@` or `#`, such as `+rec:+1w`, which is a project; `dueby:` without `due:`; a start date after the due date; a line that a change of its task would write differently; a context, project or hashtag that is written elsewhere with other capitals |
| info | The open tasks without context, which are in the [inbox](#inbox) |

Only open tasks are checked.

## Extensions of the todo.txt format

The [todo.txt format](https://github.com/todotxt/todo.txt) defines the completion mark `x`, the priority `(A)`, dates, `+project`, `@context` and tags of the form `key:value`.
The application understands the following extensions.
The column *Source* names the tool or document that defines each of them; those of this application are tags that other todo.txt tools keep as they are.

| Notation | Meaning | Source | How to use it |
| --- | --- | --- | --- |
| `due:2026-12-24` | Due date | [todo.txt format](https://github.com/todotxt/todo.txt#additional-file-format-definitions), which names it as the example of a tag | *Due date* in the edit dialog. The label is highlighted on the day and marked when the date has passed; the list orders tasks of equal priority by due date. |
| `t:2026-12-01` | Start date, which Simpletask calls the threshold date: the task is not relevant before that day | [Simpletask](https://github.com/mpcjanssen/simpletask-android/blob/master/app/src/main/assets/index.en.md#extensions) | *Start date* in the edit dialog. The task is hidden until that day; *Show tasks before their start date* in the filter panel shows it. |
| `rec:1w` | Recurrence: completing the task adds its next occurrence | [topydo](https://github.com/topydo/topydo/wiki/Recurrence), with the business days of [Simpletask](https://github.com/mpcjanssen/simpletask-android/blob/master/app/src/main/assets/index.en.md#extensions) | See [recurring tasks](#recurring-tasks). |
| `ice_recur.txt` | Schedules that add tasks on certain days | [ice_recur](https://github.com/rlpowell/todo-text-stuff/blob/master/ice_recur), an [add-on](https://github.com/todotxt/todo.txt-cli/wiki/Todo.sh-Add-on-Directory#ice_recur-another-recurring-task-action-but-with-much-more-flexibility) of the todo.txt command line | See [recurring tasks](#recurring-tasks). |
| `note:k3f.md` | A note for the task, kept in the file `notes/k3f.md` | [note](https://github.com/todotxt/todo.txt-cli/wiki/Todo.sh-Add-on-Directory#note--take-notes-related-to-tasks), an add-on of the todo.txt command line | *Note* in the edit dialog; the application names the file. New notes are Markdown files, which the dialog edits as text; a note named otherwise, such as `note:k3f.txt`, keeps its file. As in the [note add-on](https://github.com/mgarrido/todo.txt-cli/tree/note/todo.actions.d), which reads these notes with `TODO_NOTE_EXT=.md` in its configuration. |
| `#urgent` | Hashtag: a third kind of label next to `+project` and `@context`, without a fixed meaning | This application | *Add hashtag* in the edit dialog, or typed into the task. A hashtag starts with a letter or a currency sign, as `#$$$` does: `#123` and `C#` stay words of the description ([decision 0014](docs/decisions/0014-hashtags-as-a-third-kind-of-label.md)). |
| `@research>reading` | A context below another context, over any number of levels | This application | Typed as the context. The label shows `@reading`, and the path when the pointer rests on it; a click on it shows the tasks of the context. A filter for `@research` includes the contexts below it, and the filter panel shows the contexts as a tree ([decision 0017](docs/decisions/0017-levels-of-a-context-separated-by-a-greater-than-sign.md)). |
| `+papers>survey` | A project below another project, over any number of levels | This application | Typed as the project. The label shows `+survey`, and the path when the pointer rests on it; a click on it shows the tasks of the project. A filter for `+papers` includes the projects below it, as do a barrier and a limit of the [plan](#planning), and the filter panel shows the projects as a tree ([decision 0026](docs/decisions/0026-levels-of-a-project-written-like-those-of-a-context.md)). |
| `dur:1h30m` | The time the task is expected to take, in hours and minutes | This application | Field *Duration* in the edit dialog, or typed into the task. [Planning](#planning) gives the task that much time ([decision 0018](docs/decisions/0018-tag-for-the-duration-of-a-task.md)). |
| `dueby:14:00` | The time of the due date by which the task has to be done | This application | Field *Due time* in the edit dialog, or typed into the task next to `due:`. Among the tasks of a day, the earlier time comes first, and the icon of the [plan](#planning) is red if the task is planned to end later ([decision 0030](docs/decisions/0030-tag-for-the-time-of-the-due-date-named-dueby.md)). |
| `deadline:2026-11-30` | The day that others set for the task, next to the day to have it done by | This application | Field *Deadline* in the edit dialog, or typed into the task; see [Deadline next to the due date](#deadline-next-to-the-due-date) ([decision 0028](docs/decisions/0028-tag-for-the-deadline-next-to-the-due-date.md)). |
| `at:12:00` | The time of the day at which the task takes place | This application | Field *Fixed time* in the edit dialog, or typed into the task. [Planning](#fixed-times) gives the task that time and plans the others around it ([decision 0021](docs/decisions/0021-tag-for-the-fixed-time-of-a-task.md)). |
| `spent:1h30m` | The time that was worked on the task so far | This application | Field *Time spent* in the edit dialog, the button of a block in the agenda, or typed into the task. [Planning](#time-spent) gives the task the rest of its duration ([decision 0034](docs/decisions/0034-tag-for-the-time-spent-on-a-task.md)). |
| `tt:30m` | The time that the way to the task takes, and the way back as well | This application | Field *Travel time* in the edit dialog, or typed into the task. [Planning](#travel-time) keeps that time free before and after the task ([decision 0023](docs/decisions/0023-tag-for-the-travel-time-of-a-task.md)). |
| `id:1`, `p:1` | The task with `id:1` waits for the tasks with `p:1`, as topydo writes dependencies | [topydo](https://github.com/topydo/topydo/wiki/Dependencies) | Typed into the task. [Planning](#planning) gives the task its times after those it waits for ([decision 0020](docs/decisions/0020-dependencies-with-the-tags-of-topydo.md)). |
| `cs:jabref/fix-npe` | The task of [ContextSwitcher](https://github.com/contextswitcher/contextswitcher) that holds the working context | This application | Entered in the field *ContextSwitcher task* of the edit dialog or typed into the task, as the id or as the link that *Copy link* of ContextSwitcher gives, which becomes the tag. A click on the label switches to the task in ContextSwitcher, on a computer where it has registered its links ([decision 0016](docs/decisions/0016-tag-for-the-task-of-contextswitcher.md)). |
| `pri:A` | The priority a completed task had | [todo.txt format](https://github.com/todotxt/todo.txt#complete-tasks-2-format-rules) | Written when a task with a priority is completed, as the format has no priority for completed tasks; reopening the task restores the priority. |

The option *Only the first 10 tasks* of the filter panel restricts the list to the first tasks, whose number is a setting, as the add-on [top](https://github.com/todotxt/todo.txt-cli/wiki/Todo.sh-Add-on-Directory#top-see-the-top-tasks) of the todo.txt command line does.
There is no notation for a most important task as the add-ons [mit](https://github.com/todotxt/todo.txt-cli/wiki/Todo.sh-Add-on-Directory#mit-most-important-task) and [mitf](https://github.com/todotxt/todo.txt-cli/wiki/Todo.sh-Add-on-Directory#mitf-most-important-task-in-the-future) have it: priority `(A)` together with a due date serves that purpose ([decision 0009](docs/decisions/0009-no-notation-for-most-important-tasks.md)).

Notes, `ice_recur.txt`, the board columns by filter, the names of the priorities and the links of projects and contexts need a `todo.txt` file in a folder or a git repository.
They are not available for a single file chosen through the document picker of Android, as the application then has access to that one file only; a folder chosen there has them.

### Recurring tasks

There are two ways to make a task recur.
They differ in what triggers the next task: completing the previous one, or the calendar.

#### After completion: the tag `rec:`

Use it for a task whose next occurrence only makes sense once the current one is done, such as watering the plants.
There is one open task at a time.

1. Add the task with a due date and a recurrence, in the edit dialog or as a line: `Water the plants due:2026-11-12 rec:1w`.
2. Complete the task when it is done.
3. The application marks it as completed and adds the same task with the next due date.

The recurrence is an amount and a unit; without an amount it is one unit.

| Unit | Meaning | Example |
| --- | --- | --- |
| `d` | days | `rec:3d` |
| `b` | business days, Monday to Friday | `rec:b` |
| `w` | weeks | `rec:2w` |
| `m` | months | `rec:m` |
| `y` | years | `rec:1y` |

The next due date counts from the day of completion.
With a `+` in front of the interval, it counts from the previous due date, however late the task was completed:

| Task | Completed on | Next task |
| --- | --- | --- |
| `Water the plants due:2026-11-12 rec:1w` | 2026-11-14 | `Water the plants due:2026-11-21 rec:1w` |
| `Pay the rent due:2026-11-01 rec:+1m` | 2026-11-03 | `Pay the rent due:2026-12-01 rec:+1m` |

A start date moves along with the due date and keeps its distance to it.
A task with a start date only gets the next start date, and a task with neither date gets a due date.
The next task keeps the priority.

#### By the calendar: the file `ice_recur.txt`

Use it for a task that has to appear on certain days whether or not the previous one is done: every Monday, on the first of the month, on the last Friday of the month.
The format is that of the [ice_recur](https://github.com/rlpowell/todo-text-stuff/blob/master/ice_recur) add-on.

1. Open the editor with the calendar button in the top bar.
   It edits the file `ice_recur.txt` next to the `todo.txt` file, which any text editor can edit as well; the file name is fixed, as in the add-on.
2. Write one entry per line: `[@start] [@@end] timing - task`.
   The part after ` - ` is the task as it is added to the `todo.txt` file.
3. The application adds the tasks of the day when it is open, once per day.
   The days since it was last open are caught up.
   A task of an entry that is added or changed in the editor and occurs today is added at once.
4. Below the text, the editor tells for each entry the next day on which it adds its task, and which lines it does not understand.

Until the file exists, the editor suggests the entry `weekly ; day Friday - Weekly review @review dur:1h`, the weekly review of [Getting Things Done](#getting-things-done).
Nothing is scheduled until the entry is saved, which writes it to `ice_recur.txt`.

| Entry | Adds the task |
| --- | --- |
| `daily - (A) Check the mail` | every day |
| `weekly ; day Monday - (B) Plan the week` | every Monday |
| `weekly ; day Friday, Sunday - Empty the bins` | every Friday and Sunday |
| `@2026-11-02 weekly 2 ; day Thursday - Put out the paper bin` | every second Thursday, counted from the week of 2026-11-02 |
| `monthly ; day_of_month 1, 15 - Check the account` | on the 1st and the 15th of every month |
| `monthly ; day_of_month -1 - Send the time sheet` | on the last day of every month |
| `monthly ; day_of_week Friday => -1 - Back up the photos` | on the last Friday of every month |
| `monthly ; day_of_week Tuesday => 1, 3 - Team meeting` | on the first and the third Tuesday of every month |
| `@2026-03-01 monthly 3 - Read the meters` | every three months on the day of the start date: 1 March, 1 June, … |
| `@2026-01-04 yearly - Renew the membership` | every year on 4 January |
| `yearly ; month_of_year March, September - Change the tires` | on the 1st of March and of September |
| `@2026-06-01 @@2026-08-31 daily - Water the garden` | every day from 1 June to 31 August 2026 |

* The timing starts with `daily`, `weekly`, `monthly` or `yearly`, optionally followed by an interval: `weekly 2` is every second week.
* Further restrictions follow, each separated by ` ; ` with a blank on both sides: `day`, `day_of_month`, `day_of_week`, `day_of_year`, `month_of_year`.
* Days and months are written as English names or as numbers; for days of the week, 0 is Sunday.
  A negative number counts from the end: `day_of_month -1` is the last day of the month.
* Without a start date, intervals count from 2018-01-01, a Monday, as in the add-on.
  An entry with an interval greater than one should therefore have a start date.
* A line that starts with `#` is a comment.

A task is not added while an open task with the same text exists, so tasks that are not completed do not pile up.
Two tasks have the same text if they differ at most in priority, dates and the tags `t:`, `due:` and `note:`.

The application differs from the add-on in these points:

* The add-on is started once a day by a scheduler; the application runs the schedules itself, and only while it is open.
* The add-on always writes the creation date; the application writes it only if writing dates is switched on in the settings.
* The add-on stops at a line it does not understand; the application skips the line and marks it in its editor.
* The options of the add-on to ignore projects and contexts when comparing tasks do not exist.
* Each device remembers on its own when it last ran the schedules.
  In a git repository, a task that any device added since then counts as existing, also when it is completed already.
  Without git, a second device may add a task again that was added and completed on the first.

### Deadline next to the due date

`due:` is the day to have a task done by, and the day that the menu of a task moves.
A task whose official day is a later one states that day as `deadline:`, which the application does not move:

```text
File the tax return @home due:2026-11-20 deadline:2026-11-30
```

* The days between the two are a buffer: after the due date, the task is highlighted as due and not as overdue, and a [plan](#planning) that ends in the buffer has a yellow icon and not a red one.
* After the deadline, the task is overdue and its plan is late, wherever the due date is moved to.
* A task with a deadline and without a due date is due on its deadline.
* A deadline at a time of the day is written `deadline:2026-11-30T12:00`: a plan that ends on that day after that time is late ([decision 0029](docs/decisions/0029-time-of-a-deadline-as-part-of-its-date.md)).
  The due date stays a date that other programs read, with its time in `dueby:`.
* A deadline that is given for another place states the offset of its time from UTC, as ISO 8601 writes it: `deadline:2026-11-30T23:59-12:00` is the end of that day "anywhere on Earth" ([decision 0031](docs/decisions/0031-offset-from-utc-in-the-time-of-a-deadline.md)).
  The application counts with the day and the time of the device, here `2026-12-01 12:59` in Germany, and shows them in the label; the pointer on the label shows what is written.
* The next occurrence of a recurring task has its deadline moved as far as its due date.

## Images of a task

An image belongs to the note of a task ([decision 0038](docs/decisions/0038-images-of-a-task-as-files-next-to-its-note.md)).
*Add image* below the note in the dialog of a task chooses one, and on Android *Take photo* takes one with the camera.
The image is scaled down to at most 1600 pixels on its longer side and written as JPEG to the directory `notes`, next to the note, which gets a line for it:

```markdown
The receipt of the van:

![](k3f-1.jpg)
```

* The dialog shows the images of the note below it.
  It shows those that the note names by a file name; an image at an address is left to a program that shows Markdown.
* On Android, an image that another application shares to koppter becomes a task with that image; a text that comes with it is the description, and without one the task is called *Image*.
* Removing the line from the note removes the image from the task; the file stays in `notes`.
* Images need the files next to the `todo.txt` file: a folder or a git repository.

## Dates in words

The description in the dialog of a task may name its dates in words instead of tags; the dialog marks these words.
`Write the report next week` is written as `Write the report t:2026-11-16 due:2026-11-22`.
The phrases are those that [SkedPal](https://docs.skedpal.com/planning/setting-a-plan-for-a-task) reads in the title of a task; the examples are typed on Friday, 13 November 2026.

| Typed at the end of the text | Written |
| --- | --- |
| `today`, `tomorrow` | `due:2026-11-13`, `due:2026-11-14` |
| `this week`, `next week` | `due:2026-11-15`, `t:2026-11-16 due:2026-11-22` |
| `this month`, `next month` | `due:2026-11-30`, `t:2026-12-01 due:2026-12-31` |
| `weekend` | `t:2026-11-14 due:2026-11-15` |
| `Wed`, `on Wed`, `on Sep 15` | `t:` and `due:` on that day |
| `by Wed`, `due by Sep 15`, `Sep 15` | `due:` on that day |
| `by Mon 14:00` | `due:2026-11-16 dueby:14:00` |
| `tomorrow 3pm`, `15 Jul 14:00`, `on Wed at 9:30` | `due:` on that day and `at:15:00`, `at:14:00`, `at:9:30` |
| `at 3pm` | `at:15:00` |
| `in 4 days`, `start in two weeks`, `begin in a month` | `t:` that much later |
| `from Jan 18`, `start on Jan 18`, `delay until Jan 18`, `no earlier than Jan 18` | `t:2027-01-18` |
| `from tomorrow to Wed`, `from Dec 15 to Dec 17` | `t:` on the first and `due:` on the second day |
| `the week of Dec 9` | `t:2026-12-07 due:2026-12-13` |

* A day is `today`, `tomorrow`, a weekday, a day of a month such as `Sep 15`, `15 Sep` or `September 15th`, or a date such as `2026-12-24`.
* A weekday or a day of a month is the first such day from today on; a week runs from Monday to Sunday.
* The phrase stands at the end of the text; projects, contexts, hashtags and tags may follow it: `Buy milk tomorrow @shop`.
* While typing, the dialog shows the line that will be written.
  If words are not meant as a date, end the description with a full stop: `Buy tickets for the weekend.` stays as it is.
* A `due:`, `t:`, `at:` or `dueby:` tag that is typed as well stays.
* The phrases are English.
* Recurrence is not typed in words: use `rec:` or `ice_recur.txt`.

## Completion while typing

The fields of the edit dialog that add a project, a context or a hashtag narrow the known names to those that contain what is typed, so that `rea` offers `@research>reading` and `survey` offers `+papers>survey`.
A click takes one of them.

## Order of the list

The list shows the open tasks first, and among them the overdue ones: those whose due date has passed.
Without groups, the overdue tasks stand below the heading *Overdue*, the other open ones below *Next* and the completed ones below *Done*; within a group, a line separates the overdue tasks from the others.
Each of these parts is ordered by priority, then by due date and due time, then by the position of the lines in the file.
A task is moved by dragging it with the mouse, or with the finger after a long press, or one step at a time with the two arrows at its right.
A line shows where a dragged task would be put.

* Next to a task of the same priority and due date, the line of the task is moved there in the file.
  This orders the tasks that are equally important and due on the same day, such as those of a weekend.
* Next to a task of another priority, the task takes that priority: `(B)` becomes `(A)`.
  With an arrow, the first step at such a border changes the priority, and the next one passes the task above.
* Within a priority, the due date keeps its order: a task cannot pass one that is due on another day.
  Its arrow is disabled, and dragging shows no line there.
* If the task and the one it is put next to are due on different days, it takes the priority and the due date decides its place.
* No task crosses the line below the overdue tasks: the due date decides on which side a task stands.

A click with the right mouse button on a task, or a click on the label of its due date, opens a menu that moves the due date to today, tomorrow, this weekend, in three days, next weekend, in two weeks or in a month, or removes it; a weekend stands for its Sunday.
The edit dialog offers the same days as chips above the due date, which is the way to them on a phone for a task without a due date.
A weekend and a week end on their Sunday.
An overdue task that is moved this way leaves the upper part of the list.

![Menu that moves the due date of a task](docs/screenshots/due-menu.png)

On the [board](#board), a card is dragged to a place in its column in the same way.
Dragged to another column, it changes what the column stands for and keeps its priority otherwise, so that it gets its place only among the cards of that priority.
The list and the columns of the board scroll while a task is held at their upper or lower edge, and the board scrolls sideways while a card is held at its left or right edge.

## Groups in the list

The switches below the views divide the list into groups: by priority, project, context, hashtag or due date range.
The ranges of the due date are those of its menu: overdue, today, tomorrow, this weekend, in three days, next weekend, in two weeks, in a month and later.
Each group stands below a heading with its title and the number of its tasks, a priority with its colored label, and a second click on the switch shows the list without groups again.
Without groups, a text next to the switches names the [order of the list](#order-of-the-list).
A task with two projects stands in the groups of both.

![Task list divided into groups by project](docs/screenshots/list-project.png)

Within a group, the tasks have the [order of the list](#order-of-the-list), and a task is moved within its group only.
The [board](#board) shows the same groups side by side and gives a task another project, context or priority by dragging.

## Names for priorities

The todo.txt format has the priorities `(A)` to `(Z)`, of which A is the highest.
For another scale, such as numbers, *Names of the priorities* in the settings takes one priority per line as `letter = name`:

```text
A = 1
B = 2
C = 3
D = 5
E = 8
```

The buttons below the field fill in a scale: the letters, the numbers 1 to 5, or five or eight steps of the Fibonacci sequence.

* The labels of the tasks, the filter panel, the board by priority and the edit dialog then show 1, 2, 3, 5 and 8.
* The edit dialog offers the priorities that have a name, here five; without names, it offers A to F.
* The file keeps the letters, so that other todo.txt tools read the priorities and order by them ([decision 0024](docs/decisions/0024-names-for-priorities-kept-in-the-configuration-file.md)).
* The order is that of the letters: the name of A is the highest priority, so `A = 8` to `E = 1` makes 8 the highest.

The names are kept in `koppter.yml` and are therefore the same on every device that synchronizes the file.
A filter of a board column names a priority by its name or by its letter: with the names above, `(>3)` and `(>C)` both select the priorities 1 and 2.
The comparison reads *a higher priority than*, not *a greater number than*.
The line of the edit dialog and `ice_recur.txt` use the letters.

## Overview of the contexts

The switch *Overview* above the tasks shows one tile per context of the outermost level instead of the tasks.
A tile counts the open tasks of the context and of the contexts below it, and its dot tells the state of the most urgent one: overdue, due today, or neither.
The tasks without context have a tile of their own.
The switches below the views show the tiles *by project* or *by hashtag* instead: one tile per project of the outermost level with the projects below it, or one per hashtag.
A tile of a project tells if the project, or one below it, has [no next action](#getting-things-done).
The filter panel applies to the overview as it does to the list.

A click on a tile lists the tasks of the context, the most urgent first and those without due date last.
The bar of a task shows how much of its time has passed: from its start date (`t:`) to its due date, else from its creation date, else over the two weeks before the due date.

## Inbox

*Inbox* above the tasks lists the open tasks without context, and the switch tells in parentheses how many there are.
Each task shows the contexts of the file; a click on one gives it to the task, which then leaves the inbox.

With the keyboard, one task is marked.
Typing narrows the contexts of the marked task, and Enter gives it the first one shown: `rea` and Enter for `@research>reading`.
If no context contains the typed text, Enter gives the text as a new context.
The arrow keys up and down mark another task, to leave one where it is.

![Inbox with the contexts to choose from](docs/screenshots/inbox.png)

## Planning

*Plan*, the arrow in the top bar, gives each open task times in which it is to be worked on and writes them to `calendar.txt` next to the `todo.txt` file.
The `todo.txt` file leads: the plan is computed from it, and planning does not change it.

* A task with a higher priority is planned first, and every task gets the earliest free times, so that tasks without dates are planned soon as well.
* A task takes its duration (`dur:`), half an hour without one, and is not planned before its start date (`t:`).
  A free time that is too short takes a part of the task, but no part is shorter than an hour, the **block**; a task that is shorter is not cut.
* A task follows the task before it without a gap; after an event of a calendar, it starts on a quarter of an hour.
* A task that waits for others is planned after them: `Send the paper id:1` waits for `Write the paper p:1`.
  The tasks it waits for come as early as the task itself would.
* A **time map** names times of the week: `General = Mon-Fri 08:00-12:00 13:00-18:00`.
  A map that starts with the name of another one is restricted to its times: `Morning-Focus = General 08:00-11:00`.
  The first map is for tasks whose contexts name none.
* A context gets a time map: `@research = Morning-Focus`.
  It holds for the contexts below it, such as `@research>reading`, unless they name their own.
* A **barrier** is the day behind which a task is not planned: `week`, `month`, `quarter` (the default), `year` or a date.
  It is set in general (`default = year`), for a project (`+Thesis = 2027-03-31`) or for a context (`@research = year`); the earliest barrier of a task counts.
  A barrier for a project or a context holds for those below it, such as `+Thesis>chapter1`.
  A task that does not fit before its barrier is listed after planning instead of being planned later.
  A task whose start date lies behind its barrier is not due yet: it is neither planned nor listed.
* A **limit** states how much is planned for a project or a context, the contexts below it included, as rules separated by commas:

  | Rule | Effect |
  | --- | --- |
  | `@chores = max 90m/day, max 5h/week` | Budget: not more is planned on a day or in a week, which starts on Monday. |
  | `@paper = min 1h/week` | Minimum: this much is planned in each week before the tasks with a higher priority get their times. |
  | `+Thesis = block 2h` | The shortest part its tasks are cut into; of several, the longest counts. |
  | `default = block 30m, max 6h/day` | For the tasks without a block of their own, and a budget for all tasks together. |

  A paper that is finished one day is a project below a project that stays, `+papers>FirstPaper`: a limit of `+papers` then holds for every paper, and `+papers>FirstPaper` gets its barrier.

After the first plan, each open task carries a calendar icon: green if its plan ends before its due date or it has none, yellow if it ends on the due date, red if it ends later or the task is not planned.
For a task that is due at a time of the day, such as `due:2026-11-16 dueby:14:00`, red also tells that the plan ends on that day after that time.
The pointer on the icon shows the start of the plan.

`calendar.txt` has the [format of calendar.txt](https://terokarvinen.com/2021/calendar-txt/), one line per day:

```text
2026-11-16 w47 Mon  08:00-09:00 Read the survey @research>reading. 09:00-09:30 Call the landlord @phone
```

The lines of past days stay; the lines from today on are written anew by every plan, so what is written there by hand is lost.
Open tasks with the same text are told apart in the plan by their due date, as `Prepare the lecture @home (due 2026-11-16)`, and by a number if that is the same as well, as `Water the plants @home (2)`; the `todo.txt` file does not hold these additions.
In a git repository, each plan is one commit ([decision 0019](docs/decisions/0019-the-plan-is-derived-and-kept-in-calendar-txt.md)).
The plan is also written to `calendar.ics`, with tentative events, for import into a calendar program.

### Fixed times

A task with the tag `at:` is not moved by planning: it takes the time it states, for its duration, and the other tasks are planned around it.
Use it for what has its time, such as lunch, a training or an appointment, so that the `todo.txt` file is the one place where it is written.

| Line | Where | Planned |
| --- | --- | --- |
| `Dentist at:14:00 dur:1h due:2026-11-20` | `todo.txt` | on 20 November from 14:00 to 15:00 |
| `Call the tax office at:9:30` | `todo.txt` | today from 9:30 to 10:00 |
| `weekly ; day Monday, Tuesday, Wednesday, Thursday, Friday - Lunch at:12:00 dur:45m` | `ice_recur.txt` | on every working day from 12:00 to 12:45 |
| `weekly ; day Tuesday - Training @sport at:18:30 dur:1h30m` | `ice_recur.txt` | on every Tuesday from 18:30 to 20:00 |

* The day is the due date, else the start date, else today.
* The time holds whatever the time maps and the calendars say: a training in the evening needs no time map for the evening.
* For an entry of `ice_recur.txt`, the plan has the time on the later days as well, before the task of the day is added: for as long as tasks are planned, and for four weeks at least.
* The task of the day is completed like any other task, which takes its block from the next plan.
* A time that is over is not planned any more.
* The time counts for a budget of the project and the contexts of the task.

### Time spent

A task with the tag `spent:` was worked on already: the plan gives it the rest of its duration, and the estimate stays in `dur:`.

| Line | Planned |
| --- | --- |
| `Write the report dur:4h spent:2h30m` | 1 hour and 30 minutes |
| `Review the paper dur:1h spent:1h15m` | 30 minutes: the time is used up and the task is open, so it counts as a task without duration |

* In the agenda, each block of today has a button such as *+45m spent*, which adds the length of the block to the tag.
* The field *Time spent* of the edit dialog holds the sum and corrects it.
* A completed task keeps both tags and tells what it was estimated at and what it took; the next occurrence of a recurring task starts without time spent.

### Travel time

A task with the tag `tt:` takes place somewhere else: the plan keeps the stated time free before and after it, for the way there and the way back.

| Line | Planned |
| --- | --- |
| `Dentist at:15:00 dur:1h tt:30m due:2026-11-20` | the way from 14:30, the dentist from 15:00 to 16:00, the way back until 16:30 |
| `Fetch the parcel @errands dur:30m tt:20m` | a free time of 70 minutes: 20 minutes for the way, 30 for the parcel, 20 for the way back |

* The ways are blocks of their own, which the plan shows as `Travel:` followed by the task.
* A task with a travel time is not cut into parts, so it needs a free time that holds it with both ways.
* The ways do not count for a budget of the project and the contexts of the task.

### Agenda and week

*Agenda* above the tasks shows the blocks of today and of the six days after it, each with its task, which is completed and opened there as in the list.
Below the blocks of today are the tasks that are due today or overdue and have no block today.

![Agenda with the blocks of the next days](docs/screenshots/agenda.png)

*Week* shows the blocks of a week as a calendar; a click on a block opens its task.

![Plan of a week as a calendar](docs/screenshots/week.png)

Both show what `calendar.txt` says: after tasks changed, *Plan* brings it up to date.
A block whose task was completed, deleted or edited since shows the text it was planned with.

### Calendars

*Calendars with busy times* in the plan dialog takes the addresses at which calendar services publish a calendar as an iCalendar file, one per line.
Nothing is planned into the times of their events; events of whole days, canceled events and events marked as free do not count.

* Outlook on the web: *Settings → Calendar → Shared calendars → Publish a calendar*, then the ICS link.
* Google Calendar: *Settings → the calendar → Integrate calendar → Secret address in iCal format*.

Such an address lets everyone who has it read the calendar.
It is therefore kept in the settings of the device and not in `koppter.yml`; each device needs it entered once.
The application only reads the calendars and writes nothing to them.

### Limits of planning

* A budget counts what is planned from now on: what was planned earlier in the running week is not counted.
* A minimum is planned in the weeks up to the day on which the tasks of its context would be done without it; a week in which they have no time left gets nothing.
* A due date that has not passed, with or without a time, does not move a task forward in the plan: the order of the task list decides, and the icon tells when the plan ends too late.
  An overdue task is planned before the others, as it stands first in the list.
  A higher priority gives the task earlier times.
* The list marks a task as overdue from the day after its due date on, not from the time by which it was due.
* The travel time is one flat value per task: two tasks at the same place after each other each get both ways, and a way that is longer in one direction cannot be stated.
* A task with a fixed time and without a date counts for today as long as it is open: a training of Tuesday that was not completed is planned again on Wednesday.
  Complete or delete it when its day is over.
* A task with `rec:` and a fixed time takes its time on its current due date only; the later ones follow when it is completed.
  What recurs at a fixed time belongs in `ice_recur.txt`.
* A fixed time is planned also where a calendar has an event or another task has a fixed time; nothing reports the overlap.
* The task list does not tell which tasks wait for others, and it shows them all the same; the tags `id:` and `p:` are typed, and their values chosen, by hand.

* In the last days of a quarter, little fits before the default barrier, and most tasks count as not planned.
  `default = year` or a date moves the barrier.
* A block refers to its task by the text of the task.
  After the text, a project or a context of a task is edited, the task counts as not planned until the next plan.
* A time zone that the platform does not know by its name, as Outlook writes them (`W. Europe Standard Time`), counts as the time zone of the device.
  Events of a calendar that is kept in another time zone than that of the device are then read at the wrong time.
* An occurrence of a repeating event that was moved counts as busy at its old and at its new time.
* Whether a calendar program replaces the events of an earlier import of `calendar.ics` or adds them again has not been tried.
  The events carry the same id in every export, and the application leaves its own events out when it reads a calendar they were imported into.
* The events of the calendars are not shown in the agenda and the week: only their times are read, when planning.
  A task is not coupled to an event either.
* Reading calendars and the import of `calendar.ics` have been tried with files written for the tests, not with the calendars of Outlook or Google.

## Keyboard

| Keys | Effect |
| --- | --- |
| Ctrl+N, Ctrl+Shift+A | New task in the edit dialog |
| Ctrl+Enter | In the edit dialog: save |
| Esc | Close a dialog; if it holds changes, it asks first and offers to return to it |
| Ctrl+E | Search all tasks: reset the filters, show the list with completed tasks and those before their start date, go to the text of the filter |
| Ctrl+F, Ctrl+Shift+F | Go to the text of the filter and keep the other filters; from the agenda and the week, this shows the list |
| Ctrl+1 to Ctrl+6 | List, board, overview, agenda, week, inbox |
| Ctrl+Shift+O, Ctrl+Shift+Y, Ctrl+Shift+C | List, agenda, week, as in SkedPal outline, today list and calendar |
| Ctrl+Shift+L | Plan dialog |

After a search with Ctrl+E, *Clear all filters* shows the open tasks again.
On macOS, the command key takes the place of Ctrl.
There is no key yet that acts on a task of the list, such as completing or deleting it: the list has no marked task.

## Sharing to koppter on Android

A text that another application shares to koppter, such as the link of an article, becomes a task at once; no screen opens.
*Share actions* in the settings state what the task gets besides the shared text, one action per line as `name = task`:

```text
inbox = {}
reading = {} +research @reading due:2w dur:1h
```

* `{}` stands for the shared text; without it, the shared text stands in front.
* `due:` and `t:` may be relative to the day of sharing: `2w`, `3d`, `tomorrow`.
* From Android 10 on, each action is a target of its own in the share sheet, named as the action; the entry *koppter* applies the first action.
* Until actions are entered, there are the two actions of this example: `inbox` takes the shared text as it is, and `reading` is there to choose in the share sheet.
  An emptied field in the settings means no actions; the shared text alone is then the task.
* The actions are kept in `koppter.yml` next to the `todo.txt` file, so that they can be edited on the desktop.
  They need a `todo.txt` file in the storage of the application or in a git repository, not one of a document provider.
* Words of the shared text that start with `+`, `@` or `#` become projects, contexts and hashtags of the task.

## Links to koppter

A link in another program, such as a page of OneNote, opens the application with the tasks of a project, a context or a hashtag ([decision 0033](docs/decisions/0033-links-to-the-application-with-a-query.md)):

```text
koppter://filter?project=papers>survey
koppter://filter?context=phone
koppter://filter?hashtag=someday
koppter://filter?project=Fair&context=phone
```

* A right click on the label of a project, a context or a hashtag at a task offers *Copy link*.
  The copied link has signs such as `>` encoded, as `koppter://filter?project=papers%3Esurvey`, so that the other program takes the whole link.
* The link replaces the filter and shows the list.
* A link can name the rest of the filter as well: `not-hashtag=import` leaves tasks out, `priority=A` selects a priority, `text=mom` searches, `show=all` or `show=done` includes completed tasks, and `future=1` those before their start date.
  A project or a context includes those below it.
* If the application is open, its window shows the tasks and comes to the front; no second window opens.
* On Windows, the installed application registers the links for the current user when it starts, so it has to be started once before the first link works.
* On Android, the installed application opens the links.
* The packages for Linux and macOS do not register the links yet.

## Links of projects and contexts

A project, a context or a hashtag has a link of its own, for example to the section of a notebook that holds its material.
*Links of projects and contexts* in the settings takes one link per line as `label = link`, the label with its sign:

```text
+Fair = contextswitcher://switch-category/fair
@phone = onenote:https://example.org/Notebook/Numbers.one
#someday = https://example.org/ideas
```

* A right click on the label, at a task or in the filter panel, offers *Open link*.
  On a touch screen, a long press on the label in the filter panel does.
* The link is of any kind that the device opens: an address of the web, a link of OneNote, or a link of ContextSwitcher.
  `contextswitcher://switch-category/<name>` switches to a category there and opens its note, as the tag `cs:` does for a single task.
* A project or a context below another one, such as `+Fair>Booth`, has the link of that one unless it has its own.
* The links are kept in `koppter.yml` and are therefore the same on every device that synchronizes the file ([decision 0035](docs/decisions/0035-links-of-projects-and-contexts-in-the-configuration-file.md)).
  A link to a program that a device does not have is reported as not opened there.

## What a new task starts with

The dialog of a new task starts with the priority C and the due date of the next weekend, the Sunday of the next week.
*New task* in the settings changes that:

```text
priority = C
due = next weekend
```

* `priority` takes a priority by its letter or by its [name](#names-for-priorities), such as `3`.
* `due` takes a day as the menu of the due date names it (`today`, `tomorrow`, `this weekend`, `in three days`, `next weekend`, `in two weeks`, `in a month`) or an interval from today, such as `10d`.
* A line that is left out is not set, and without lines a new task starts empty.
* The values are kept in `koppter.yml` under the key `new-task` and are therefore the same on every device that synchronizes the file.
  A `todo.txt` file without the files next to it starts a new task empty.
* A task that is shared to the application on Android has what its [share action](#sharing-to-koppter-on-android) states instead.

## Contexts of projects

*Contexts of projects* in the settings takes one project per line as `+project = @context`:

```text
+Fair = @errands
+Garden = @home @weekend
```

* A project that is added to a task in its dialog, with a click on its name or typed into the field next to the names, adds these contexts to the task.
  A context added this way is removed like any other; it comes back only when the project is added again.
* A project below another one, such as `+Fair>Booth`, brings the contexts of that one as well.
* A project typed into the description brings no contexts.
* The contexts are kept in `koppter.yml` and are therefore the same on every device that synchronizes the file.

## Board

The board shows one column per priority, project, context, hashtag or due date range.
Dragging a task to another column changes that attribute of the task; the column of a due date range sets its day, such as the Sunday for *This weekend*.

Columns can also be defined by filters, one per line as `title = filter`.
They are entered in the settings and kept in `koppter.yml` next to the `todo.txt` file, so that they are synchronized with the tasks ([decision 0012](docs/decisions/0012-board-columns-by-filter-in-a-file.md)):

```text
Now = due:<=today -@waiting
Waiting = @waiting
This week = due:<1w
Important = (>C)
```

Until columns are entered, there are the four columns of [Getting Things Done](#getting-things-done); an emptied field in the settings means no columns by filter.

| Term | Matches a task |
| --- | --- |
| `+project`, `@context`, `#hashtag` | with this project, context or hashtag |
| `(A)`, `(>B)`, `(<=C)` | with this priority, a higher one, this or a lower one; a priority is named by its letter or by its [name](#names-for-priorities) |
| `due:today`, `due:<1w`, `t:>=2026-12-01`, `status:doing` | whose tag has this value or compares so to it; dates are ISO dates, `today`, `tomorrow`, `yesterday` or an interval from today |
| `-term` | that does not match the term |
| any other word | whose line contains it |

Dragging a task to such a column changes the task as little as possible so that it matches the filter ([decision 0013](docs/decisions/0013-smallest-change-on-drop-on-a-filter-column.md)): projects, contexts and tags are added or, for negated terms, removed, and a priority or date is set to the nearest value that matches.
Nothing else is removed, so the task stays in its former column as well unless the filter of the new column excludes that.
A column whose filter contains a word or a negated comparison does not take tasks.

In `koppter.yml`, a filter that contains a hashtag has to be quoted (`Doing: "#doing"`), as YAML reads an unquoted `#` as the start of a comment.
The application writes all filters quoted.

## Installing

The [latest release](https://github.com/contextswitcher/koppter/releases/latest) carries the application as files to install:

| File | System |
| --- | --- |
| `koppter-<date>.apk` | Android |
| `koppter_<version>_amd64.deb` | Linux with Debian packages, such as Debian and Ubuntu |
| `koppter-<version>.msi` | Windows; installs for the current user |
| `koppter-<version>.dmg` | macOS on Apple Silicon |

There is one release per day on which something changed, named after that day.
The version of a desktop package is that day as `YY.M.D`, such as `26.11.14`.
The desktop packages bring their own Java runtime.
They are not signed: Windows and macOS warn before they open them, and macOS opens the application only after it is allowed under *Privacy & Security* in the system settings.

## Building

Requirements: a JDK that starts Gradle (17 or newer) and the Android SDK (`ANDROID_HOME`, or `sdk.dir` in `local.properties`).
The build itself runs on JDK 25 and compiles for it.
The Gradle wrapper downloads everything else, including that JDK and the Android platform the build needs.

```shell
./gradlew build traceRequirements
```

`build` compiles and runs the unit tests and the user interface tests.
`traceRequirements` checks that every requirement and decision is covered ([OpenFastTrace](https://github.com/itsallcode/openfasttrace)); the report is written to `build/reports/tracing.txt`.

## Running

```shell
./gradlew :app:run
```

starts the desktop application.
It reads the file that is set in the settings, which may also be given as the address of a git repository, see [Files](#files).
Without one, it reads `todo.txt` in the home directory if that file exists, and otherwise `todo.txt` in its data directory: `%APPDATA%\koppter` on Windows, `~/.local/share/koppter` on Linux ([decision 0025](docs/decisions/0025-locations-on-the-desktop-from-appdirs.md)).
The directory `logs` in the data directory holds the log of the application, with what failed, such as a synchronization, and the messages of the git library: `koppter-0.log` and following, the highest number being the newest, at most three files of one megabyte.
On Android, the log is in `Android/data/io.github.koppor.koppter/files/logs` of the shared storage, which a computer connected by USB shows, and warnings and errors go to logcat as well.
*Share log* in the settings passes the end of the newest log file as text to another application, such as a mail program.
A log can name the address of the git repository: look at it before passing it on.

```shell
just run-loop
```

runs the desktop application from the checkout and keeps it current; it needs [just](https://just.systems), and `just loop` does the same.
It pulls, builds the application and starts it.
The application then looks every half hour whether the repository has newer commits and, if so, shows *Restart to update* in its top bar.
A click ends it with the exit code 55, on which the loop pulls, builds and starts it again; closing the window ends the loop.
Without just, `scripts/run-loop.sh` or `scripts\run-loop.cmd` does the same.

```shell
./gradlew :android:assembleDebug
```

builds the Android application as `android/build/outputs/apk/debug/android-debug.apk`.
The workflows build the release variant, signed with a key that is a secret of the repository ([decision 0010](docs/decisions/0010-signing-key-as-repository-secret.md)): the build workflow publishes it as the artifact `koppter-android`, the daily release workflow attaches it to the release.

## Project documents

| Document | Content |
| --- | --- |
| [CHANGELOG.md](CHANGELOG.md) | User-visible changes, [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format, [CalVer](https://calver.org/) releases |
| [docs/organizing.md](docs/organizing.md) | A way to organize many tasks with contexts, projects, dates and the plan |
| [docs/related-work.md](docs/related-work.md) | Comparison with other applications for tasks in plain text files |
| [docs/requirements](docs/requirements/README.md) | Features, requirements and design, traced to code |
| [docs/decisions](docs/decisions/README.md) | Architectural decisions as [MADR](https://adr.github.io/madr/) |
| [CLAUDE.md](CLAUDE.md) | Conventions for development sessions with Claude Code |

## Related work

These projects served as models; [docs/related-work.md](docs/related-work.md) compares koppter with further applications.

* [todo.txt-cli](https://github.com/todotxt/todo.txt-cli) — `todo.sh`, the command line of the format.
  Its [add-on directory](https://github.com/todotxt/todo.txt-cli/wiki/Todo.sh-Add-on-Directory) is the source of `note`, `top` and `ice_recur` and lists more that this application does not cover, such as [gimme](https://github.com/todotxt/todo.txt-cli/wiki/Todo.sh-Add-on-Directory#gimme-select-a-task-at-random-based-on-criteria), which selects a task at random.
* [Sleek](https://github.com/ransome1/sleek) — desktop todo.txt manager; model for the label style and the edit dialog.
* [mindstream](https://github.com/xuhcc/mindstream) — model for the label placement in the list.
* [Web Todo.txt](https://github.com/EpocDotFr/webtodotxt) — model for the filter panel.
* [Simpletask](https://github.com/cdelahousse/simpletask-android) — Android todo.txt app with file-based synchronization.
* [topydo](https://github.com/topydo/topydo) — model for the Kanban columns.

## License

[MIT](LICENSE)
