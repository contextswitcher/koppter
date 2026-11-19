---
status: accepted
date: 2026-11-19
decision-makers: Oliver Kopp
---

# A Line Follows a Changed TODO of LaTeX by the Words They Share

`adr~latex-rematch~1`

Needs: dsn

## Context and Problem Statement

The tag `tex:` of a line names a TODO of a LaTeX document by a hash of its file and text ([decision 0045](0045-tag-for-a-todo-of-latex-with-a-hash-of-its-file-and-text.md)).
When the author rewords the TODO, the tag names nothing: the line keeps its old text, and the TODO is shown again without the priority that the user gave it.
How does the application tell that a new TODO is a changed old one?

## Considered Options

* The words that the text of the line and the text of the TODO share
* The edit distance of the two texts
* The history of the document in git
* No pairing: the user deletes the old line

## Decision Outcome

Chosen option: "The words that the text of the line and the text of the TODO share", because it needs nothing besides the line and the document, and a reworded TODO usually keeps most of its words.

A line and a TODO of the same project form a pair when at least half of all their words are shared.
The line is written with the text and the tag of the TODO; this is the one case in which the application changes a line that the user did not edit, because the document leads for the text of a TODO.

### Consequences

* Good, because the priority, the dates and the contexts that the user gave a TODO survive its rewording.
* Good, because it works for a document that is not in a repository and on every device that reads the document.
* Bad, because a TODO whose words were mostly replaced counts as a new one, and the old line stays open.
* Bad, because two lines with similar texts may be paired with the wrong TODOs; the most alike pair is taken first.
* Bad, because a text that the user changed in koppter is replaced by that of the document.

## Pros and Cons of the Options

### The edit distance of the two texts

* Good, because a changed letter, such as a corrected typo, counts less than a changed word.
* Bad, because reordered words count as many changes, though the TODO means the same.

### The history of the document in git

* Good, because the diff tells exactly which line became which.
* Bad, because it needs the document in a repository and the change in a commit before koppter reads it; a TODO is usually reworded while writing, before a commit.
* Bad, because the reading of the document would need JGit and a repository per project.

### No pairing: the user deletes the old line

* Good, because the application never changes a line by itself.
* Bad, because the user loses the priority and the dates and has to find the old line.
