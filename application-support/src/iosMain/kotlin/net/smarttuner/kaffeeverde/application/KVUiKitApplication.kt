package net.smarttuner.kaffeeverde.application

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import net.smarttuner.kaffeeverde.compose.ui.platform.LocalLifecycleOwner
import net.smarttuner.kaffeeverde.lifecycle.ui.BackDispatcher
import net.smarttuner.kaffeeverde.lifecycle.ui.BackDispatcherOwner
import net.smarttuner.kaffeeverde.lifecycle.ui.LocalBackDispatcherOwner
import net.smarttuner.kaffeeverde.lifecycle.ui.LocalViewModelStoreOwner
import platform.UIKit.UIViewController

fun KVUiKitApplication(
    title: String,
    lifecycleHolder: KVLifecycleHolder,
    content: @Composable (lifecycleHolder: KVLifecycleHolder) -> Unit
): UIViewController {
    return ComposeUIViewController {
        val holder = remember {
            PreComposeWindowHolder()
        }
        ProvideDesktopCompositionLocals(
            lifecycleHolder,
            holder
        ) {
            content.invoke(lifecycleHolder)
        }
    }
}

@Composable
private fun ProvideDesktopCompositionLocals(
    lifecycleHolder: LifecycleOwner,
    holder: PreComposeWindowHolder = remember {
        PreComposeWindowHolder()
    },
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLifecycleOwner provides lifecycleHolder,
        LocalViewModelStoreOwner provides holder,
        LocalBackDispatcherOwner provides holder,
    ) {
        content.invoke()
    }
}

class KVLifecycleHolder : LifecycleOwner {
    override val lifecycle by lazy {
        LifecycleRegistry(this)
    }
}

private class PreComposeWindowHolder : ViewModelStoreOwner, BackDispatcherOwner {
    override val viewModelStore by lazy {
        ViewModelStore()
    }
    override val backDispatcher by lazy {
        BackDispatcher()
    }
}
