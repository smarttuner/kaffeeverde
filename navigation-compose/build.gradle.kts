plugins {
    id("net.smarttuner.gradle.kv.android.library")
    id("net.smarttuner.gradle.kv.commonConfig")
    id("net.smarttuner.gradle.kv.kotlin.multiplatform")
    id(composePlugin) version Versions.COMPOSE_MULTIPLATFORM_PLUGIN
    id("net.smarttuner.gradle.kv.configmaven")
}

version = Versions.KAFFEEVERDE_NAVIGATION_COMPOSE

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":compose-helper"))
                api(project(":navigation-runtime"))
                api(compose.ui)
                api(compose.foundation)
                api(compose.runtime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting
        val androidUnitTest by getting
        val iosMain by getting
        val iosTest by getting
    }
}

android {
    namespace = "net.smarttuner.kaffeeverde.navigation.compose"
}