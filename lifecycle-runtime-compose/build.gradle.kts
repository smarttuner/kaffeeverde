plugins {
    id("net.smarttuner.gradle.kv.android.library")
    id("net.smarttuner.gradle.kv.commonConfig")
    id("net.smarttuner.gradle.kv.kotlin.multiplatform")
    id(composePlugin) version Versions.COMPOSE_MULTIPLATFORM_PLUGIN
    id("net.smarttuner.gradle.kv.configmaven")
}

version = Versions.KAFFEEVERDE_LIFECYCLE_RUNTIME_COMPOSE

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":lifecycle"))
                api(project(":compose-helper"))
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
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}

android {
    namespace = "net.smarttuner.kaffeeverde.lifecycle.runtime.compose"
}