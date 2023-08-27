plugins {
    id(mavenPublish)
    id("net.smarttuner.gradle.kv.android.library")
    id("net.smarttuner.gradle.kv.commonConfig")
    id("net.smarttuner.gradle.kv.kotlin.multiplatform")
}

version = Versions.KAFFEEVERDE_CORE

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinStdlib)
                api(libs.kotlinStdlibCommon)
                api(Deps.Ktor.Core)
                api(Deps.UUID.UUID)
                api(Deps.Napier.Napier)
                implementation(Deps.DitchoomBuffer.DitchoomBuffer)
                api(libs.collection)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting
        val androidUnitTest by getting
        val desktopMain by getting
        val desktopTest by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by getting {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by getting {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "net.smarttuner.kaffeeverde.core"
}