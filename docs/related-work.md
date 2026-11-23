# Related work

This document compares koppter with other applications that manage tasks in plain text files.
It is filled one application at a time.
The applications that served as models for parts of koppter are listed in the [README](../README.md#related-work).

## Beorg

Beorg is a task manager for iOS that keeps its data in plain text files.
The comparison follows the review [Beorg may well be the best free iOS plain text task manager](https://ellanew.com/2025/07/21/ptpl-166-beorg-best-free-ios-plain-text-task-manager) by Ellane W of July 2025: the column for Beorg states what the review says, and Beorg itself has not been tried for this document.

| Aspect | Beorg, according to the review | koppter |
| --- | --- | --- |
| Platforms | iOS; the same files on a Mac with EasyOrg or Emacs | Android, Windows and Linux; no iOS |
| Price | Free; extensions are paid | Free and open source |
| Data | Plain text files of Org mode, which comes from Emacs | One plain text file in the todo.txt format, next to it files for notes, schedules and the plan |
| Work without a connection | Yes | Yes |
| Synchronization of the files | Not covered by the review | git, a folder of the device, or a storage service such as OneDrive through the platform |
| Agenda | Agenda view for scheduled and repeating tasks | *Agenda* and *Week* show the plan, which is computed from the durations of the tasks, the time maps and the fixed times |
| Repeating tasks | Yes | `rec:` after completion, `ice_recur.txt` by the calendar |
| Overdue tasks | Appear on the following day | Stay in the list and are marked; the overview shows the contexts that have overdue tasks |
| Calendar | Tasks are synchronized to Apple Calendar and Reminders; tasks created in Reminders are shown and cannot be edited | Reads calendars published as iCalendar files and plans around their events; writes the plan to `calendar.ics` for import; writes nothing into a calendar |
| Alerts | Alerts of the system through Reminders | None |
| Widgets | Several widgets, each with its own search | None |
| Subtasks | Any number | None; a task can wait for other tasks (`id:`, `p:`), and contexts have levels |
| Notes | Any number of notes per task | One note file per task (`note:`) |
| Attachments | None; links to files | None |
| Filtering | Less than the Tasks plugin of Obsidian offers: no complex boolean logic, date queries and tag combinations | Filter panel for priorities, projects, contexts and hashtags; board columns by filter expressions with dates and negation |
| Planning of the day | Not covered by the review | *Plan* gives each task times according to its duration, time map, limits and dependencies |
| Collaboration | None | None; the git repository serves the devices of one person |

What the review counts as the strength of Beorg, tasks in the calendar of the system with its alerts, and widgets, is what koppter does not have.
koppter in turn plans the tasks into the free times of the day, which the review does not report for Beorg.

## tuxedo

[tuxedo](https://github.com/webstonehq/tuxedo) is a task manager for the terminal that reads and writes a todo.txt file.
It is written in Rust and is operated with the keys of the editor Vim.
The comparison follows its README of November 2026: the column for tuxedo states what the README says, and tuxedo itself has not been tried for this document.

| Aspect | tuxedo, according to its README | koppter |
| --- | --- | --- |
| Platforms | A terminal on Linux, macOS and Windows | Android, Wear OS, and Windows and Linux desktops; no terminal |
| Operation | The keyboard alone: `j` and `k` move the marked task, `x` completes it, `dd` deletes it, `p` changes its priority; the keys are set in a file | Mouse and touch; keys open the views, the search and the dialog of a new task, and none acts on a task of the list |
| Command line | The commands of the todo.txt command line, such as `add`, `do`, `pri` and `list`, with the output as JSON if wanted | The check of a file only |
| Completed tasks | *Archive* moves them to `done.txt`; a view shows that file by the day of completion, puts a task back and deletes one | Stay in the `todo.txt` file, below *Done*; those completed before the last month are moved to `done.txt` at the start of a month, and the application does not show that file |
| Undo | 50 steps | None; in a git repository, the history holds each change |
| Several tasks at once | A visual mode marks tasks, which are then completed, deleted or copied together | None |
| Copying | The line of a task, or its text without metadata, to the clipboard | The link to a label; the line is selected by hand in the edit dialog |
| Search | A text, with a range of due dates such as `due:+1w`; a search is saved under a name and chosen again | A text, and a panel for priorities, projects, contexts and hashtags; filter expressions with dates and negation define the board columns; the last filter is kept, and none has a name |
| Order | By priority, by due date or as in the file | By priority, then due date, then as in the file; a task is moved by dragging |
| Groups | By range of the due date | By priority, project, context, hashtag or range of the due date |
| Recurring tasks | `rec:` after completion or strictly by the due date, with business days | The same `rec:`, and `ice_recur.txt` by the calendar |
| Start date | `t:` | `t:` |
| Dates in words | Dates, recurrence, the start before the due date, priority, project and context | Dates and times; no recurrence |
| Notes | `note:` names a file in a directory of notes, which the editor of the user opens | `note:` names a file in `notes` next to the `todo.txt` file, which the edit dialog edits |
| Tags that are not shown | A setting lists the tags to leave out of the rows | None |
| Appearance | Five themes and those of files of the user; three densities; line numbers | The palettes Nord and Everforest and three of the themes of tuxedo |
| Capture on a phone | A page that the phone opens from a QR code writes to `inbox.txt`, which is merged into the `todo.txt` file | The Android application, to which other applications share a text |
| Synchronization | None; left to other tools | git, a folder of the device, or a storage service through the platform |
| Changes by other programs | Read within a quarter of a second | Read within two seconds |
| Board, plan, agenda, overview, dependencies, hashtags, levels of projects and contexts | None | Yes |

What koppter does not have and tuxedo has is, in the order of its use to someone who has koppter already: work on the list with the keyboard alone, undo, a view of the archived tasks, changes to several tasks at once, searches with a name, the commands of the todo.txt command line, and recurrence in words.
None of these is planned yet.

An interface for the terminal is not planned either.
`TodoController` needs only the runtime of Compose, so that a library such as [Mosaic](https://github.com/JakeWharton/mosaic), which is Compose for the terminal, could show its state; the list, the filter, the inbox and the agenda fit a terminal, the board and the editor of time maps do not.
Against it stand a second interface to keep in step and the start of a Java virtual machine, which takes about a second.
Keys that act on a marked task of the list, and undo, would give the desktop application most of what tuxedo offers at the keyboard.

## Applications still to compare

* [Other Todo.txt Projects](https://github.com/todotxt/todo.txt-cli/wiki/Other-Todo.txt-Projects), the list of applications for the todo.txt format in the wiki of the todo.txt command line.
* The other applications that the review names: NotePlan and the Tasks plugin of Obsidian.
