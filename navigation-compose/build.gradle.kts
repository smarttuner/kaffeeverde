plugins {
    id("net.smarttuner.gradle.kv.android.library")
    id("net.smarttuner.gradle.kv.commonConfig")
    id("net.smarttuner.gradle.kv.kotlin.multiplatform")
    id(composePlugin) version Versions.COMPOSE_MULTIPLATFORM_PLUGIN
    id(mavenPublish)
}

version = Versions.KAFFEEVERDE_NAVIGATION_COMPOSE

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":navigation-runtime"))
                api(project(":compose-helper"))
                api(compose.ui)
                api(compose.foundation)
                api(compose.runtime)
                implementation(libs.lifecycle.runtime)
                api(libs.kotlinStdlib)
                api(libs.kotlinStdlibCommon)

            }
        }
    }
}

android {
    namespace = "net.smarttuner.kaffeeverde.navigation.compose"
}