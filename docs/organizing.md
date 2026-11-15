# Organizing many tasks

A todo.txt file with hundreds of tasks stays usable if every task answers a few questions in the same way.
This guide describes one way to do that with what koppter offers.
It is a suggestion, not a rule of the application: the file is yours.

## What each part of a task is for

| Part | Question it answers | Example |
| --- | --- | --- |
| Project `+` | To which area and to which outcome does the task belong? | `+research>FirstPaper` |
| Context `@` | In which kind of work, or where, is it done? | `@reading` |
| Priority | Does it matter more than the rest right now? | `(A)` |
| `due:` | Is there a date that someone else set? | `due:2026-12-01` |
| `deadline:` | Is that date later than the day to be done by? | `deadline:2026-12-10` |
| `t:` | From when on is it worth looking at? | `t:2027-01-10` |
| `dur:` | How long does it take? | `dur:2h` |
| `at:` | At which time does it take place, if it has one? | `at:12:00` |
| `tt:` | How long is the way there, if it is somewhere else? | `tt:30m` |
| `dueby:` | By which time of the due date does it have to be done? | `dueby:14:00` |
| Hashtag `#` | Which property does it share with tasks of other projects? | `#money` |

## Projects are the outline

This section and the next one describe one of two ways to place the areas of work and life; [Where the areas go](#where-the-areas-go) compares it with the other one.

A project names what a task belongs to, and levels written with `>` form an outline.
The outermost level holds the areas that stay: `+work`, `+research`, `+home`.
Below them stands what is reached one day: `+research>FirstPaper`, `+home>GarageSale`, `+home>TaxReturn2026`.

* Five to nine projects at the outermost level are enough, and two or three levels are usually enough as well.
* A filter for `+research` shows the tasks of everything below it, and the filter panel shows the projects as a tree.
* The last level is enough to type a project: `+First` in the line that adds a task offers `+research>FirstPaper`.
* A task that serves two outcomes carries both projects and stands in the groups of both.
* A project gets a barrier in the plan dialog if it has a deadline: `+research>FirstPaper = 2027-03-31`.
  A barrier or a limit of a project holds for the projects below it: `+research = max 10h/week`.
* A project whose tasks are all completed stays in the filter panel as long as these tasks are in the file.
  The application does not move completed tasks out of the file; other todo.txt tools archive them to `done.txt`.
* A task that belongs to nothing larger needs no project.

## Contexts are the kinds of work and the places

A context names how or where a task is done: `@reading`, `@writing`, `@phone`, `@errands`, `@train`.
A task carries both: `Outline the related work +research>FirstPaper @writing`.

* The context decides when a task is planned (its time map), how much of the week it may take (its limits), and where it shows in the overview.
  Reading is planned into the quiet hours and errands into the opening hours, whatever project they serve.
  That is why the kind of work is the context and the area is the project.
* Every task has a context.
  The [inbox](../README.md#inbox) lists the tasks that have none.
* A task that can be done in two ways carries two contexts: `@writing @train`.
  It shows under both, and the plan takes the time map of the first context that has one.
* `@waiting` in addition marks a task that waits for someone else.
* Five to nine contexts are enough.
  A context may have levels as well, such as `@errands>city`, which is planned in the times of `@errands` unless it has a time map of its own; most files do not need them.

## Where the areas go

An area of work or life, such as research, teaching or an open-source project, stays for years and holds many outcomes.
The two sections above make it the outermost project.
It can as well be a context, and which way fits depends on how the week is divided.
This is a decision for each file, not one of the application: both ways use the same notation.

### Areas as the outermost projects

`Outline the related work +research>FirstPaper @writing`

* Good, because the outline is in one place: area, outcome and its parts are the levels of one project.
* Good, because the context stays free for the kind of work and the place, and the plan gives reading the quiet hours whatever area it serves.
* Good, because a task that serves two areas carries both projects.
* Bad, because an area has no time map: the plan knows a limit and a barrier for a project, but its times go by the context.
* Bad, because the overview shows the kinds of work and not the areas.
* Bad, because the lines of the file get long with each level; the labels show the last level only.

### Areas as contexts

`Outline the related work +FirstPaper @research`

* Good, because each area gets its times and its share of the week: a time map for the days of teaching, `@opensource = max 5h/week`.
* Good, because the overview shows one tile per area with its overdue tasks, and the inbox lists what has no area yet.
* Good, because the lines stay short: one context, one project, and a level only where a project has parts.
* Bad, because the kind of work or the place needs a second context, `@research @train`, and the plan takes the time map of the first context that has one, so that their order matters.
* Bad, because a project does not tell its area: `+website` in two areas is one project for a filter.

### Choosing

* A week that is divided by area, with days or hours that belong to teaching or to one large project, speaks for areas as contexts.
* A week that is divided by the kind of work, with mornings for writing and afternoons for calls, speaks for areas as projects.
* One file keeps to one way.
  Write the choice down next to the file, for example in a `README.md`, so that other devices, other people and agents that add tasks follow it.

## Priorities are rare

Priority `(A)` is for what has to happen this week, `(B)` for what should, and most tasks have none.
If half of the tasks have a priority, it says nothing anymore: the plan and the list order by it.

## Two dates with two meanings

* `due:` is a deadline that the world sets: a submission, an appointment, a bill.
  A day on which you would like to do the task is not a due date; the plan finds that day.
* `deadline:` is that date for a task that should be done earlier: `due:2026-12-01 deadline:2026-12-10` keeps nine days as a buffer, in which the task is due and not overdue.
* `t:` is the threshold date: before it, the task is hidden from the list and is not planned.
  It is the answer to "not now": a task for next semester gets `t:` at its start.
* A task for some day gets a threshold date far ahead, such as the start of the next quarter.
  On that day it shows up again and is decided anew: do it, move it again, or delete it.
  A task whose threshold date lies behind its barrier is left out of the plan without a complaint.

## Durations for what is more than half an hour

A task without `dur:` counts as half an hour.
A task of more than a day of work is a project: split it into tasks of at most a few hours, and let them wait for each other with `id:` and `p:` if their order matters.

## Finding a task

* Ctrl+E searches all tasks, the completed ones included.
* The projects in the filter panel are a tree: a click on `+research` shows it with everything below it.
* A click on the project or context label of a task shows the tasks of that project or context.
* The list in groups by project shows the outline with its tasks.
* The [board](../README.md#board) with columns defined by filters keeps the searches that are needed every day, such as `Now = due:<=today -@waiting`.
* The [overview](../README.md#overview-of-the-contexts) shows one tile per context and tells which kind of work has overdue tasks.

## A rhythm

* **Whenever something comes up:** type it into the line that adds a task, or share it to koppter on the phone.
  It does not need a context yet.
* **Once a day:** empty the inbox, which takes a few keys per task, and work from the agenda.
* **Once a week:** look at the overview for the contexts with overdue tasks, then plan.
  The tasks that the plan lists as not fitting are the decisions of the week: give each a later threshold date, a later barrier, less duration, or delete it.

## Coming from SkedPal

| SkedPal | koppter |
| --- | --- |
| Level of the outline | Project `+`, with `>` for the levels |
| Project with an end | Project below the project of its area, with a barrier for its deadline |
| Time map | Time map, given to contexts: the kind of work of a task names the times it is planned in |
| Zone with a budget | Limit of a context or a project: `max 5h/week` |
| Minimum block length | Limit `block 2h` |
| Plan with a start date | Threshold date `t:` |
| Deadline | `due:` |
| Priority | Priority `(A)` to `(Z)` |
| Dependency | Tags `id:` and `p:` |
| On hold | `@waiting`, or a threshold date |
| Repeating task | `rec:`, or an entry of `ice_recur.txt` |
| Scheduling window | Barrier: the day behind which nothing is planned |
| Inbox | Inbox: the tasks without context |
| Today, next seven days | Agenda |
| Calendar | Week |
