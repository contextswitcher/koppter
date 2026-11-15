# todo.txt format

Requirements and design for reading and writing the todo.txt file.
The format is specified in [todo.txt format](https://github.com/todotxt/todo.txt).

## Requirements

### Read the todo.txt format
`req~read-todo-txt~1`

The application reads each non-blank line of the file as one task with completion marker, priority, completion date, creation date, projects (`+name`), contexts (`@name`) and tags (`key:value`).

Covers:

* `feat~todo-txt-file~1`

Needs: dsn

### Leave untouched lines unchanged
`req~untouched-lines-unchanged~1`

A line that the user did not change in the application is written back exactly as it was read.
The application thus causes no differences in a file that is also edited by hand or by other tools, which keeps the history of a synchronized file readable.

Covers:

* `feat~todo-txt-file~1`

Needs: dsn

### Description separate from metadata
`req~description-separate-from-metadata~1`

The application distinguishes the description of a task from its metadata.
The description is the text without projects, contexts, hashtags and tags.

Covers:

* `feat~task-editing~1`

Needs: dsn

### No dates by default
`req~no-dates-by-default~1`

By default, the application writes neither a creation date when a task is added nor a completion date when a task is completed.
A setting switches the writing of both dates on.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Restore the priority of a reopened task
`req~priority-survives-completion~1`

A task that is completed and later reopened has the priority it had before.

Covers:

* `feat~task-editing~1`

Needs: dsn

### Filter criteria
`req~filter-criteria~1`

The user restricts the list by completion state (all, completed, open), by a text, by priorities, by projects and by contexts.
Tasks with a threshold date (`t:`) in the future are hidden unless the user asks for them.

Covers:

* `feat~filtering~1`

Needs: dsn

### List order
`req~list-order~1`

The list shows the tasks that need attention first: open before completed, overdue before the others, higher priority first, earlier due date first.

Covers:

* `feat~task-list~1`

Needs: dsn

### Order by hand
`req~list-step~1`

The user orders the tasks of the same priority and due date by hand: with a step up or down, or by dragging a task to its place.
A task that crosses the border to another priority takes that priority.

Covers:

* `feat~task-list~1`

Needs: dsn

### Recurrence tag
`req~recurrence-tag~1`

The application understands the recurrence tag `rec:` with an amount and one of the units day (`d`), business day (`b`), week (`w`), month (`m`) and year (`y`), optionally preceded by `+` for a recurrence that counts from the due date.
Without an amount, the interval is one unit: `rec:w` is `rec:1w`.

Covers:

* `feat~extensions~1`

Needs: dsn

### Check of a file
`req~lint~1`

The user checks a todo.txt file, and the files of the application next to it, for what the application does not read the way it was probably meant.
The check runs on the command line without the application, so that it also serves a file that is edited by hand or by an agent and a repository that checks its files on every change.

Covers:

* `feat~todo-txt-file~1`

Needs: dsn

## Design

### Task model
`dsn~task-model~1`

`Task` holds the completion marker, the priority and the two dates as fields and the remainder of the line as `body`.
Projects, contexts, tags and the description are computed from the space-separated tokens of the body:

* `+name` is a project, `@name` a context.
* `key:value` is a tag, unless the value starts with `/` (a URL) or the key starts with a digit (a time).
* All other tokens form the description.

Covers:

* `req~read-todo-txt~1`
* `req~description-separate-from-metadata~1`

Needs: impl, utest

### Task parser
`dsn~task-parser~1`

`Task.parse` reads, in this order: `x` followed by a space, a priority `(A)` to `(Z)`, and dates in the form `YYYY-MM-DD`.
A completed task has up to two dates, completion date first; an open task has at most a creation date.
Text that does not match, such as a lowercase priority or an impossible date, belongs to the body.

Covers:

* `req~read-todo-txt~1`

Needs: impl, utest

### Task serializer
`dsn~task-serializer~1`

`Task.toLine` writes completion marker, priority, completion date, creation date and body, separated by single spaces.

Covers:

* `req~read-todo-txt~1`

Needs: impl, utest

### File as a list of lines
`dsn~todo-file-lines~1`

`TodoFile` holds the lines of the file and the line separator found in it.
Tasks are parsed from the lines on demand and are identified by their line index.
Only `TodoFile.replace` rewrites a line, and only if the given task differs from the task the line already contains.
Blank lines are kept and are not tasks.

Covers:

* `req~untouched-lines-unchanged~1`

Needs: impl, utest

### Metadata after the description
`dsn~canonical-order-on-edit~1`

`Task.edit` rebuilds the body in the order description, projects, contexts, hashtags, tags.
If no value differs from the current one, the task is returned unchanged.

Covers:

* `req~description-separate-from-metadata~1`
* `adr~metadata-after-description~1`

Needs: impl, utest

### Completion and dates
`dsn~completion-dates~1`

`Task.complete` sets the completion marker.
It writes the completion date only if the caller asks for it or if the task has a creation date; without a completion date in front of it, the creation date would be read as the completion date.
The priority is moved to a tag `pri:`, the convention of other todo.txt tools, and `Task.reopen` moves it back.

Covers:

* `req~no-dates-by-default~1`
* `req~priority-survives-completion~1`
* `adr~dates-from-history~1`

Needs: impl, utest

### Filter
`dsn~filter~1`

`Filter` holds the criteria; `Filter.matches` combines them with *and*.
Within priorities, projects, contexts and hashtags, one matching value suffices, and an empty selection does not restrict.
The text is searched in the whole line, ignoring case.
The default shows open tasks whose threshold date is not in the future.

Covers:

* `req~filter-criteria~1`

Needs: impl, utest

### Sort order
`dsn~sort-order~1`

`listOrder` compares by completion, by whether the task is overdue, by priority (none last), due date (none last), the time by which the task is due on that day (none last) and line index.
A task is overdue if it is open and its due date is before today (`Task.isOverdue`).

Covers:

* `req~list-order~1`

Needs: impl, utest

### Step in the list
`dsn~list-step~1`

Each task of the list has a button for a step up and one for a step down, which `TodoController.step` passes to `TodoFile.step` with the neighbor in the shown list.
The order of the list stays as `listOrder` defines it; a step changes what that order is computed from.
If the neighbor has the same priority, due date and due time, the line of the task is moved directly before the line of the neighbor, or behind it on the way down, and is written as it was.
If the neighbor has another priority, the task takes that priority and its line is moved so that it stays on its side of the neighbor: one step crosses the border, the next one passes the neighbor.
If the due date or due time differs as well, only the priority changes, as the due date then orders the two tasks.
A neighbor with the same priority and another due date or due time cannot be passed, and neither can a completed task.
An overdue task and one that is not overdue do not pass each other either, as the due date decides which of them stands first.
`Task.canStepOver` tells that, and the button is disabled.

Covers:

* `req~list-step~1`

Needs: impl, utest

### Drag in the list
`dsn~list-drag~1`

A task of the list is dragged with the mouse, or with the finger after a long press, as a card of the board is.
While it is dragged, its description follows the pointer, and a line marks where it would be put: before the nearest task if the pointer is in the upper half of that task, behind it otherwise.
Dropping it calls `TodoController.drop`, which applies `TodoFile.moveTo`.
The task takes the priority of the task it is put next to.
If both have the same due date and due time, its line is moved directly before or behind the line of that task; a line whose priority stays is written as it was.
If they differ in that, only the priority changes, as the due date then orders the two tasks.
There is no line, and no drop, at a task of the same priority and another due date or due time, at a completed task, or between an overdue task and one that is not overdue.

Covers:

* `req~list-step~1`

Needs: impl, utest

### Scrolling while a task is dragged
`dsn~drag-scroll~1`

While a task is held within 56 dp of the upper or lower edge of the list, or beyond that edge, the list scrolls towards it, the faster the nearer the pointer is to the edge.
The columns of the board scroll in the same way, and the board scrolls sideways while a card is held at its left or right edge.
The dragged card is pinned in its lazy list, as the list would otherwise drop it, and its gesture with it, when it leaves the window.

Covers:

* `req~list-step~1`

Needs: impl, utest

### Recurrence syntax
`dsn~recurrence-syntax~1`

`Recurrence.parse` reads the value of a `rec:` tag; `Recurrence.next` adds the interval to a date.
Business days skip Saturday and Sunday.

Covers:

* `req~recurrence-tag~1`

Needs: impl, utest

### Creation date
`dsn~creation-date~1`

`Task.created` sets today as the creation date of a new task if the caller asks for it and the task has none.

Covers:

* `req~no-dates-by-default~1`

Needs: impl, utest

### Findings in the tasks
`dsn~lint~1`

`lint` returns the findings in the open tasks of a `TodoFile`, each with the number of its line and a severity, and uses the functions with which the application reads the tags.
An error is a value that is not read: a date, recurrence, duration or time in a tag for one, a `p:` without a task of that `id:`, an `id:` of two tasks, and a note that does not exist.
A warning is what probably has another effect than intended: a tag given twice, a known key without a value, `dueby:` without `due:`, a threshold date after the due date, a line that a change of its task writes differently, and a context, project or hashtag that differs from another one in case only.
The tasks without context are named in one finding of the severity information, as they are in the inbox.

Covers:

* `req~lint~1`

Needs: impl, utest

### Findings in the files
`dsn~lint-files~1`

`lintFiles` reads the todo.txt file at a path and adds what the files next to it have: a note is looked up in the directory `notes`, a line of `ice_recur.txt` that `iceRecurLines` does not understand is an error, and so is each problem of the plan configuration in `koppter.yml`.
The findings name each file by the path that the path of the todo.txt file gives it.
`lintMain` prints the findings, writes their count to the error stream, and ends with 1 if an error is among the findings and with 2 if the arguments are not understood or the path is no file.

Covers:

* `req~lint~1`

Needs: impl, utest

### Formats of the findings
`dsn~lint-formats~1`

The option `--output-format` of the command line, read by `lintOptions`, selects how `written` writes the findings; the names are those of the check of JabRef.
`errorformat`, the default, writes `file:line: severity: message`, which editors and compilers share.
`github-actions` writes the workflow commands `::error`, `::warning` and `::notice` with file and line, which GitHub shows as annotations at the lines of a pull request.
`csv` writes a table with the columns file, line, severity and message.
A finding that belongs to no single line has no line.

Covers:

* `req~lint~1`

Needs: impl, utest

### Check started by jbang
`dsn~lint-jbang~1`

`.jbang/KoppterLint.kt` is a script for [jbang](https://www.jbang.dev/) that compiles the common sources of the module `core` and `lintFiles` and calls `lintMain`; `jbang-catalog.json` names it `lint`.
The check thereby needs no released file and is the one of the sources at hand.
The script lists the sources one by one, as jbang reads no pattern from a web address, and names the JVM variant of the library for dates, as the common one leads jbang to a file that does not exist.
A test compares the list with the sources and the versions with those of the build.

Covers:

* `req~lint~1`

Needs: impl, utest
