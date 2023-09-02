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
        val commonMain by getting {
            dependencies {
                api(project(":navigation-compose"))
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
        val androidMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(Deps.AndroidXPlatform.AppCompat)
                implementation(Deps.AndroidXPlatform.ActivityKtx)
                implementation("androidx.lifecycle:lifecycle-runtime:2.6.1")
                implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")

            }
        }
        val androidUnitTest by getting
        val iosMain by getting
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}

android {
    namespace = "net.smarttuner.kaffeeverde.application"
}