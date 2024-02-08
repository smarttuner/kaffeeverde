// SPDX-License-Identifier: Apache-2.0
package net.smarttuner.kv.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import java.net.URI

class ConfigMavenPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("maven-publish")
            apply("org.jetbrains.kotlin.multiplatform")
        }
        //configureToGitHubPackages()
    }
}

private fun Project.configureToGitHubPackages() {
    kotlin{
        val publicationsFromMainHost =
            listOf(
                androidTarget(),
                jvm("desktop"),
                iosX64(),
                iosArm64(),
                iosSimulatorArm64()
            ).map { it.name } + "kotlinMultiplatform"
        publishing {
            publications {
                matching { it.name in publicationsFromMainHost }.all {
                    val targetPublication = this@all
                    tasks.withType<AbstractPublishToMaven>()
                        .matching { it.publication == targetPublication }
                        .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
                }
            }
        }
        if (pluginManager.hasPlugin("com.android.library")) {
            androidTarget{
                publishLibraryVariants("release", "debug")
            }
        }
    }
    publishing {
        publications.withType<MavenPublication>().configureEach {
            setupPublicationPom(project)
        }
        repositories {
            maven {
                name = "KaffeeVerde"
                url = URI("https://maven.pkg.github.com/smarttuner/kaffeeverde")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}

internal fun MavenPublication.setupPublicationPom(
    project: Project) {
    pom {
        name.set(project.name)
        description.set(project.description)
        url.set("https://github.com/smarttuner/kaffeeverde")

        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }

        developers {
            developer {
                id.set("smarttuner")
                name.set("Daniele Saitta")
                email.set("opensource@smarttuner.net")
            }
        }

        scm {
            url.set("https://github.com/smarttuner/kaffeeverde")
            connection.set("scm:git:git://github.com/smarttuner/kaffeeverde.git")
            developerConnection.set("scm:git:git://github.com/smarttuner/kaffeeverde.git")
        }
    }
}

internal fun Project.publishing(action: PublishingExtension.() -> Unit) = extensions.configure<PublishingExtension>(action)
