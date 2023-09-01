// SPDX-License-Identifier: Apache-2.0
package net.smarttuner.kv.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import java.net.URI

class ConfigMavenPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("maven-publish")
        }
        configureToGitHubPackages()
    }
}

private fun Project.configureToGitHubPackages() {
    publishing {
        publications.withType<MavenPublication>().configureEach {
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
}

internal fun Project.publishing(action: PublishingExtension.() -> Unit) = extensions.configure<PublishingExtension>(action)
