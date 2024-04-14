plugins {
    id("net.smarttuner.gradle.kv.android.library")
    id("net.smarttuner.gradle.kv.commonConfig")
    id("net.smarttuner.gradle.kv.kotlin.multiplatform")
    id(mavenPublish)
}

version = Versions.KAFFEEVERDE_NAVIGATION_RUNTIME

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":navigation-common"))
            }
        }
    }
}

android {
    namespace = "net.smarttuner.kaffeeverde.navigation.runtime"
}