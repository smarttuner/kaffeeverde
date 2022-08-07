/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/lifecycle/lifecycle-viewmodel/src/main/java/androidx/lifecycle/ViewModelStore.java
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.lifecycle

/**
 * Class to store `ViewModels`.
 *
 *
 * An instance of `ViewModelStore` must be retained through configuration changes:
 * if an owner of this `ViewModelStore` is destroyed and recreated due to configuration
 * changes, new instance of an owner should still have the same old instance of
 * `ViewModelStore`.
 *
 *
 * If an owner of this `ViewModelStore` is destroyed and is not going to be recreated,
 * then it should call [.clear] on this `ViewModelStore`, so `ViewModels` would
 * be notified that they are no longer used.
 *
 *
 * Use [ViewModelStoreOwner.getViewModelStore] to retrieve a `ViewModelStore` for
 * activities and fragments.
 */
class ViewModelStore {
    private val mMap: HashMap<String, ViewModel> = HashMap()
    fun put(key: String, viewModel: ViewModel) {
        val oldViewModel: ViewModel? = mMap.put(key, viewModel)
        oldViewModel?.onCleared()
    }

    operator fun get(key: String): ViewModel? {
        return mMap[key]
    }

    fun keys(): Set<String> {
        return mMap.keys
    }

    /**
     * Clears internal storage and notifies ViewModels that they are no longer used.
     */
    fun clear() {
        for (vm in mMap.values) {
            vm.clear()
        }
        mMap.clear()
    }
}