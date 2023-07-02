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
 * https://github.com/Tlaster/PreCompose/blob/master/precompose/src/androidMain/kotlin/moe/tlaster/precompose/lifecycle/PreComposeActivity.kt
 *
 * The content of this file is a port of the original work with some additions
 *
 *
 */
package net.smarttuner.kaffeeverde.application

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import net.smarttuner.kaffeeverde.compose.ui.platform.LocalLifecycleOwner
import net.smarttuner.kaffeeverde.compose.ui.platform.LocalSavedStateRegistryOwner
import net.smarttuner.kaffeeverde.lifecycle.*
import net.smarttuner.kaffeeverde.lifecycle.ui.*


open class KVActivity :
    ComponentActivity(),
    LifecycleOwner,
    ViewModelStoreOwner,
    BackDispatcherOwner,
    SavedStateRegistryOwner{

    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)
    override val platformSavedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    override val platformLifecycle by lazy {
        LifecycleRegistry(this)
    }

    override val platformViewModelStore by lazy {
        ViewModelStore()
    }

    override fun onResume() {
        super.onResume()
        platformLifecycle.currentState = Lifecycle.State.RESUMED
    }

    override fun onPause() {
        platformLifecycle.currentState = Lifecycle.State.STARTED
        super.onPause()
    }

    override fun onDestroy() {
        platformLifecycle.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override val backDispatcher by lazy {
        BackDispatcher()
    }

    override fun onBackPressed() {
        if (!backDispatcher.onBackPress()) {
            super.onBackPressed()
        }
    }
}

fun KVActivity.setContent(
    parent: CompositionContext? = null,
    content: @Composable () -> Unit
) {
    val existingComposeView = window.decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ComposeView

    if (existingComposeView != null) with(existingComposeView) {
        setParentCompositionContext(parent)
        setContent {
            ContentInternal(content)
        }
    } else ComposeView(this).apply {
        // Set content and parent **before** setContentView
        // to have ComposeView create the composition on attach
        setParentCompositionContext(parent)
        setContent {
            ContentInternal(content)
        }
        // Set the view tree owners before setting the content view so that the inflation process
        // and attach listeners will see them already present
        setOwners()
        setContentView(this, DefaultActivityContentLayoutParams)
    }
}

private fun KVActivity.setOwners() {
    val decorView = window.decorView
//    if (ViewTreeLifecycleOwner.get(decorView) == null) {
//        ViewTreeLifecycleOwner.set(decorView, this)
//    }
    if (decorView.findViewTreeSavedStateRegistryOwner() == null) {
        decorView.setViewTreeSavedStateRegistryOwner(this)
    }
}

@Composable
private fun KVActivity.ContentInternal(
    content: @Composable () -> Unit
) {
    ProvideAndroidCompositionLocals() {
        content.invoke()
    }
}

@Composable
private fun KVActivity.ProvideAndroidCompositionLocals(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLifecycleOwner provides this,
        LocalViewModelStoreOwner provides this,
        LocalBackDispatcherOwner provides this,
        LocalSavedStateRegistryOwner provides this,
    ) {
        content.invoke()
    }
}

private val DefaultActivityContentLayoutParams = ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.WRAP_CONTENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
)

