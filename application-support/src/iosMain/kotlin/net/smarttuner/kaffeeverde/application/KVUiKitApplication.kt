package net.smarttuner.kaffeeverde.application

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Application
import net.smarttuner.kaffeeverde.compose.ui.platform.LocalLifecycleOwner
import net.smarttuner.kaffeeverde.lifecycle.LifecycleOwner
import net.smarttuner.kaffeeverde.lifecycle.LifecycleRegistry
import net.smarttuner.kaffeeverde.lifecycle.ViewModelStore
import net.smarttuner.kaffeeverde.lifecycle.ViewModelStoreOwner
import net.smarttuner.kaffeeverde.lifecycle.ui.*
import platform.UIKit.UIViewController

fun KVUiKitApplication(
    title: String,
    lifecycleHolder: KVLifecycleHolder,
    content: @Composable (lifecycleHolder: KVLifecycleHolder) -> Unit
): UIViewController {
    return Application(
        title
    ) {
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
