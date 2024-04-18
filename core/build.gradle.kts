plugins {
    id("net.smarttuner.gradle.kv.android.library")
    id("net.smarttuner.gradle.kv.commonConfig")
    id("net.smarttuner.gradle.kv.kotlin.multiplatform")
    id("net.smarttuner.gradle.kv.configmaven")
}

version = Versions.KAFFEEVERDE_CORE

kotlin {
    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
            }
        }
        commonMain {
            dependencies {
                api(libs.kotlinStdlib)
                api(libs.kotlinStdlibCommon)
                api(Deps.Ktor.Core)
                api(Deps.UUID.UUID)
                api(Deps.Napier.Napier)
                api(Deps.DitchoomBuffer.DitchoomBuffer)
                api(libs.collection)
                implementation(libs.kotlinSerializationJson)
            }
        }
        nativeMain {
            dependencies {
                implementation(libs.atomicFu)
            }
        }
        androidMain {
            dependencies {
                api(Deps.DitchoomBuffer.DitchoomBufferAndroid)
            }
        }
    }
}

android {
    namespace = "net.smarttuner.kaffeeverde.core"
}