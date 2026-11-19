---
status: accepted
date: 2026-11-19
decision-makers: Oliver Kopp
---

# Shared Lists as Repositories Shown with a Context

`adr~shared-lists~1`

Needs: dsn

## Context and Problem Statement

Some tasks are shared with other people, such as those of a household or of a shared flat.
They do not belong into the main todo.txt file, whose repository is private, but the user wants to see them together with the own tasks.
Their notes have to be shared as well.
How does the application keep and show such lists?

## Considered Options

* One repository per shared list, its tasks shown in the main list with a context named after the repository
* One repository per shared list, each shown on its own, with a switch between the lists
* The shared tasks in the main file, copied by a tag into the repository of the list

## Decision Outcome

Chosen option: "One repository per shared list, its tasks shown in the main list with a context named after the repository", because filter, board, overview and planning work on the shared tasks without a change, and a new task goes to a list by the context that is typed.

The context is not written into the file of the list: the other people see their tasks as they wrote them, and a list keeps its tasks when its repository is renamed in its clone.
The main file has no context of its own; a task without the context of a list is written there.

### Consequences

* Good, because each list is a todo.txt file with notes of its own in a repository of its own, which is shared as any repository is, and other todo.txt tools work on it.
* Good, because the storage combines the files, and the screens see one file.
* Bad, because a task of the main file that has the context of a list moves into that list at the next change.
* Bad, because the name of the context comes from the directory of the clone, which can differ between devices.

## Pros and Cons of the Options

### One repository per shared list, each shown on its own, with a switch between the lists

* Good, because the lists cannot mix.
* Bad, because the plan, the agenda and the overview see only the list that is shown.
* Bad, because every screen needs to know the list it shows.

### The shared tasks in the main file, copied by a tag into the repository of the list

* Good, because one file holds all tasks of the user.
* Bad, because every task is kept twice, and changes of the other people have to be copied back, which is a merge of its own.
