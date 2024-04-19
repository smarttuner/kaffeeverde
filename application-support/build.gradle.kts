import net.smarttuner.kv.gradle.configureKotlin

plugins {
    id("net.smarttuner.gradle.kv.commonConfig")
    id(composePlugin) version Versions.COMPOSE_MULTIPLATFORM_PLUGIN
    id("net.smarttuner.gradle.kv.configmaven")
}

version = Versions.KAFFEEVERDE_APPLICATION_SUPPORT
description = "KaffeeVerde Application Support"

kotlin {
    jvm{
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    configureKotlin()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":navigation-compose"))
                api(compose.ui)
                api(compose.foundation)
                api(compose.runtime)
                api(libs.lifecycle.runtime)
                implementation(project(":compose-helper"))
            }
        }
    }
}