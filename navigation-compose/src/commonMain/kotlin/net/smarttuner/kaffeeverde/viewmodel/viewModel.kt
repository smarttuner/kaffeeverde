/*
 * Copyright 2024 The Android Open Source Project
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

package net.smarttuner.kaffeeverde.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.smarttuner.kaffeeverde.lifecycle.ui.LocalViewModelStoreOwner
import kotlin.reflect.KClass

@Composable
public fun <T : ViewModel> viewModel(
    modelClass: KClass<T>,
    keys: List<Any?> = emptyList(),
    creator: () -> T,
): T {
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "Require LocalViewModelStoreOwner not null for $modelClass"
    }
    return remember(
        modelClass, keys, creator, viewModelStoreOwner
    ) {
        viewModelStoreOwner.getViewModel(keys, modelClass = modelClass, creator = creator)
    }
}

public fun <T : ViewModel> ViewModelStoreOwner.getViewModel(
    keys: List<Any?> = emptyList(),
    modelClass: KClass<T>,
    creator: () -> T,
): T {
    val key = (keys.map { it.hashCode().toString() } + modelClass.qualifiedName).joinToString()
    val existing = viewModelStore[key]
    if (existing != null && modelClass.isInstance(existing)) {
        @Suppress("UNCHECKED_CAST")
        return existing as T
    } else {
        @Suppress("ControlFlowWithEmptyBody")
        if (existing != null) {
            // TODO: log a warning.
        }
    }
    val viewModel = creator.invoke()
    viewModelStore.put(key, viewModel)
    return viewModel
}

