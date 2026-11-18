---
status: accepted
date: 2026-11-18
decision-makers: Oliver Kopp
---

# Colors of Time Maps as Words in Their Rules, Taken When a Task Would Be Late

`adr~time-map-colors~1`

Needs: dsn

## Context and Problem Statement

SkedPal paints the times of a time map green, yellow, red or black: green times are preferred, yellow and red ones are taken only when a task would otherwise miss its deadline, and black times belong to one map alone.
The user wants these colors in koppter and paints the maps on a week.
The time maps are lines of `koppter.yml` that the user also types, such as `General = Mon-Fri 08:00-12:00 13:00-17:00`.
How does a time map hold its colors, and when does the planner take a time of a worse color?

## Considered Options

* A color word in a rule, green without one; worse colors for a task that ends late in the plan with the better ones
* A color mark on each time, such as `07:00-08:00!` for yellow
* A separate map for each color, such as `General-yellow`
* The cells of the week as a string of letters, one per quarter of an hour

## Decision Outcome

Chosen option: "A color word in a rule, green without one; worse colors for a task that ends late in the plan with the better ones", because every definition written so far keeps its meaning, the editor writes lines that read like those the user types, and the planner already plans again for the tasks that end after their due date.

The planner first moves a task that ends after its due date to the front.
If it still ends late, or a task does not fit before its barrier, the task gets the yellow times as well, the plan is made again, and then the red times.
Black times are taken out of the times of all other maps, except a map that is restricted to the black map.

### Consequences

* Good, because definitions without colors are read as before, and the text stays readable and editable by hand.
* Good, because a painted map is written as few rules: the days with the same times share one.
* Bad, because a task that takes yellow times takes the earliest of them, not only as many as it needs to be in time.
* Bad, because the editor works in quarters of an hour; a time that starts at another minute is rounded when the map is painted.

## Pros and Cons of the Options

### A color mark on each time

* Good, because one rule can mix colors.
* Bad, because a mark like `!` has to be learned, and a line with many marks is hard to read.

### A separate map for each color

* Good, because the notation does not change.
* Bad, because a context names one map, and the planner would have to know which maps belong together.

### The cells of the week as a string of letters

* Good, because the editor would write it without any grouping.
* Bad, because a line of 672 letters cannot be read or edited by hand.
