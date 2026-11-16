---
status: accepted
date: 2026-11-16
decision-makers: Oliver Kopp
---

# Links of Projects and Contexts Are Kept in the Configuration File

`adr~label-links~1`

Needs: dsn

## Context and Problem Statement

A task refers to a task of ContextSwitcher with the tag `cs:`.
The material of a whole project, such as the section of a notebook or a category of ContextSwitcher, belongs to no single task.
Where does the link of a project or a context live?

## Considered Options

* An entry per label in the configuration file next to the todo.txt file
* A tag at every task of the project
* A line in the todo.txt file that stands for the project
* The settings of the device

## Decision Outcome

Chosen option: "An entry per label in the configuration file next to the todo.txt file", because the link is stated once, is synchronized with the tasks, and the todo.txt file stays free of lines that are no tasks.

The key `links` of `koppter.yml` maps a label with its sign, such as `+Fair` or `@phone`, to a link.
The link is of any kind that the device opens; the application does not interpret it.

### Consequences

* Good, because a project below another one has the link of that one without an entry of its own.
* Good, because the link works for a project that has no open task at the moment.
* Bad, because another todo.txt program does not show the link.
* Bad, because a file chosen through the document picker of Android has no configuration file and therefore no links.
* Bad, because a link to a program that is installed on one device only fails on the others.

## Pros and Cons of the Options

### A tag at every task of the project

* Good, because other programs keep the tag and show it.
* Bad, because the link is repeated in every line and has to be changed in all of them.
* Bad, because a link with a blank or of some hundred characters, as a notebook gives it, does not suit a line.

### A line in the todo.txt file that stands for the project

* Good, because one file holds everything.
* Bad, because the line is a task for every program, the application included: it is counted, planned and completed.

### The settings of the device

* Good, because each device has the links that work on it.
* Bad, because the user enters the links on every device.
