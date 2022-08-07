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
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/navigation/navigation-runtime/src/main/java/androidx/navigation/NavControllerViewModel.kt
 */
package net.smarttuner.kaffeeverde.navigation

import net.smarttuner.kaffeeverde.core.toHexString
import net.smarttuner.kaffeeverde.lifecycle.*
import kotlin.collections.Iterator
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.reflect.KClass

internal class NavControllerViewModel : ViewModel(), NavViewModelStoreProvider {
    private val viewModelStores = mutableMapOf<String, ViewModelStore>()
    fun clear(backStackEntryId: String) {
        // Clear and remove the NavGraph's ViewModelStore
        val viewModelStore = viewModelStores.remove(backStackEntryId)
        viewModelStore?.clear()
    }
    override fun onCleared() {
        for (store in viewModelStores.values) {
            store.clear()
        }
        viewModelStores.clear()
    }
    override fun getViewModelStore(backStackEntryId: String): ViewModelStore {
        var viewModelStore = viewModelStores[backStackEntryId]
        if (viewModelStore == null) {
            viewModelStore = ViewModelStore()
            viewModelStores[backStackEntryId] = viewModelStore
        }
        return viewModelStore
    }
    override fun toString(): String {
        val sb = StringBuilder("NavControllerViewModel{")
        sb.append(this.hashCode().toHexString())
        sb.append("} ViewModelStores (")
        val viewModelStoreIterator: Iterator<String> = viewModelStores.keys.iterator()
        while (viewModelStoreIterator.hasNext()) {
            sb.append(viewModelStoreIterator.next())
            if (viewModelStoreIterator.hasNext()) {
                sb.append(", ")
            }
        }
        sb.append(')')
        return sb.toString()
    }
    companion object {

        private val FACTORY: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: KClass<T>): T {
                return NavControllerViewModel() as T
            }
        }

        @Deprecated("Use instance instead")
        fun getInstance(viewModelStore: ViewModelStore): NavControllerViewModel {
            val viewModelProvider = ViewModelProvider(viewModelStore, FACTORY)
            var vm = viewModelProvider.get<NavControllerViewModel>()
            if(vm==null){
                val newVM = NavControllerViewModel()
                vm = newVM
                viewModelProvider.set(vm)
            }
            return vm
        }
    }
}