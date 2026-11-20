# Android

Requirements and design that are specific to the Android application.
The screens are those of [ui.md](ui.md).

## Requirements

### Android application
`req~android-app~1`

The application runs on Android 8.0 (API level 26) and later with the same screens as on the desktop.

Covers:

* `feat~platforms~1`

Needs: dsn

### File of a document provider
`req~document-provider-file~1`

On Android, the user chooses the todo.txt file with the document picker of the system.
The file may belong to any document provider, including that of a cloud storage application.

Covers:

* `feat~sync-local-folder~1`

Needs: dsn

### File in OneDrive
`req~onedrive-file~1`

The user uses a todo.txt file that is stored in OneDrive: on Android by choosing it in the document picker, on the desktop by choosing it in the folder that the OneDrive client synchronizes.

Covers:

* `feat~sync-onedrive~1`

Needs: dsn

### Sharing to the application
`req~share-actions~1`

On Android, the user shares a text from another application, such as the link of an article, to koppter, which adds it as a task without asking anything.
The user defines share actions: each has a name and states what the task gets besides the shared text, such as a context, a due date relative to the day of sharing and a duration.
Each share action is a target of its own where the system offers that.

Covers:

* `feat~platforms~1`

Needs: dsn

### Folder of a document provider
`req~document-provider-folder~1`

On Android, the user chooses a folder with the document picker of the system instead of a single file.
The application then uses the file `todo.txt` of that folder together with the files next to it.

Covers:

* `feat~sync-local-folder~1`

Needs: dsn

### Back gesture
`req~back~1`

The back gesture of Android first closes what lies above the list and then returns to the list; from the list, it warns, as other applications do, and leaves the application when it follows at once.

Covers:

* `feat~platforms~1`

Needs: dsn

### Widget for the home screen
`req~android-widget~1`

On Android, the user places a widget on the home screen that shows what is planned from now on, by day, with the time of each block.
When nothing is planned from now on, it shows the open tasks as the list of the application does.

Covers:

* `feat~platforms~1`
* `feat~planning~1`

Needs: dsn

### Notification of a block
`req~android-block-notification~1`

On Android, the application tells the user with a notification that a block of the plan starts.
The notification names the task and the times of the block and offers to complete the task.
A watch that is connected to the phone shows the notification with that action.

Covers:

* `feat~platforms~1`
* `feat~planning~1`

Needs: dsn

### Agenda on a watch
`req~watch-agenda~1`

On a watch with Wear OS that is connected to the phone, the user sees what the widget shows: what is planned from now on, by day, or the open tasks when nothing is planned.
The user completes a task there, after confirming it.

Covers:

* `feat~platforms~1`
* `feat~planning~1`

Needs: dsn

## Design

### Shared UI as an Android library
`dsn~android-shell~1`

The module `:app` has an Android library target next to the desktop target; the screens are compiled for both from `commonMain`.
The module `:android` contains the activity, which creates the controller and shows `App`.

Covers:

* `req~android-app~1`

Needs: impl

### Locations on Android
`dsn~android-locations~1`

The settings file is `settings.properties` in the private files directory of the application.
Without a configured location, the todo.txt file is `todo.txt` in the same directory.
A configured location that starts with `content:` is opened as a document, or as a folder if it is the root of a tree of documents; any other is opened as a file.

Covers:

* `req~android-app~1`
* `req~persistent-settings~1`

Needs: impl

### Document storage
`dsn~document-storage~1`

`DocumentStorage` reads and writes the document behind a `content:` URI through the `PlatformFile` of FileKit.
The version is the modification time together with the size, as reported by the document provider.

Covers:

* `req~document-provider-file~1`
* `req~external-changes~1`
* `req~onedrive-file~1`
* `adr~onedrive-through-platform~1`

Needs: impl

### Folder storage
`dsn~folder-storage~1`

`FolderStorage` reads and writes the file `todo.txt` of a folder behind a `content:` URI and, as `CompanionFiles`, the files next to it by their path below the folder.
It finds a document by its name at every access: with some document providers, the address of a document does not contain its name, so that the address of a document that was created since is not known in advance.
It creates the directory of a file before it writes the file.
A folder without the file `todo.txt` reads as an empty list, a folder that is not accessible as a failure.

Covers:

* `req~document-provider-folder~1`
* `req~external-changes~1`

Needs: impl

### Choose the folder
`dsn~choose-folder~1`

Next to the button for a file, the settings dialog has a button that opens the dialog of the platform for a folder, through FileKit.
On Android, the application bookmarks the chosen folder, which takes the persistable read and write permission for it, and the location is the URI of the tree of the folder.
On the desktop, the location is the path of the file `todo.txt` in the chosen folder, which need not exist yet; a folder of a git repository is thus used as `dsn~git-detection~1` says.

Covers:

* `req~document-provider-folder~1`
* `req~choose-file~1`

Needs: impl

### Choose the file
`dsn~choose-file~1`

The settings dialog has a button that opens the file dialog of the platform and puts the chosen location into the field.
Both platforms open the dialog through FileKit ([decision 0036](../decisions/0036-file-dialogs-and-document-access-through-filekit.md)).
On Android, this is the document picker; the application bookmarks the chosen document, which takes the persistable read and write permission for it.
On the desktop, this is the file dialog of the operating system, which is shown from a thread of its own, so that the user interface keeps running as usual while the dialog is open.
The help of the field tells that the files next to the todo.txt file belong to it.
Below the button, the dialog shows what the platform passes as `chooseFileNote`: on Android, that the document picker gives access to a chosen file alone and that a chosen folder has the files next to the todo.txt file.

Covers:

* `req~document-provider-file~1`
* `req~choose-file~1`
* `req~onedrive-file~1`
* `adr~onedrive-through-platform~1`

Needs: impl

### Back gesture
`dsn~back~1`

`App` takes a `backHandler` from the platform: back closes the navigation drawer with the filter panel if it is open, and otherwise shows the list.
From the list, back shows the short message *Back again leaves koppter.*; another back while the message is shown calls the `leave` of the platform.
The Android application passes the `BackHandler` of its activity and moves its task to the background to leave, as back does it for the first activity of an application since Android 12; the desktop application passes none.
A dialog closes on back by itself, with the question about changes that are not saved.

Covers:

* `req~back~1`

Needs: impl, utest

### Share actions
`dsn~share-actions~1`

A `ShareAction` has a title and a template, a line in todo.txt syntax in which `{}` stands for the shared text; without `{}`, the shared text stands in front.
The values of `due:` and `t:` in the template may be relative to the day of sharing, as in the filter expressions: `due:2w`.
The shared text becomes the body of the task as it is, with line breaks as blanks.
The actions are kept in `koppter.yml` under the key `share-actions` and edited in the settings dialog, one per line as `title = template`.
`TodoController.share` applies the action with the given title, or the first one, and adds the task; without actions, the shared text is the task.

Covers:

* `req~share-actions~1`

Needs: impl, utest

### Share target on Android
`dsn~android-share-target~1`

`ShareActivity` receives text, or an image as `dsn~note-images~1` says, that is shared to the application, adds the task through a controller of its own, tells the result in a toast and finishes without showing a screen.
`MainActivity` publishes one shortcut per share action; from Android 10 on, the share sheet offers these shortcuts as targets, and the id of the chosen shortcut names the action.
The entry of the application itself in the share sheet applies the first action.

Covers:

* `req~share-actions~1`

Needs: impl

### Links on Android
`dsn~android-links~1`

The manifest declares that `MainActivity` views links of the scheme `koppter`, and starts it as a single task, so that a link reaches the open application.
The activity passes the link of the intent that started it, and of each later intent, to `TodoController.openLink`; after a change of the configuration it does not pass the first one again.

Covers:

* `req~app-links~1`

Needs: impl

### Widget on Android
`dsn~android-widget~1`

`AgendaWidget` shows today's date as its title and a list that `AgendaWidgetService` fills through a controller of its own.
`CalendarTxt.ahead` gives the blocks of `calendar.txt` that have not ended, in the order of time; the list shows them under a heading per day, each with the text of its task and its times.
Without such a block, the list shows the open tasks of the filtered list.
It is placed at two cells by one and grows in both directions.
The widget is refreshed every half hour and when `MainActivity` stops; a click on it opens the application.
Its colors are those of the system theme from Android 12 on.

Covers:

* `req~android-widget~1`

Needs: impl, utest

### Notification of a block on Android
`dsn~android-block-notification~1`

`CalendarTxt.next` gives the first block that starts after now, and `scheduleBlockAlarm` sets an alarm for its start: when `MainActivity` stops, when the widget is refreshed, after the device started and after the application was updated.
The alarm is inexact, so that it needs no permission; the system may deliver it some minutes late.
`PlanReceiver` receives it and shows the block that `CalendarTxt.current` gives, if its task is open: the description as the title, the times as the text and the action *Done*, which completes the task through a controller of its own.
A block of travel time and a block whose task was completed, deleted or edited since get no notification.
The notification leaves when its block ends, and each alarm sets the next one.
From Android 13 on, `MainActivity` asks for the permission to notify when the file has a plan.
Wear OS shows the notifications of the phone with their actions, so the watch needs no code for it.

Covers:

* `req~android-block-notification~1`

Needs: impl, utest

### Agenda for the widget and the watch
`dsn~watch-agenda~1`

`TodoController.agenda` gives the lines that the widget and the watch show: the blocks that `CalendarTxt.ahead` gives under a line per day, or the open tasks of the filtered list.
A line of an open task carries the name that the plan has for the task, which tells tasks with the same text apart.
`TodoController.entryNamed` finds the open task of a name, so that a completion names its task by it: the line of the task may change in between, and a task that is completed already is not found and thus not reopened.

Covers:

* `req~watch-agenda~1`

Needs: impl, utest

### Watch through the Data Layer
`dsn~watch-data-layer~1`

The module `:wear` is an application for Wear OS with the application id of the phone application and its signature, without which the Data Layer does not connect the two.
The phone application stores the first 50 lines of the agenda as the item `/koppter/agenda`: a list `lines` of maps with `day`, or with `text` and optionally `times` and `name`.
It does so when `MainActivity` stops, when the widget is refreshed, when the alarm of a block arrives and after each message of the watch.
The watch application shows the item while it is open, days as headings and tasks as buttons, and sends `/koppter/refresh` when it comes to the front, as blocks may have ended since.
A tap on a task asks whether to complete it; the answer sends `/koppter/complete` with the name of the task, and `WatchListenerService` of the phone application completes it through a controller of its own and stores the agenda again.
The item carries the time at which it was stored, so that the watch is told of it also when the lines stayed the same.
A line without a name, whose task was completed, deleted or edited since, is shown and cannot be tapped.

Covers:

* `req~watch-agenda~1`
* `adr~watch-through-data-layer~1`

Needs: impl
