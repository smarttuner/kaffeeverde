plugins {
    id("net.smarttuner.gradle.kv.android.library")
    id("net.smarttuner.gradle.kv.commonConfig")
    id("net.smarttuner.gradle.kv.kotlin.multiplatform")
    id(composePlugin) version Versions.COMPOSE_MULTIPLATFORM_PLUGIN
    id("net.smarttuner.gradle.kv.configmaven")
}

version = Versions.KAFFEEVERDE_COMPOSE_HELPER

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.lifecycle.common)
                implementation(libs.lifecycle.viewmodel)
                implementation(project(":lifecycle-viewmodel-savedstate"))
                api(compose.ui)
                api(compose.foundation)
                api(compose.runtime)
            }
        }
    }
}

android {
    namespace = "net.smarttuner.kaffeeverde.compose.helper"
}
