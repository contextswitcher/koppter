---
status: accepted
date: 2026-11-26
decision-makers: Oliver Kopp
---

# Link to a Task by its Page of OneNote and its Text

`adr~task-link-by-page-and-text~1`

Needs: dsn

## Context and Problem Statement

A page of OneNote that a task refers to with its tag `onenote:` ([decision 0055](0055-tag-for-a-page-of-onenote-with-its-link-as-copied.md)) shows a link back to the task, which AnchorHub writes onto the page.
The links that koppter has so far name a filter ([decision 0033](0033-links-to-the-application-with-a-query.md)), not one task.
A line of todo.txt has no identity of its own: its position changes when another line is added or removed, and its text when the user edits it.
How does a link name one task?

## Considered Options

* The key of the page and the description of the task in the link: `koppter://task?onenote=<page-id>&text=<description>`
* A tag `id:` that koppter adds to each task with a page, named by the link
* A hash of the line in the link

## Decision Outcome

Chosen option: "The key of the page and the description of the task in the link", because the file needs no new tag, and the page already tells the tasks apart that are meant: those few that refer to it.
The key is the GUID of `page-id` in the link of OneNote, which stays when the page is renamed or moved to another section; a link without it is its own key.
Of the tasks that refer to the page, the link opens the one with its description; if none has it, because the task was edited since the last synchronization, it opens the only task of the page, or lists the tasks of the page when there are several.

### Consequences

* Good, because todo.txt stays as the user wrote it, and other todo.txt tools see no tag that they do not know.
* Good, because the link can be read: it names the task in words.
* Good, because the link is right again after the next synchronization, which writes the edited description into it.
* Bad, because a link to a task that was edited, on a page with several tasks, leads to a list of them instead of the task, until the next synchronization.
* Bad, because two tasks with the same description on the same page cannot be told apart; the link lists both.

## Pros and Cons of the Options

### A tag `id:` that koppter adds to each task with a page, named by the link

* Good, because the link names exactly one task, also after the task was edited.
* Bad, because koppter would write a tag into lines that the user did not change, against the rule that the file is the data store and an unedited line is written back unchanged.
* Bad, because `id:` already means a dependency, as topydo writes it: a task with `p:` of the same value waits for it; a tag of another name would be one more that other tools do not know.

### A hash of the line in the link

* Good, because the file needs no tag and the hash names one line.
* Bad, because any change of the line, also completing it or one more context, breaks the link until the next synchronization, and then there is nothing to fall back to.
* Bad, because the link cannot be read.
