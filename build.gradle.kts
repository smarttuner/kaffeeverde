// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.cacheFixPlugin) apply false
}

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        google()
    }

    dependencies {
        classpath(Deps.kotlin_gradle_plugin)
        classpath(Deps.android_gradle_plugin)
        classpath(Deps.kotlin_serialization_gradle_plugin)
    }
}

allprojects {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")
        mavenCentral()
        mavenLocal()
        google()
    }
    afterEvaluate {
        project.extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()?.let { ext ->
            ext.sourceSets.removeAll { sourceSet ->
                setOf(
                    "androidAndroidTestRelease",
                    "androidTestFixtures",
                    "androidTestFixturesDebug",
                    "androidTestFixturesRelease",
                ).contains(sourceSet.name)
            }
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

