---
name: ux-review
description: Review a change of the user interface of koppter before it is committed - the questions about its flow, the settled rules, the screens rendered at the size of a phone and of a laptop, and a reviewer that sees only the images. Use it for every change that a user can see - a new or changed screen, dialog, field, button, list or message in app, android or wear - before the first line of the change is written and again before the commit.
---

# Review of a change of the user interface

Between 2026-11-12 and 2026-11-20, about thirty commits reworked an element that an earlier commit had just added: a search field out of view, a dialog without a heading, a Cancel that did not cancel.
A reviewer that saw only the screenshots from before twelve of these commits named five of the twelve problems and part of four more.
The other three, and all the problems of a flow, show in no image: the questions below are for them.

## Before the change: the flow

Answer these for the element that is added or changed, and write the answers into the `req` or `dsn` item.

* **Frequency:** what does the user do here most often?
  That stands first and in view; what is changed least often stands last or in a tab of its own.
* **Ways out:** what do Save, Cancel, Esc, a click beside the dialog and back on Android each write, discard and show next?
  Cancel leaves no trace.
  An action that takes effect at once closes the dialog, so that no Save stands beside something already done.
* **Failure:** where does the message appear?
  In the color of an error, next to the control that was used, in view without scrolling, and with the cause.
* **Afterwards:** where is the focus and the cursor, so that typing goes on where the user expects it?
* **Restart:** which choice is kept in the settings: view, filter, width?
* **Phone:** no keyboard, no hover, no right click.
  Back closes the innermost thing first: drawer, dialog, then the warning before the application is left.
  Nothing that reads the network or a file runs on the main thread.

## Settled

These were decided; neither the change nor the reviewer reopens them without a word of the user.

* Two sizes of text, from `compactTypography`; a title is told apart by its weight.
  No other size is passed to a text.
* An empty field does not look filled.
  Its title stands above it or its label says `(optional)`; an example or a default is a gray hint, not a label.
* The window is 1280 x 720 on a laptop and 412 x 760 on a phone, not the height of the screenshots of the README.
  The title and the buttons of a dialog stay in view while its body scrolls.
  What is used most on a screen is in view without scrolling.
* A pane that holds text has no fixed width: it takes the room that is left, or a handle changes its width and the settings keep it.
* A list shows all its entries or says that it is cut.
* The views are one row of switches of which one is always selected, Today and List first.
  A switch of that row selects a view and nothing else: an action such as Plan is a button of the top bar, and an option of the list belongs to the filter panel, where the switches Open and Done stand last.
* Words instead of an icon where no icon tells what happens.
* A default of Material is not a decision: check the height of a field, the first day of a week (Monday, decision 0049) and whatever follows the locale of the system.

## Render

`./gradlew :app:desktopTest --tests '*ScreenshotTest*'` writes `app/build/screenshots/review/phone-*.png` and `laptop-*.png`.
A screen or a state that the change touches and that is not among them gets a `screen(...)` line in `reviewScreens` of `ScreenshotTest`, which stays.
Look at the images of the changed screens in both sizes, and check first that each one shows the screen it is named after: a dialog that did not open leaves an image of the list.

## Independent reviewer

The session that built the change sees what it meant to build.
Start one fresh subagent, not a fork, and give it only this:

```text
Review screenshots of koppter, a task manager for todo.txt files, used all day by one person on a laptop (1280 x 720) and on a phone (412 x 760, touch only).
Read .claude/skills/ux-review/SKILL.md, section "Settled", and the images <paths of the changed screens in both sizes>.
Read no source code and no history.
The user wants to: <one sentence on the task of the user, not on the implementation>.
For each image, list at most four problems that you would want fixed before a release, most severe first: one sentence on the element and what is wrong for the user, one on the fix.
Name what falls out of view. Report nothing that "Settled" decides, no praise and no general advice.
```

Not the diff, not the reasoning, not what was changed.

## What to do with the findings

* A finding about the change at hand is fixed before the commit, and the images are rendered again.
* A finding about another part of the screen is not fixed in this commit: the report to the user lists it in one line.
* A finding against a rule of "Settled" is dropped.
* When the user corrects the user interface afterwards, the rule behind the correction goes into "Settled" in the commit that fixes it.

## Limits

The images come from the desktop: Android and Wear OS are not run, and the report to the user says so.
An image shows no flow; a Compose test in `AppTest` covers each answer of the first section.
