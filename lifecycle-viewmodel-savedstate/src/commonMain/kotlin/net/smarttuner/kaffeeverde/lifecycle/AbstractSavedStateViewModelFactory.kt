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
/*
 *
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/lifecycle/lifecycle-viewmodel-savedstate/src/main/java/androidx/lifecycle/AbstractSavedStateViewModelFactory.java
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.lifecycle

import net.smarttuner.kaffeeverde.core.Bundle


/**
 * Skeleton of androidx.lifecycle.ViewModelProvider.KeyedFactory
 * that creates [SavedStateHandle] for every requested [androidx.lifecycle.ViewModel].
 * The subclasses implement [.create] to actually instantiate
 * `androidx.lifecycle.ViewModel`s.
 */
abstract class AbstractSavedStateViewModelFactory : ViewModelProvider.OnRequeryFactory,
    ViewModelProvider.Factory {
    private var mSavedStateRegistry: SavedStateRegistry? = null
    private var mLifecycle: Lifecycle? = null
    private var mDefaultArgs: Bundle? = null

    /**
     * Constructs this factory.
     *
     *
     * When a factory is constructed this way, a component for which [SavedStateHandle] is
     * scoped must have called
     * [SavedStateHandleSupport.enableSavedStateHandles].
     * See [SavedStateHandleSupport.createSavedStateHandle] docs for more
     * details.
     */
    constructor() {}

    /**
     * Constructs this factory.
     *
     * @param owner [SavedStateRegistryOwner] that will provide restored state for created
     * [ViewModels][androidx.lifecycle.ViewModel]
     * @param defaultArgs values from this `Bundle` will be used as defaults by
     * [SavedStateHandle] passed in [ViewModels][ViewModel]
     * if there is no previously saved state
     * or previously saved state misses a value by such key
     */
    
    constructor(
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle?
    ) {
        mSavedStateRegistry = owner.savedStateRegistry
        mLifecycle = owner._lifecycle
        mDefaultArgs = defaultArgs
    }


    companion object {
        const val TAG_SAVED_STATE_HANDLE_CONTROLLER = "androidx.lifecycle.savedstate.vm.tag"
    }
}