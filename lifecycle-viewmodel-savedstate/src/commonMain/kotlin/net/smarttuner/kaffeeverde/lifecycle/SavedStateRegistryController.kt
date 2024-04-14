/*
 * Copyright 2018 The Android Open Source Project
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
/**
 * An API for [SavedStateRegistryOwner] implementations to control [SavedStateRegistry].
 *
 * `SavedStateRegistryOwner` should call [performRestore] to restore state of
 * [SavedStateRegistry] and [performSave] to gather SavedState from it.
 */
/*
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/savedstate/savedstate/src/main/java/androidx/savedstate/SavedStateRegistryController.kt
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.lifecycle

import androidx.lifecycle.Lifecycle
import net.smarttuner.kaffeeverde.core.Bundle

class SavedStateRegistryController private constructor(private val owner: SavedStateRegistryOwner) {
    /**
     * The [SavedStateRegistry] owned by this controller
     */
    val savedStateRegistry: SavedStateRegistry = SavedStateRegistry()
    private var attached = false
    /**
     * Perform the initial, one time attachment necessary to configure this
     * [SavedStateRegistry]. This must be called when the owner's [Lifecycle] is
     * [Lifecycle.State.INITIALIZED] and before you call [performRestore].
     */
    
    fun performAttach() {
        val lifecycle = owner.lifecycle
        check(lifecycle.currentState == Lifecycle.State.INITIALIZED) {
            ("Restarter must be created only during owner's initialization stage")
        }
        lifecycle.addObserver(Recreator(owner))
        savedStateRegistry.performAttach(lifecycle)
        attached = true
    }
    /**
     * An interface for an owner of this [SavedStateRegistry] to restore saved state.
     *
     * @param savedState restored state
     */
    
    fun performRestore(savedState: Bundle?) {
        // To support backward compatibility with libraries that do not explicitly
        // call performAttach(), we make sure that work is done here
        if (!attached) {
            performAttach()
        }
        val lifecycle = owner.lifecycle
        check(!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            ("performRestore cannot be called when owner is ${lifecycle.currentState}")
        }
        savedStateRegistry.performRestore(savedState)
    }
    /**
     * An interface for an owner of this  [SavedStateRegistry]
     * to perform state saving, it will call all registered providers and
     * merge with unconsumed state.
     *
     * @param outBundle Bundle in which to place a saved state
     */
    
    fun performSave(outBundle: Bundle) {
        savedStateRegistry.performSave(outBundle)
    }
    companion object {
        /**
         * Creates a [SavedStateRegistryController].
         *
         * It should be called during construction time of [SavedStateRegistryOwner]
         */
        fun create(owner: SavedStateRegistryOwner): SavedStateRegistryController {
            return SavedStateRegistryController(owner)
        }
    }
}