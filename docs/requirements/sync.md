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

The settings name further todo.txt files, such as those of clones of repositories shared with other people.
Their tasks are shown together with those of the main file, each with a context named after the directory of its file.
A new task with that context, or with a level below it, is written into that file, a task without such a context into the main file; changing the context moves the task with its note.
A line of a file is written back unchanged unless the user edited it; the context of the list is not written into the file of the list.
Where the application clones a repository, it clones the repository of a shared list as well and adds its todo.txt file to the shared lists.

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

Of the files that both sides of a merge changed, the todo.txt file and `ice_recur.txt` are the result of `mergeLines`, as both consist of lines that stand for themselves.
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
An empty field stands for the value of the git configuration, `user.name` or `user.email`, and without one for `koppter` or `koppter@localhost`; the empty field shows that default in gray, with the title above the field so that it does not hide the default.
`GitStorage` makes each commit in that name as author and committer; a change of either field opens the storage again, so that the next commit uses it.

Covers:

* `req~git-sync~1`

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

For a synchronizing storage, the top bar shows the status as text and a button that starts a synchronization.
The controller starts a synchronization when it opens a file; the storage starts one after every write.

Covers:

* `req~sync-status~1`

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
The button *Clone* below the field is enabled while the field holds such an address, and *Save* is not, as an address is no file.
*Clone* clones the address, makes the todo.txt file of the clone the file of the application through `onOpenFile` and leaves the dialog, so that the tasks of the repository show; nothing that stood in the dialog is written, neither to the repository nor to the file from before.
If the clone fails, the dialog stays and shows the reason in the color for errors and, where the application has a key of its own, asks whether that key is registered.
The reason is the message of the failure followed by its causes (`failureText`), as the library that clones often reports only that the connection ended; the failure also goes to the log.
The dialog scrolls to the message of a clone or a check, which stands at the field, above the buttons.
The button *Check connection* below the field, next to those that choose a file or a folder and before *Clone*, is enabled while the field holds such an address: it asks the server for the repository without cloning it and shows *Connected*, or *Not connected* with the reason as for a clone that fails.
The address of a page of a repository of GitHub, as the browser has it, becomes the address for SSH of that repository (`sshAddress`) when it is pasted and before it is cloned: the application has an SSH key and no password for HTTPS.
The dialog shows the public key of the device where the actions have one; a click on the key copies it to the clipboard.
While the field holds the address of a repository of GitHub, a button below the key copies the key and opens the page of that repository that adds a deploy key (`deployKeyPage`) in the browser.
The application does not add the key itself: the API for deploy keys needs a login with the rights of an administrator of the repository, which the browser has and the application has not.
The Android application passes actions with the key of the application; the desktop application passes actions without a key, which clone into the data directory with the SSH configuration of the user.

Covers:

* `req~ssh-key~1`
* `req~clone-in-app~1`

Needs: impl, utest

### Shared lists in one storage
`dsn~shared-lists~1`

`SharedLists` is a storage that combines the storage of the main file with one storage per shared list, each a `GitStorage` or a `FileStorage` as for the main file.
It reads the lines of the main file, followed by those of each list with the context of the list appended (`SharedLists.shown`), unless the line has that context or a level below it.
On a write, it gives each line that has the context of a list, or a level below it, to the first such list without the context (`SharedLists.stripped`), every other line to the main file, and a blank line to the file of the line above it.
It writes only the files whose lines changed or whose companion files were written.
A note that does not exist yet waits until the line that names it is written and goes to the file of that line, with the images that it shows; the note of a line that is new in a file is copied there from the file that has it.
A note is read from the first file that has it; the other companion files are those of the main file.
The synchronization status is the first failure of a storage, prefixed with the name of its list, else the first running one.
Without a shared list, and for a main storage without companion files, the main storage is used alone (`SharedLists.of`).

Covers:

* `req~shared-lists~1`
* `adr~shared-lists~1`

Needs: impl, utest

### Setting of the shared lists
`dsn~shared-lists-setting~1`

The settings dialog has the field *Shared lists* below the field of the file, with one todo.txt file per line.
A change of the field opens the files anew, as a change of the file does.

Covers:

* `req~shared-lists~1`

Needs: impl

### Clone of a shared list
`dsn~shared-list-clone~1`

Below the field *Shared lists*, where the platform passes `GitSetupActions`, the settings dialog has a field for the address of a shared repository and the button *Clone*, enabled while the field holds an address (`isGitAddress`).
*Clone* clones the repository with `GitSetup.cloneShared` and adds its todo.txt file to the field; *Save* then opens it, and the dialog says with which context its tasks show.
If the clone fails, the dialog shows the reason in the color for errors, and the failure goes to the log.
`GitSetup.cloneShared` clones into a directory of `shared` next to the main clone that is named after the repository, as the name of that directory is the context of the list.
A clone of the same repository is kept; a directory that holds anything else, such as the clone of another repository of the same name, is not replaced, and the name takes a number, as in `household-2`.

Covers:

* `req~shared-lists~1`

Needs: impl, utest
