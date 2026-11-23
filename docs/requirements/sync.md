# Synchronization

Requirements and design for the synchronization of the todo.txt file.

## Requirements

### Git repository as the synchronization channel
`req~git-sync~1`

A todo.txt file that lies in the working tree of a git repository is synchronized with the remote repository without further configuration: the application commits its changes, fetches and merges the changes of other devices, and pushes.
History is never rewritten: remote changes are merged, not rebased.

Covers:

* `feat~sync-git~1`

Needs: dsn

### One commit per change
`req~commit-per-change~1`

Adding, completing, reopening, editing and deleting a task each create one commit whose message names the change and the task.

Covers:

* `feat~sync-git~1`

Needs: dsn

### No conflicts between devices
`req~no-merge-conflicts~1`

Changes made to different tasks on two devices before either of them synchronized are both kept, without the user having to resolve a conflict.
If the same task was changed differently on both devices, both versions are kept.
The schedule in `ice_recur.txt` is kept in the same way, and of two plans made on two devices the one made last is kept.

Covers:

* `feat~sync-git~1`

Needs: dsn

### Dates from the history
`req~dates-from-history~1`

For a file synchronized through git, the application shows when a task was added and when it was completed, although the file contains neither date.

Covers:

* `feat~sync-git~1`

Needs: dsn

### Synchronization status
`req~sync-status~1`

The application shows whether the file is synchronized, being synchronized, or could not be synchronized and why.
The user can start a synchronization at any time.

Covers:

* `feat~sync-git~1`

Needs: dsn

### SSH key of the device
`req~ssh-key~1`

On a device without an SSH configuration of its own, the application has its own SSH key pair and shows the public key, so that the user can register it with the git server.
On the desktop, the application uses the SSH configuration of the user.

Covers:

* `feat~sync-git~1`

Needs: dsn

### Clone in the application
`req~clone-in-app~1`

The user enters the address of a repository instead of the location of a file, and the application clones it and uses its todo.txt file.

Covers:

* `feat~sync-git~1`

Needs: dsn

### Shared lists
`req~shared-lists~1`

The settings name further lists, each by the address of its repository or by its todo.txt file, such as those of repositories shared with other people.
Their tasks are shown together with those of the main file, each with a context named after its repository, or after the directory of its file.
A new task with that context, or with a level below it, is written into that list, a task without such a context into the main file; changing the context moves the task, and its note and the images of the note move with it.
The entries of the `ice_recur.txt` of a list are scheduled as those of the main file and add their tasks to that list.
A line of a file is written back unchanged unless the user edited it; the context of the list is not written into the file of the list.
A list that cannot be read, such as one whose clone is gone, does not keep the others from being shown: the application reports why, and does not write a change that would go into that list.
Where the application clones a repository, it clones the repository of a shared list as well, and clones it again when the clone is gone.

Covers:

* `feat~shared-lists~1`

Needs: dsn

### Lists in the directories of a list
`req~member-lists~1`

A directory next to the todo.txt file of the main file or of a shared list that has a todo.txt file of its own is a list as well: its tasks show with the context named after the directory, and it needs no entry in the settings.
The members of a family or of a team thus keep a list each in one repository, which a device clones once.
Changes that two devices make to the lists of one repository do not conflict.

Covers:

* `feat~shared-lists~1`

Needs: dsn

## Design

### Detection of the repository
`dsn~git-detection~1`

`storageForFile` looks for a git repository whose working tree contains the todo.txt file.
If there is one, the storage is a `GitStorage`, otherwise a `FileStorage`.

Covers:

* `req~git-sync~1`

Needs: impl, utest

### Merge of the files next to todo.txt
`dsn~merge-of-companions~1`

Of the files that both sides of a merge changed, the todo.txt file, `ice_recur.txt`, `timelog.txt` and `done.txt` are the result of `mergeLines`, as they consist of lines that stand for themselves.
`calendar.txt` and `calendar.ics` are taken whole from the side whose `calendar.txt` was committed last, as the lines of two plans do not make a plan; with the same time, the side of the device is taken.
Notes and `koppter.yml` are left to the merge of git.

Covers:

* `req~no-merge-conflicts~1`

Needs: impl, utest

### Git storage
`dsn~git-storage~1`

`GitStorage` reads and writes the file in the working tree and uses JGit for all git operations.
A lock guards the working tree and the index against concurrent use by the user interface and by the synchronization.
All synchronizations run one after the other on a single background thread.
Commits are made in the name of the committer of the settings, else of the configured git user, else of the application (`dsn~committer~1`).

Covers:

* `req~git-sync~1`
* `adr~jgit~1`

Needs: impl

### Committer
`dsn~committer~1`

The settings dialog has, under *Git repository*, the fields *Name of the commits* and *Email address of the commits*, which are settings of the device.
An empty field stands for the value of the git configuration, `user.name` or `user.email`, and without one for `koppter` or `koppter@localhost`, on Android for the name of the device and `koppter@android<version>`, such as `koppter@android14`; the empty field shows that default in gray, with the title above the field so that it does not hide the default.
`GitStorage` makes each commit in that name as author and committer; a change of either field opens the storage again, so that the next commit uses it.

Covers:

* `req~git-sync~1`

Needs: impl, utest

### Email address of the reflog
`dsn~reflog-email~1`

JGit records each commit in the reflog in the name of the configured git user, whatever the committer of the commit is.
Without a configured email address it makes one up from the name of the host, for which it asks the network; Android forbids that on the main thread, where a change is committed, so the commit would fail.
`GitStorage` therefore passes `defaultCommitterEmail` to JGit as the email address of the environment, unless the environment has one.

Covers:

* `req~commit-per-change~1`

Needs: impl, utest

### Commit per change
`dsn~commit-per-change~1`

`TodoController` passes a description of each change, such as `Complete task: Call Mom`, to the storage.
`GitStorage.write` writes the file and commits it at once, with the description as the message.
A write that does not change the file creates no commit.

Covers:

* `req~commit-per-change~1`
* `adr~dates-from-history~1`

Needs: impl, utest

### Synchronization
`dsn~git-sync~1`

A synchronization commits pending changes of the file made by other programs, fetches, merges the remote branch of the current branch, and pushes.
Changes made during the fetch are committed before the merge, which refuses a working tree with a change.
A branch that is behind is fast-forwarded.
For diverged branches, a file that both sides changed is merged as `dsn~merge-of-companions~1` says if it is the todo.txt file, `ice_recur.txt` or a file of the plan.
A file that one side changed is taken from that side as it is, so that an order of the lines that one device changed is kept.
A conflict in any other file fails the synchronization and leaves the working tree as it was.
A push that is rejected because the remote branch moved is retried once after another fetch and merge.

Covers:

* `req~git-sync~1`
* `req~no-merge-conflicts~1`

Needs: impl, utest

### Line merge
`dsn~line-merge~1`

`mergeLines` computes the merged file from the common ancestor, our version and their version.
It starts from their version, removes the lines we removed and appends the lines we added.
If we removed as many lines as we added, each added line takes the place of the removed one.
A line that both sides added is kept once.
A line that both sides changed differently is kept in both versions.

Covers:

* `req~no-merge-conflicts~1`

Needs: impl, utest

### Dates from the history
`dsn~dates-from-history~1`

`taskDates` walks through the versions of the file, oldest first.
A task is added at the first version that contains it and completed at the first version in which it carries the completion marker; reopening forgets the completion.
A task is followed by its body without the `pri:` tag.
If a version removes as many tasks as it adds, these are taken as edits and keep their dates.
`GitStorage.dates` supplies the versions from the commits that changed the file, with the author date of each commit.

Covers:

* `req~dates-from-history~1`
* `adr~dates-from-history~1`

Needs: impl, utest

### Dates in the edit dialog
`dsn~dates-in-dialog~1`

The edit dialog shows the dates of the task from the history above its buttons.

Covers:

* `req~dates-from-history~1`

Needs: impl, utest

### Synchronization status
`dsn~sync-status~1`

For a synchronizing storage, the top bar shows one button that starts a synchronization and whose icon is the status: the arrows of a synchronization around a check mark, alone, or around an exclamation mark.
While a synchronization runs, the button is disabled and its arrows turn, as arrows that stand still read as an offer to synchronize; they keep the color of the other buttons, as the faded color of a disabled button is hardly seen.
Its description, which is its tooltip, names the status, the minute of the last synchronization that succeeded and, for a failure, the cause in full.
Only a failure adds words, in the color of an error: it has a cause to tell, and beside a status in words, a phone breaks the name of the application into two lines.
These words stand on a line of their own below the row of the views, in the width of the window.
Among the buttons, they take the room of the buttons: a phone loses the last ones out of view, and a wide window moves them to the next line until the next success.
The controller starts a synchronization when it opens a file; the storage starts one after every write.

Covers:

* `req~sync-status~1`

Needs: impl, utest

### Periodic synchronization
`dsn~periodic-sync~1`

The main screen starts a synchronization every five minutes, so that an application left open receives the changes of other devices without a write of its own.

Covers:

* `req~git-sync~1`

Needs: impl, utest

### SSH key
`dsn~ssh-key~1`

`GitSetup` keeps an ECDSA key pair (NIST P-256) in OpenSSH format in a directory of the application.
The pair is created when the public key is first asked for.

Covers:

* `req~ssh-key~1`
* `adr~ssh-key-in-app~1`

Needs: impl, utest

### Host keys
`dsn~host-keys~1`

The SSH directory of the application contains a `config` file with `StrictHostKeyChecking accept-new` and a `known_hosts` file.
The key of a host is recorded on the first connection; a host that later presents another key is rejected.

Covers:

* `req~ssh-key~1`
* `adr~ssh-key-in-app~1`

Needs: impl, utest

### Clone
`dsn~clone~1`

`GitSetup.clone` clones the repository into a directory of the application and returns the location of `todo.txt` in it.
With `ownKey = false`, it uses the SSH configuration of the user instead of the key of the application.
An existing clone of the same repository is kept, including changes that are not pushed yet; a clone of another repository is replaced.
`GitSetup.check` asks the server for the branches of the repository in the same way and clones nothing: it fails as a clone would if the server is not reached or does not let the device in.

Covers:

* `req~clone-in-app~1`

Needs: impl, utest

### Clone by its address
`dsn~clone-by-address~1`

The settings keep the address of a repository that the application cloned, not the location of the clone.
`GitSetup.storageForAddress` is the storage of the todo.txt file in the clone of that repository; without a clone of it, as after a new installation that restored the settings but not the clone, its first synchronization clones the repository again in the background.
Until the clone is done, the storage has no tasks and writing fails.
`GitSetup.addressOf` names the repository of the clone for settings that name the todo.txt file of the clone, as those of earlier versions; the application replaces such a file in the settings by the address when it starts.

Covers:

* `req~clone-in-app~1`

Needs: impl, utest

### Missing clone
`dsn~missing-clone~1`

A file in a clone of the application whose repository is gone, and the todo.txt file of a clone that has none, cannot be read: the application reports that instead of showing no tasks.
`GitSetup.storageFor` gives such a file a storage that fails to read and to write; any other file that does not exist is a new, empty file.

Covers:

* `req~clone-in-app~1`

Needs: impl, utest

### Home folder for SSH
`dsn~ssh-home~1`

The SSH library asks for the home folder of the user when it is loaded and fails where the system has none, as Android.
`GitSetup` then names the directory of the application as that folder.
The tests of `:core` run without a home folder, so that the test of the SSH transport fails if this is missing.

Covers:

* `req~clone-in-app~1`

Needs: impl, utest

### Git section of the settings dialog
`dsn~git-setup-dialog~1`

Where the platform passes `GitSetupActions` to `App`, the field for the file in the settings dialog also takes the address of a repository: a URL with the scheme `ssh`, `git`, `http` or `https`, or the form `user@host:path` (`isGitAddress`).
The button *Clone* below the field is enabled while the field holds such an address, and *Save* is not, as an address is no file; the address of the repository that is open is saved as it is.
*Clone* clones the address, makes the address the file of the application through `onOpenFile` and leaves the dialog, so that the tasks of the repository show; nothing that stood in the dialog is written, neither to the repository nor to the file from before.
If the clone fails, the dialog stays and shows the reason in the color for errors and, where the application has a key of its own, asks whether that key is registered.
The reason is the message of the failure followed by its causes (`failureText`), as the library that clones often reports only that the connection ended; the failure also goes to the log.
The dialog scrolls to the message of a clone or a check, which stands at the field, above the buttons.
The button *Check connection* below the field, next to those that choose a file or a folder and before *Clone*, is enabled while the field holds such an address: it asks the server for the repository without cloning it and shows *Connected*, or *Not connected* with the reason as for a clone that fails.
The address of a page of a repository of GitHub, GitLab or Codeberg (`gitlab.com` and `codeberg.org`, not a server of their own), as the browser has it, becomes the address for SSH of that repository (`sshAddress`) when it is pasted and before it is cloned: the application has an SSH key and no password for HTTPS.
The dialog shows the public key of the device where the actions have one; a click on the key copies it to the clipboard.
While the field holds the address of a repository of one of these servers, a button below the key copies the key and opens the page of that repository that adds a deploy key (`deployKeyPage`) in the browser; the text below the button names the steps on that page.
A server of its own is not recognized, as its name does not tell whether it runs GitLab, Forgejo or something else.
The application does not add the key itself: the API for deploy keys needs a login with the rights of an administrator of the repository, which the browser has and the application has not.
The Android application passes actions with the key of the application; the desktop application passes actions without a key, which clone into the data directory with the SSH configuration of the user.

Covers:

* `req~ssh-key~1`
* `req~clone-in-app~1`

Needs: impl, utest

### Shared lists in one storage
`dsn~shared-lists~1`

`SharedLists` is a storage that combines the storage of the main file with one storage per shared list, each a `GitStorage` or a `FileStorage` as for the main file, or the storage of a clone by its address.
It reads the lines of the main file, followed by those of each list with the context of the list appended (`SharedLists.shown`), unless the line has that context or a level below it.
On a write, it gives each line that has the context of a list, or a level below it, to the first such list without the context (`SharedLists.stripped`), and every other line to the main file.
A line without a task, such as a blank line, goes with the next line that has one, as it usually heads it, and at the end of the text with the line above.
It writes only the files whose lines changed or whose companion files were written.
A note that does not exist yet waits until the line that names it is written and goes to the file of that line, with the images that it shows.
The note of a line that is new in a file, of a task that moved, is copied there with the images that it shows, and deleted where it was unless a line of that file still names it.
A note is read from the first file that has it; the files next to the todo.txt file other than the notes and the schedule are those of the main file.
A list whose file cannot be read shows no tasks, and its failure is reported as the synchronization status; a write that would change its lines fails before any file is written.
The synchronization status is the first failure of a storage, prefixed with the context of its list, else the first running one.
Without a shared list, and for a main storage without companion files, the main storage is used alone (`SharedLists.of`).
The name of a list is the last part of its address without `.git`, or the directory of its file for an entry that ends with `.txt` (`SharedLists.name`).

Covers:

* `req~shared-lists~1`
* `adr~shared-lists~1`

Needs: impl, utest

### Lists of the members in one repository
`dsn~member-lists~1`

`memberFiles` gives the todo.txt files of the directories next to a todo.txt file, in the order of the names of the directories and without the directories whose name starts with a full stop.
`GitSetup.listsWithMembers` puts them behind the list that they lie in: those next to the main file first, then each shared list of the settings followed by those next to its file; a main file that is a document of Android has none.
The desktop application and the Android application pass the result to `SharedLists.of`, so that such a list is a shared list in everything else, named after its directory by `SharedLists.name`.
The directories are read when the storage is built: one that arrives with a synchronization shows after the next start.
Two instances of `GitStorage` for one repository share one lock and one background thread, as they share the index and the remote.
A merge covers the whole repository: `GitStorage` merges every file named `todo.txt`, `ice_recur.txt` or `timelog.txt` line by line and takes the newer one of every `calendar.txt` with its `calendar.ics`, as it does for the files of its own list.

Covers:

* `req~member-lists~1`
* `adr~member-lists~1`

Needs: impl, utest

### Schedule of the shared lists
`dsn~shared-ice-recur~1`

`SharedLists` reads `ice_recur.txt` as the schedule of the main file followed by the schedules of the lists, each entry of a list with the context of the list appended to its task, which ends the line; a comment stays as it is.
It writes the schedule split as the lines of the todo.txt file, an entry by the contexts of its task and a comment with the next entry, and writes only the schedules that changed; a list without a schedule gets none for no entries.
The tasks that the schedule adds thus carry the context of their list and are written into it.

Covers:

* `req~shared-lists~1`

Needs: impl, utest

### Setting of the shared lists
`dsn~shared-lists-setting~1`

The settings dialog shows the shared lists below the field of the file as rows, one per entry: the context of the list, its address or file, the button *Check* for an address, which checks the connection as for the main repository, and the button *Remove*, which leaves the clone alone.
A row says so when another list has the same context, whose tasks go to the first of them.
*Add folder*, where the platform chooses folders, adds the todo.txt file of a folder of the file system.
A change of the shared lists opens the files anew, as a change of the file does.

Covers:

* `req~shared-lists~1`

Needs: impl, utest

### Clone of a shared list
`dsn~shared-list-clone~1`

Below the shared lists, where the platform passes `GitSetupActions`, the settings dialog has a field for the address of a shared repository and the button *Clone*, enabled while the field holds an address (`isGitAddress`).
*Clone* clones the repository with `GitSetup.cloneShared` and adds its address as a row; *Save* then opens it, and the dialog says with which context its tasks show.
If the clone fails, the dialog shows the reason in the color for errors, and the failure goes to the log.
The settings keep the address, as for the main repository (`dsn~clone-by-address~1`): `GitSetup.storageForSharedAddress` clones the repository again in the background when the clone is gone, and settings that name the clone of a shared list get its address (`GitSetup.withAddresses`).
`GitSetup.cloneShared` clones into a directory of `shared` next to the main clone that is named after the repository.
A clone of the same repository is kept; a directory that holds anything else, such as the clone of another repository of the same name, is not replaced, and the name of the directory takes a number, as in `household-2`.

Covers:

* `req~shared-lists~1`

Needs: impl, utest
