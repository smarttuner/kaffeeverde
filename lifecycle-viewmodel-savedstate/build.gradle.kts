plugins {
    id("net.smarttuner.gradle.kv.android.library")
    id("net.smarttuner.gradle.kv.commonConfig")
    id("net.smarttuner.gradle.kv.kotlin.multiplatform")
    id("net.smarttuner.gradle.kv.configmaven")
}

version = Versions.KAFFEEVERDE_LIFECYCLE_VIEWMODEL_SAVEDSTATE

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":core"))
                api(libs.lifecycle.common)
                api(libs.lifecycle.viewmodel)
                //api(project(":lifecycle-viewmodel")) }
            }
        }
    }

}

android {
    namespace = "net.smarttuner.kaffeeverde.viewmodel"
}