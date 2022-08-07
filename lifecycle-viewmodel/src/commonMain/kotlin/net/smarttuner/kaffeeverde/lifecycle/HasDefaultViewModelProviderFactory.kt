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
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/lifecycle/lifecycle-viewmodel/src/main/java/androidx/lifecycle/HasDefaultViewModelProviderFactory.java
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.lifecycle

import net.smarttuner.kaffeeverde.lifecycle.viewmodel.CreationExtras


/**
 * Interface that marks a [ViewModelStoreOwner] as having a default
 * [androidx.lifecycle.ViewModelProvider.Factory] for use with
 * [androidx.lifecycle.ViewModelProvider.ViewModelProvider].
 */
interface HasDefaultViewModelProviderFactory {
    /**
     * Returns the default [androidx.lifecycle.ViewModelProvider.Factory] that should be
     * used when no custom `Factory` is provided to the
     * [androidx.lifecycle.ViewModelProvider] constructors.
     *
     * @return a `ViewModelProvider.Factory`
     */
    val defaultViewModelProviderFactory: ViewModelProvider.Factory

    /**
     * Returns the default [CreationExtras] that should be passed into the
     * [ViewModelProvider.Factory.create] when no overriding
     * [CreationExtras] were passed to the
     * [androidx.lifecycle.ViewModelProvider] constructors.
     */
    val defaultViewModelCreationExtras: CreationExtras
        get() = CreationExtras.Empty
}