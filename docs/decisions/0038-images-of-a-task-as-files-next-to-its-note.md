---
status: accepted
date: 2026-11-17
decision-makers: Oliver Kopp
---

# Images of a Task Are Files Next to Its Note

`adr~images-next-to-notes~1`

Needs: dsn

## Context and Problem Statement

A task sometimes needs a picture: a photographed receipt, a whiteboard, a screenshot.
The todo.txt file holds one line of text per task, and the note of a task is a Markdown file in the directory `notes`, which the task names with the tag `note:`.
Where does an image of a task live?

## Considered Options

* A file next to the note, which the note shows as a Markdown image
* A tag at the task that names the file of the image
* The image inside the note, encoded as text
* A link to a service that keeps images

## Decision Outcome

Chosen option: "A file next to the note, which the note shows as a Markdown image", because the note is already the place for what does not fit into the line, Markdown has a notation for images that other programs show, and the file is synchronized with the tasks.

The image is written to `notes/<name of the note>-<number>.jpg`, and the note gets the line `![](<file name>)`.
The application scales an image down when it is attached, so that a photo of several megabytes becomes a file of some hundred kilobytes.
It shows the images that a note names by a plain file name; an address or a path is left to other programs.

### Consequences

* Good, because a program that shows the Markdown file shows the image.
* Good, because a task has any number of images, in the order and with the text around them that the note gives.
* Good, because the todo.txt line stays as short as it is: it has the tag `note:` and nothing else.
* Bad, because a task with an image has a note, also where the note says nothing else.
* Bad, because a repository keeps every image for good, the removed ones included.
* Bad, because an image that is scaled down loses detail, and one with transparency loses that.
* Bad, because a todo.txt file without the files next to it has no images.

## Pros and Cons of the Options

### A tag at the task that names the file of the image

* Good, because a task has an image without a note.
* Bad, because every image makes the line longer, and a tag has one value.
* Bad, because no other program knows the tag.

### The image inside the note, encoded as text

* Good, because the note is one file.
* Bad, because the note is no longer read or edited as text: one photo is some hundred thousand characters.
* Bad, because a merge of two changed notes then compares these characters.

### A link to a service that keeps images

* Good, because the repository stays small.
* Bad, because the image depends on an account and a connection, and it is not kept with the tasks.
