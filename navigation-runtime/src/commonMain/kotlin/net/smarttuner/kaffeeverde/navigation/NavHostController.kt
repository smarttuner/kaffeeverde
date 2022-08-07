/*
 * Copyright 2019 The Android Open Source Project
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
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/navigation/navigation-runtime/src/main/java/androidx/navigation/NavHostController.kt
 */
package net.smarttuner.kaffeeverde.navigation

import net.smarttuner.kaffeeverde.lifecycle.LifecycleOwner
import net.smarttuner.kaffeeverde.lifecycle.ViewModelStore

/**
 * Subclass of [NavController] that offers additional APIs for use by a
 * [NavHost] to connect the NavController to external dependencies.
 *
 * Apps should generally not construct controllers, instead obtain a relevant controller
 * directly from a navigation host via [NavHost.getNavController] or by using one of
 * the utility methods on the [Navigation] class.
 */
open class NavHostController
/**
 * Construct a new controller for a given [Context] suitable for use in a
 * [NavHost]. Controllers should not be used outside of their context and retain a
 * hard reference to the context supplied. If you need a global controller, pass
 * [Context.getApplicationContext].
 *
 * Note that controllers that are not constructed with an [Activity] context
 * (or a wrapped activity context) will only be able to navigate to
 * [new tasks][android.content.Intent.FLAG_ACTIVITY_NEW_TASK] or
 * [new document tasks][android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT] when
 * navigating to new activities.
 *
 */
     : NavController() {
    /**
     * Sets the host's [LifecycleOwner].
     *
     * @param owner The [LifecycleOwner] associated with the containing [NavHost].
     * @see NavHostController.setOnBackPressedDispatcher
     */
    final override fun setLifecycleOwner(owner: LifecycleOwner) {
        super.setLifecycleOwner(owner)
    }

    /**
     * Sets the host's ViewModelStore used by the NavController to store ViewModels at the
     * navigation graph level. This is required to call [getViewModelStoreOwner] and
     * should generally be called for you by your [NavHost].
     *
     * You must call this method before [setGraph] or similar methods, because the
     * [ViewModelStore] set here will be used by the created [NavBackStackEntry] items.
     *
     * @param viewModelStore ViewModelStore used to store ViewModels at the navigation graph level
     * @throws IllegalStateException if this method is called when graph was already set via
     * [setGraph] or similar methods.
     */
    final override fun setViewModelStore(viewModelStore: ViewModelStore) {
        super.setViewModelStore(viewModelStore)
    }
}
