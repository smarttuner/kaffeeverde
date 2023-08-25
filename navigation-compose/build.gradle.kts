plugins {
    kotlin(multiplatform)
    id(androidLib)
    id(composePlugin) version Versions.COMPOSE_MULTIPLATFORM_PLUGIN
    id(mavenPublish)
}

group = Versions.KAFFEEVERDE_LIB_GROUP
version = Versions.KAFFEEVERDE_NAVIGATION_COMPOSE

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    ios()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":compose-helper"))
                api(project(":navigation-runtime"))
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
        val iosTest by getting
    }
}

android {
    compileSdk = Versions.ANDROID_COMPILE_SDK
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = Versions.ANDROID_MIN_SDK
        targetSdk = Versions.ANDROID_TARGET_SDK
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}