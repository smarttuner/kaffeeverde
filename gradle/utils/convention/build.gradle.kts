plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    compileOnly(libs.androidGradlePluginz)
    compileOnly(libs.kotlinGradlePluginz)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "net.smarttuner.gradle.kv.android.library"
            implementationClass = "net.smarttuner.kv.gradle.AndroidLibraryConventionPlugin"
        }
        register("kotlinMultiplatform") {
            id = "net.smarttuner.gradle.kv.kotlin.multiplatform"
            implementationClass = "net.smarttuner.kv.gradle.KotlinMultiplatformConventionPlugin"
        }
        register("commonConfig") {
            id = "net.smarttuner.gradle.kv.commonConfig"
            implementationClass = "net.smarttuner.kv.gradle.CommonPlugin"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}