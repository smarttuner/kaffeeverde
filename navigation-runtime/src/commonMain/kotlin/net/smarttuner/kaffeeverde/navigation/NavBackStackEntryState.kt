/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/navigation/navigation-runtime/src/main/java/androidx/navigation/NavBackStackEntryState.kt
 */
package net.smarttuner.kaffeeverde.navigation

import androidx.lifecycle.Lifecycle
import net.smarttuner.kaffeeverde.core.Bundle
import net.smarttuner.kaffeeverde.core.Serializable


internal class NavBackStackEntryState(entry: NavBackStackEntry) : Serializable {
    val id: String
    val destinationId: Int
    val args: Bundle?
    val savedState: Bundle

    init {
        id = entry.id
        destinationId = entry.destination.id
        args = entry.arguments
        savedState = Bundle()
        entry.saveState(savedState)
    }
    fun instantiate(
        destination: NavDestination,
        hostLifecycleState: Lifecycle.State,
        viewModel: NavControllerViewModel?
    ): NavBackStackEntry {
        return NavBackStackEntry.create(
            destination, args,
            hostLifecycleState, viewModel,
            id, savedState
        )
    }
}