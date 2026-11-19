---
status: accepted
date: 2026-11-19
decision-makers: Oliver Kopp
---

# A Tag Names a TODO of a LaTeX Document by a Hash of Its File and Text

`adr~latex-tag~1`

Needs: dsn

## Context and Problem Statement

The TODOs of a LaTeX document are shown as tasks without being written to the todo.txt file, as the items of Outlook are.
A TODO becomes a line of the file when the user gives it a priority or completes it.
The line has to name the TODO, so that the TODO is not shown a second time.
A TODO, unlike an item of Outlook, has no id: it is a command in a text file that the author keeps editing.
How does the line name the TODO?

## Considered Options

* The tag `tex:` with a hash of the file and the text of the TODO
* The tag `tex:` with the file and the number of the line
* A label written into the LaTeX source, such as `\todo[id=17]{...}`

## Decision Outcome

Chosen option: "The tag `tex:` with a hash of the file and the text of the TODO", because it stays the same while the text around the TODO changes, and the document is not changed.

The hash is that of the file name, relative to the main file, and the text, with runs of blanks as one blank, in base 36: at most seven characters.

### Consequences

* Good, because adding or removing lines above a TODO keeps its line in the todo.txt file connected to it.
* Good, because the same document gives the same tags on every computer, as long as the main file has the same name.
* Bad, because a changed text of a TODO is a new TODO: the line of the old one stays and the new one is shown again.
* Bad, because two TODOs with the same text in one file share the tag; the second one is not shown.

## Pros and Cons of the Options

### The tag `tex:` with the file and the number of the line

* Good, because the user sees where the TODO is.
* Bad, because every line added above the TODO connects the line of the todo.txt file to another TODO or to none.

### A label written into the LaTeX source, such as `\todo[id=17]{...}`

* Good, because the id survives a change of the text.
* Bad, because the application would write into the document of the user, which is often in a repository shared with coauthors.
* Bad, because not every package accepts an unknown option; `\TODO` of `fixmetodonotes` takes none.
