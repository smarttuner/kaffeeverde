plugins {
    id("net.smarttuner.gradle.kv.android.library")
    id("net.smarttuner.gradle.kv.commonConfig")
    id("net.smarttuner.gradle.kv.kotlin.multiplatform")
    id(composePlugin) version Versions.COMPOSE_MULTIPLATFORM_PLUGIN
    id("net.smarttuner.gradle.kv.configmaven")
}

version = Versions.KAFFEEVERDE_APPLICATION_SUPPORT
description = "KaffeeVerde Application Support"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":navigation-compose"))
                api(compose.ui)
                api(compose.foundation)
                api(compose.runtime)
                implementation(project(":compose-helper"))
            }
        }
        androidMain {
            dependencies {
                implementation(Deps.AndroidXPlatform.AppCompat)
                implementation(Deps.AndroidXPlatform.ActivityKtx)
                implementation(libs.lifecycle.runtime)
                implementation(libs.lifecycle.runtime.compose)
            }
        }
    }
}

android {
    namespace = "net.smarttuner.kaffeeverde.application"
}
