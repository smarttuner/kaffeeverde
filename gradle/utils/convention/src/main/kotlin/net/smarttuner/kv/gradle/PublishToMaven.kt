package net.smarttuner.kv.gradle

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import java.net.URI

fun Project.publishToGitHubPackages() {
    plugins.apply("maven-publish")

    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = URI("https://maven.pkg.github.com/octocat/hello-world")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
    }
}