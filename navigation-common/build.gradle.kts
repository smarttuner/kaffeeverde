plugins {
    kotlin(multiplatform)
    id(androidLib)
    id(mavenPublish)
}

group = Versions.KAFFEEVERDE_LIB_GROUP
version = Versions.KAFFEEVERDE_NAVIGATION_COMMON

kotlin {
    android {
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
                api(project(":lifecycle-viewmodel-savedstate"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting
        val androidTest by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by getting
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