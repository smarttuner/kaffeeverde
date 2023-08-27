// SPDX-License-Identifier: Apache-2.0
package net.smarttuner.kv.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class CommonPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        group = Versions.KAFFEEVERDE_LIB_GROUP
    }
}