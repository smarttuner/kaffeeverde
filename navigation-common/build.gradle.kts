plugins {
    id("net.smarttuner.gradle.kv.android.library")
    id("net.smarttuner.gradle.kv.commonConfig")
    id("net.smarttuner.gradle.kv.kotlin.multiplatform")
    id("net.smarttuner.gradle.kv.configmaven")
    alias(libs.plugins.kotlinSerialization)
}

version = Versions.KAFFEEVERDE_NAVIGATION_COMMON

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":lifecycle-viewmodel-savedstate"))
                implementation(libs.lifecycle.runtime)
                implementation(libs.kotlinSerializationJson)
            }
        }
    }
}

android {
    namespace = "net.smarttuner.kaffeeverde.navigation"
}