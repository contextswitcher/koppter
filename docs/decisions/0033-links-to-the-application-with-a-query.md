---
status: accepted
date: 2026-11-15
decision-makers: Oliver Kopp
---

# Links to the Application Name a Filter in a Query

`adr~links-with-query~1`

Needs: dsn

## Context and Problem Statement

A note in another program, such as a page of OneNote, is to lead to the tasks of a project, a context or a hashtag.
The operating systems open a link with the program that registered its scheme.
What does a link to the application look like?

## Considered Options

* `koppter://filter?project=papers>survey`, with the keys `project`, `context` and `hashtag` in a query
* `koppter://project/papers>survey`, with the kind and the name as the path
* `koppter://filter/+papers>survey @phone`, with a filter expression as on the board

## Decision Outcome

Chosen option: "`koppter://filter?project=papers>survey`, with the keys `project`, `context` and `hashtag` in a query", because one link can name a project together with a context, and further keys can be added without breaking the links that exist.

### Consequences

* Good, because the link is typed by hand: a key and a name.
* Good, because `koppter://filter?project=Fair&context=phone` shows what a link with a path could not name.
* Good, because the word after the scheme leaves room for links that do something else later.
* Bad, because the link is longer than one with a path.
* Bad, because a link names what the filter panel has, not a filter expression with dates and priorities.

## Pros and Cons of the Options

### `koppter://project/papers>survey`, with the kind and the name as the path

* Good, because it is the shortest form.
* Bad, because it names one project, context or hashtag only.
* Bad, because a program may change the path, for example the case of its first part, which is the host of the link.

### `koppter://filter/+papers>survey @phone`, with a filter expression as on the board

* Good, because it can name everything a board column can.
* Bad, because the signs `+`, `#` and the blank have a meaning in a link, so that the expression has to be encoded by hand.
* Bad, because the list has no place that shows such an expression: the user would not see what is filtered.
