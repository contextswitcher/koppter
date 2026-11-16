// [impl->adr~kotlin-multiplatform~1]
// The application: Compose UI shared by all targets, plus the desktop entry point.

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    jvmToolchain(25)

    jvm("desktop")

    // [impl->dsn~android-shell~1]
    android {
        namespace = "io.github.koppor.koppter.ui"
        compileSdk = 37
        minSdk = 26
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons)
            implementation(libs.kotlinx.coroutines.core)
        }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.appdirs)
                // [impl->adr~file-dialogs-of-filekit~1]
                implementation(libs.filekit.dialogs)
                // [impl->adr~log-messages-through-tinylog~1]
                // JGit and its SSH client log through SLF4J, which complains at the start if nothing takes the messages.
                implementation(libs.tinylog.api)
                runtimeOnly(libs.tinylog.slf4j)
                runtimeOnly(libs.tinylog.impl)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.compose.ui.test)
            }
        }
    }
}

// The version of a package is the day of the release as YY.M.D:
// the formats of Windows and macOS take three numbers, of which the first is above 0 and below 256.
val releaseDay = providers.environmentVariable("KOPPTER_VERSION").orNull?.let { Regex("""\d\d(\d\d)-(\d\d)-(\d\d)""").matchEntire(it) }
val desktopVersion = releaseDay?.destructured?.let { (year, month, day) -> "$year.${month.toInt()}.${day.toInt()}" }

compose.desktop {
    application {
        mainClass = "io.github.koppor.koppter.MainKt"
        // Skia and JNA load native libraries, for which Java warns at the start and announces to refuse it one day
        // unless the application states that it wants this (JEP 472).
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "koppter"
            // A build that is not a release is below every release.
            packageVersion = desktopVersion ?: "0.1.0"
            description = "Tasks in a todo.txt file"
            // What `suggestRuntimeModules` finds for JGit, its SSH client, JNA and the D-Bus client of FileKit, plus naming for Kerberos and the bridge for screen readers.
            // All modules cannot be included: that list contains jlink itself, which a JDK without packaged modules cannot link.
            modules("java.instrument", "java.management", "java.naming", "java.rmi", "java.security.jgss", "java.sql", "jdk.accessibility", "jdk.security.auth", "jdk.unsupported")
            linux {
                iconFile = project.file("src/desktopMain/resources/koppter.png")
            }
            windows {
                iconFile = project.file("packaging/koppter.ico")
                // Identifies the application to the installer, so that a newer package replaces the installed one.
                upgradeUuid = "7978242a-0e73-4e9d-a3ff-1ade94c65369"
                menu = true
                perUserInstall = true
            }
            macOS {
                // macOS takes no version that starts with 0.
                packageVersion = desktopVersion ?: "1.0.0"
                iconFile = project.file("packaging/koppter.icns")
                bundleID = "io.github.koppor.koppter"
            }
        }
    }
}
