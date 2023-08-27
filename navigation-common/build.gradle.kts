plugins {
    id("net.smarttuner.gradle.kv.android.library")
    id("net.smarttuner.gradle.kv.commonConfig")
    id("net.smarttuner.gradle.kv.kotlin.multiplatform")
    id(mavenPublish)
}

version = Versions.KAFFEEVERDE_NAVIGATION_COMMON

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":lifecycle-viewmodel-savedstate"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting
        val androidUnitTest by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by getting
    }
}

android {
    namespace = "net.smarttuner.kaffeeverde.navigation"
}