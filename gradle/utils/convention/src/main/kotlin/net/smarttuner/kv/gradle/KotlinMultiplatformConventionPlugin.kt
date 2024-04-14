// Copyright 2023, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package net.smarttuner.kv.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

@OptIn(ExperimentalKotlinGradlePluginApi::class)
class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.multiplatform")
        }

        kotlin {
            jvm{
                compilations.all {
                    kotlinOptions.jvmTarget = "17"
                }
            }
            androidTarget()

            iosX64()
            iosArm64()
            iosSimulatorArm64()
            configureKotlin()
        }
    }
}

fun Project.addKspDependencyForAllTargets(dependencyNotation: Any) = addKspDependencyForAllTargets("", dependencyNotation)
fun Project.addKspTestDependencyForAllTargets(dependencyNotation: Any) = addKspDependencyForAllTargets("Test", dependencyNotation)

private fun Project.addKspDependencyForAllTargets(
    configurationNameSuffix: String,
    dependencyNotation: Any,
) {
    val kmpExtension = extensions.getByType<KotlinMultiplatformExtension>()
    dependencies {
        kmpExtension.targets
            .asSequence()
            .filter { target ->
                // Don't add KSP for common target, only final platforms
                target.platformType != KotlinPlatformType.common
            }
            .forEach { target ->
                add(
                    "ksp${target.targetName.capitalized()}$configurationNameSuffix",
                    dependencyNotation,
                )
            }
    }
}

internal fun Project.kotlin(action: KotlinMultiplatformExtension.() -> Unit) = extensions.configure<KotlinMultiplatformExtension>(action)
