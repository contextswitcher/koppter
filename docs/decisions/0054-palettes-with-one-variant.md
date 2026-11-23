---
status: accepted
date: 2026-11-23
decision-makers: Oliver Kopp
---

# Palettes with One Variant Ignore the Setting of the Variant

`adr~palettes-with-one-variant~1`

Needs: dsn

## Context and Problem Statement

The task manager tuxedo offers the themes Muted Slate, Dawn, Nord, Matrix and Terminal, which users of todo.txt know from the terminal.
Nord is already a palette of koppter, and Terminal takes the colors of the terminal, which a graphical application does not have.
Muted Slate and Matrix exist only dark, Dawn only light, while the palettes of koppter have both variants and a setting chooses between them ([decision 0015](0015-colors-from-published-palettes.md)).
How do palettes with one variant fit the setting of the variant?

## Considered Options

* A palette with one variant is shown in it, and the setting of the variant is hidden while it is chosen
* Dawn and Muted Slate form one palette as its light and its dark variant
* The missing variant is derived from the existing one

## Decision Outcome

Chosen option: "A palette with one variant is shown in it, and the setting of the variant is hidden while it is chosen", because the application then shows the colors as tuxedo defines them.

The setting of the variant is kept, so that it applies again once a palette with both variants is chosen.
The role `dim` of tuxedo, made for completed tasks, is too faint for labels; its `status_fg` takes that place.

### Consequences

* Good, because the colors are those of tuxedo.
* Bad, because a dark palette stays dark while the system is light.

## Pros and Cons of the Options

### Dawn and Muted Slate form one palette as its light and its dark variant

* Good, because the setting of the variant keeps its meaning.
* Bad, because tuxedo does not pair them, and Matrix still has one variant only.

### The missing variant is derived from the existing one

* Good, because every palette follows the system.
* Bad, because the derived colors are not those of any published palette.
