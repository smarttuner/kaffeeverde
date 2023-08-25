package net.smarttuner.kaffeeverde.navigation.compose

/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.smarttuner.kaffeeverde.navigation.NavBackStackEntry
import net.smarttuner.kaffeeverde.navigation.NavDestination
import net.smarttuner.kaffeeverde.navigation.NavOptions
import net.smarttuner.kaffeeverde.navigation.Navigator

/**
 * Navigator that navigates through [Composable]s that will be hosted within a
 * [Dialog]. Every destination using this Navigator must  set a valid [Composable] by setting it
 * directly on an instantiated [Destination] or calling [dialog].
 */
@Navigator.Name("dialog")
public class DialogNavigator : Navigator<DialogNavigator.Destination>() {
    /**
     * Get the back stack from the [state].
     */
    internal val backStack get() = state.backStack
    /**
     * Dismiss the dialog destination associated with the given [backStackEntry].
     */
    internal fun dismiss(backStackEntry: NavBackStackEntry) {
        state.popWithTransition(backStackEntry, false)
    }
    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        entries.forEach { entry ->
            state.push(entry)
        }
    }
    override fun createDestination(): Destination {
        return Destination(this) { }
    }
    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.popWithTransition(popUpTo, savedState)
    }
    internal fun onTransitionComplete(entry: NavBackStackEntry) {
        state.markTransitionComplete(entry)
    }
    /**
     * NavDestination specific to [DialogNavigator]
     */
    @NavDestination.ClassType(Composable::class)
    public class Destination(
        navigator: DialogNavigator,
        internal val dialogProperties: DialogProperties = DialogProperties(),
        internal val content: @Composable (NavBackStackEntry) -> Unit
    ) : NavDestination(navigator)
    internal companion object {
        internal const val NAME = "dialog"
    }
}