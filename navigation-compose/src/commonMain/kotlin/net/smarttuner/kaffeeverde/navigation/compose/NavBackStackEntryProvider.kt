package net.smarttuner.kaffeeverde.navigation.compose

/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.benasher44.uuid.Uuid
import net.smarttuner.kaffeeverde.core.UUID
import net.smarttuner.kaffeeverde.lifecycle.ViewModel
import net.smarttuner.kaffeeverde.compose.ui.platform.LocalLifecycleOwner
import net.smarttuner.kaffeeverde.compose.ui.platform.LocalSavedStateRegistryOwner
import net.smarttuner.kaffeeverde.lifecycle.ui.LocalViewModelStoreOwner
import net.smarttuner.kaffeeverde.navigation.NavBackStackEntry

/**
 * Provides [this] [NavBackStackEntry] as [LocalViewModelStoreOwner], [LocalLifecycleOwner] and
 * [LocalSavedStateRegistryOwner] to the [content] and saves the [content]'s saveable states with
 * the given [saveableStateHolder].
 *
 * @param saveableStateHolder The [SaveableStateHolder] that holds the saved states. The same
 * holder should be used for all [NavBackStackEntry]s in the encapsulating [Composable] and the
 * holder should be hoisted.
 * @param content The content [Composable]
 */
@Composable
public fun NavBackStackEntry.LocalOwnersProvider(
    saveableStateHolder: SaveableStateHolder,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides this,
        LocalLifecycleOwner provides this,
        LocalSavedStateRegistryOwner provides this
    ) {
        saveableStateHolder.SaveableStateProvider(content)
    }
}
@Composable
private fun SaveableStateHolder.SaveableStateProvider(content: @Composable () -> Unit) {
    val viewModel = BackStackEntryIdViewModel()
    viewModel.saveableStateHolder = this
    SaveableStateProvider(viewModel.id, content)
    DisposableEffect(viewModel) {
        onDispose {
            viewModel.saveableStateHolder = null
        }
    }
}
internal class BackStackEntryIdViewModel() : ViewModel() {
    private val IdKey = "SaveableStateHolder_BackStackEntryKey"
    // we create our own id for each back stack entry to support multiple entries of the same
    // destination. this id will be restored by SavedStateHandle
    val id: Uuid = UUID.randomUUID()
    var saveableStateHolder: SaveableStateHolder? = null
    // onCleared will be called on the entries removed from the back stack. here we notify
    // RestorableStateHolder that we shouldn't save the state for this id, so when we open this
    // destination again the state will not be restored.
    override fun onCleared() {
        super.onCleared()
        saveableStateHolder?.removeState(id)
    }
}