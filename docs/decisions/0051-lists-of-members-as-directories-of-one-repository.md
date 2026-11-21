---
status: accepted
date: 2026-11-21
decision-makers: Oliver Kopp
---

# Lists of Members as Directories of One Repository

`adr~member-lists~1`

Needs: dsn

## Context and Problem Statement

A family keeps the tasks of each of its members, and a device that all of them use, such as a tablet in the hallway, shows the tasks of all.
The tasks of a member have to be told from those of the others, and a new device should need one step to get them all.
Where are the tasks of the members kept?

## Considered Options

* One repository with a directory per member, each with a todo.txt file of its own
* One repository per member, each named as a shared list in the settings
* One todo.txt file, the tasks of a member marked with a context

## Decision Outcome

Chosen option: "One repository with a directory per member, each with a todo.txt file of its own", because a device clones one repository and has the lists of all members without a further setting, and a member gets a list as soon as the directory exists.

A directory next to a todo.txt file that has a todo.txt file of its own is a list as a shared list is ([decision 0043](0043-shared-lists-as-repositories-shown-with-a-context.md)): its tasks show with the context named after the directory.
This holds next to the main file and next to the file of a shared list, so that a member reads the repository of the family as a shared list beside a private main file.
The other two options stay possible: a further repository is a shared list, and a context needs no file.

### Consequences

* Good, because the files next to the todo.txt file, such as the notes and `calendar.txt`, exist once per member.
* Good, because other todo.txt tools work on the file of a member.
* Bad, because everybody who reads the repository reads the lists of all members; a list that is private needs a repository of its own.
* Bad, because a directory that happens to hold a todo.txt file becomes a list without the user asking for it.
* Bad, because several lists in one repository need a merge that covers all of them, where a repository per list is merged on its own.

## Pros and Cons of the Options

### One repository per member, each named as a shared list in the settings

* Good, because it needs no change, and each member decides who reads the list.
* Bad, because every device needs an entry per member, and a new member means a change of the settings of every device.

### One todo.txt file, the tasks of a member marked with a context

* Good, because it needs neither a directory nor a setting.
* Bad, because the context is written into every line, and a task without it belongs to nobody.
* Bad, because the notes and the calendars of all members share one directory.
