///usr/bin/env jbang "$0" "$@" ; exit $?
// Checks a todo.txt file: jbang .jbang/KoppterLint.kt <path of the todo.txt file>
// [impl->dsn~lint-jbang~1]
// The sources are listed one by one, as jbang reads no pattern from a web address.
//KOTLIN 2.4.21
//JAVA 21+
//DEPS org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.8.0
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/latex/Latex.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/outlook/Outlook.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/outlook/OutlookCalendar.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/plan/BusyTimes.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/plan/CalendarTxt.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/plan/Holidays.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/plan/Ics.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/plan/Planner.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/plan/TimeMaps.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/storage/ConfigFile.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/storage/Settings.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/storage/SharedLists.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/storage/TodoStorage.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/sync/LineMerge.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/sync/TaskHistory.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/ContextSwitcherSync.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/DatePhrase.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/Filter.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/Grouping.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/IceRecur.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/Lint.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/PriorityNames.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/Query.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/Recurrence.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/ShareAction.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/Task.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/TodoFile.kt
//SOURCES ../core/src/commonMain/kotlin/io/github/koppor/koppter/todotxt/Urgency.kt
//SOURCES ../core/src/jvmMain/kotlin/io/github/koppor/koppter/storage/LintFiles.kt

import io.github.koppor.koppter.storage.lintMain

fun main(args: Array<String>) = lintMain(args)
