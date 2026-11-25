# Related work

This document states the idea koppter started from and compares koppter with other applications: those that manage tasks in plain text files, and those that plan tasks into a calendar.
It is filled one application at a time.
The applications that served as models for parts of koppter are listed in the [README](../README.md#related-work).

## Idea and roadmap

Many applications bring tasks and a calendar together, and [Mitra](https://github.com/a11delavar/mitra), a calendar for events and tasks to host oneself, shows that the interest continues.
[SkedPal](https://www.skedpal.com/) has a data model and a planning algorithm that work well, but what it plans is hard to follow: the tool just does it, and the data lie in its own store.
The [todo.txt format](https://github.com/todotxt/todo.txt), in turn, is a text file that a person reads and that a language model handles well, and its tools do not plan.
The idea of koppter is to bring the two together: tasks in todo.txt, planned into the free times of a calendar, with a plan that is a text file as well.

The roadmap that followed from the idea, with its state:

| Step | State |
| --- | --- |
| 1. Survey of the planning applications, as a table of features by tools | Open; [Planning applications](#planning-applications) lists the tools, and [organizing.md](organizing.md#coming-from-skedpal) maps the terms of SkedPal |
| 2. Survey of the todo.txt format: its variants and its tools | Begun: the [README](../README.md#related-work), the comparisons below and [Applications still to compare](#applications-still-to-compare) |
| 3. What the todo.txt format offers for the features of step 1 | Done feature by feature, in the context of each [decision](../decisions/README.md) on a tag; not as one table |
| 4. Extensions of the format where needed | Done as tags and as files next to the `todo.txt` file; see [Extensions of the todo.txt format](../README.md#extensions-of-the-todotxt-format) |
| 5. Planning with calendars | Done; see [Planning](../README.md#planning) and [Calendars](../README.md#calendars) |
| 6. Optional: import of existing tasks | Tasks of Outlook only; see [Tasks of Outlook](../README.md#tasks-of-outlook) |
| 7. Evaluation: the table of step 1 with a column for koppter | Open |

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

## Planning applications

These applications put tasks into a calendar.
Each line states what the linked page says; none of them except SkedPal has been tried for this document, and the table of features by tools is still to be written.

| Application | Tasks | Planning | Calendar |
| --- | --- | --- | --- |
| [SkedPal](https://www.skedpal.com/) | Its own store; an integration with Asana | Automatic, from goals, preferences and constraints | Synchronizes with the calendars of Microsoft, Google and iCloud |
| [Mitra](https://github.com/a11delavar/mitra) | Its own database on the server of the user; AGPL-3.0 | By hand: a task with a due date and an estimate is dragged into the week | Its own, or synchronized in both directions with CalDAV, Google Calendar, Apple Calendar, Notion, Todoist and Tempo |
| [TimeFinder](https://www.heise.de/tests/Planungshelfer-TimeFinder-fuer-Mac-iPhone-und-iPad-im-Test-11310515.html) for Mac, iPhone and iPad, according to the open part of the review by heise of June 2026 | A list beside the day | By hand: a task dragged onto the day becomes an appointment; a longer appointment moves the later tasks of the day | The appointments of the day are shown next to the tasks |
| [Taskline](https://alternativeto.net/software/taskline/about/) for Windows, according to AlternativeTo; commercial | The tasks of Outlook | Automatic | The calendar of Outlook |
| koppter | The `todo.txt` file | Automatic, from priority, duration, time maps, barriers, limits and dependencies | Reads calendars published as iCalendar files and the calendar of Outlook; writes the plan to `calendar.txt` and `calendar.ics` and, on Windows, into the calendar of Outlook |

[Timefold](https://timefold.ai) is not an application for tasks but a constraint solver, with an open source edition, for timetables and routes.
The planner of koppter is a greedy one, which gives each task in the order of the list the earliest free times; a solver would be the way to go if the plan were to be optimized, for example to meet the due dates that the [limits of planning](../README.md#limits-of-planning) name.

## Links to the context of a task

Most applications for tasks do not link a task to what it is about: the mail, the document, the place in a PDF.
[Hookmark](https://hookproductivity.com/help/general/features/) for macOS does that for any pair of items: links in both directions between items of different applications, links to a mail and to a selection in a PDF, and links to files that hold when the file is renamed or moved.

koppter links a task to its context with tags: `note:` for a file of notes, a tag for the task of ContextSwitcher ([decision 0016](../decisions/0016-tag-for-the-task-of-contextswitcher.md)), for an item of Outlook ([decision 0040](../decisions/0040-tag-for-an-item-of-outlook-with-its-entryid-in-base64.md)) and for a page of OneNote ([decision 0055](../decisions/0055-tag-for-a-page-of-onenote-with-its-link-as-copied.md)), and an address in the text of the task is opened by a click.
There is one tag per kind of target, and the link leads from the task to the target only.

## Other formats

* [vtodo](https://github.com/chyyynh/vtodo) keeps the tasks of a software project in `.vtodo/todos.json` with a status, tags, a checklist and an expected duration, and shows them as a board; it is made for work with coding agents.
  It has no dates and no calendar, and, despite the name, the page states no relation to the `VTODO` of iCalendar.

## Applications still to compare

* [Other Todo.txt Projects](https://github.com/todotxt/todo.txt-cli/wiki/Other-Todo.txt-Projects), the list of applications for the todo.txt format in the wiki of the todo.txt command line.
* The other applications that the review names: NotePlan and the Tasks plugin of Obsidian.
* The applications and tools for the todo.txt format that [AlternativeTo](https://alternativeto.net/software/todotxt/about/) lists as alternatives.
* [topydo](https://github.com/topydo/topydo) and [Sleek](https://github.com/ransome1/sleek), which the README names as models, in full.
