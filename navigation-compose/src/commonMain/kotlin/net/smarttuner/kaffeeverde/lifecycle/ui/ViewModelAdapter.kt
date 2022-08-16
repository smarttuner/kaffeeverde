/*
 *    Original source code is Copyright (c) 2021 Tlaster
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy
 *    of this software and associated documentation files (the "Software"), to deal
 *    in the Software without restriction, including without limitation the rights
 *    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *    copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all
 *    copies or substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *    SOFTWARE.
 *
 *
 *
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://github.com/Tlaster/PreCompose/blob/master/precompose/src/commonMain/kotlin/moe/tlaster/precompose/ui/ViewModelAdapter.kt
 *
 * The content of this file is a port of the original work with some additions
 *
 *
 */
package net.smarttuner.kaffeeverde.lifecycle.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.smarttuner.kaffeeverde.lifecycle.ViewModel
import net.smarttuner.kaffeeverde.lifecycle.ViewModelStoreOwner
import kotlin.reflect.KClass

@Composable
inline fun <reified T : ViewModel> viewModel(
    keys: List<Any?> = emptyList(),
    noinline creator: () -> T,
): T = viewModel(T::class, keys, creator = creator)

@Composable
fun <T : ViewModel> viewModel(
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

fun <T : ViewModel> ViewModelStoreOwner.getViewModel(
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