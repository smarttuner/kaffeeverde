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
/**
 * A scope that owns [SavedStateRegistry]
 *
 * This owner should be passed in to create a [SavedStateRegistryController] object
 * through which this owner can access and perform operations via the
 * controller's [SavedStateRegistry]
 *
 * [SavedStateRegistryController.performAttach] must be called once (and only once) on the
 * main thread during the owner's [Lifecycle.State.INITIALIZED] state.
 * It should be called before you call [SavedStateRegistryController.performRestore]
 *
 * [SavedStateRegistryController.performRestore] can be called with a nullable if nothing
 * needs to be restored, or with the state Bundle to be restored. performRestore can be called
 * in one of two places:
 * 1. Directly before the Lifecycle moves to [Lifecycle.State.CREATED]
 * 2. Before [Lifecycle.State.STARTED] is reached, as part of the [LifecycleObserver]
 * that is added during owner initialization
 *
 * [SavedStateRegistryController.performSave] should be called after owner has been stopped but
 * before it reaches [Lifecycle.State.DESTROYED] state. Hence it should only be called once the
 * owner has received the [Lifecycle.Event.ON_STOP] event. The bundle passed to performSave
 * will be the bundle restored by performRestore.
 *
 * @see [ViewTreeSavedStateRegistryOwner]
 */
/*
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/savedstate/savedstate/src/main/java/androidx/savedstate/SavedStateRegistryOwner.kt *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.lifecycle

interface SavedStateRegistryOwner : LifecycleOwner {
    /**
     * The [SavedStateRegistry] owned by this SavedStateRegistryOwner
     */
    val savedStateRegistry: SavedStateRegistry
}