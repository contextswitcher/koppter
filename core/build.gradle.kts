// [impl->adr~kotlin-multiplatform~1]
// Platform-independent todo.txt logic. Only a JVM target for now: it serves
// the desktop app and, as a JVM library, the Android app.

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(25)

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
        }
        jvmMain.dependencies {
            implementation(libs.jgit)
            implementation(libs.jgit.ssh)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            // An SSH server that serves a git repository, for the test of the SSH transport.
            implementation(libs.jgit.junit.ssh)
        }
    }
}

// As on Android, which has no home folder of the user: the SSH library fails there unless `GitSetup` names one.
// It also keeps the tests apart from the git configuration of whoever runs them.
tasks.withType<Test>().configureEach {
    systemProperty("user.home", "")
}
