package net.smarttuner.kaffeeverde.application

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import net.smarttuner.kaffeeverde.compose.ui.platform.LocalLifecycleOwner
import net.smarttuner.kaffeeverde.lifecycle.ui.BackDispatcher
import net.smarttuner.kaffeeverde.lifecycle.ui.BackDispatcherOwner
import net.smarttuner.kaffeeverde.lifecycle.ui.LocalBackDispatcherOwner
import net.smarttuner.kaffeeverde.lifecycle.ui.LocalViewModelStoreOwner

@Composable
fun KVComposeWindow(
    title: String = "JetpackDesktopWindow",
    state: WindowState,
    onCloseRequest: (() -> Unit),
    lifecycleHolder: LifecycleOwner,
    content: @Composable () -> Unit = { }
) {
    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        title = title
    ) {
        ProvideDesktopCompositionLocals(lifecycleHolder) {
            content.invoke()
        }
    }
}

@Composable
private fun ProvideDesktopCompositionLocals(
    lifecycleHolder: LifecycleOwner,
    content: @Composable () -> Unit,
) {
    val holder = remember {
        KVWindowHolder()
    }

    CompositionLocalProvider(
        LocalLifecycleOwner provides lifecycleHolder,
        LocalViewModelStoreOwner provides holder,
        LocalBackDispatcherOwner provides holder,
    ) {
        content.invoke()
    }
}

class KVLifecycleWindowHolder : LifecycleOwner {
    override val lifecycle by lazy {
        LifecycleRegistry(this)
    }
}


private class KVWindowHolder : ViewModelStoreOwner, BackDispatcherOwner {
    override val viewModelStore by lazy {
        ViewModelStore()
    }
    override val backDispatcher by lazy {
        BackDispatcher()
    }
}
