package net.smarttuner.kaffeeverde.compose.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf
import net.smarttuner.kaffeeverde.lifecycle.LifecycleOwner
import net.smarttuner.kaffeeverde.lifecycle.SavedStateRegistryOwner

/**
 * The CompositionLocal containing the current [LifecycleOwner].
 */
val LocalLifecycleOwner = staticCompositionLocalOf<LifecycleOwner> {
    noLocalProvidedFor("LocalLifecycleOwner")
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}

/**
 * The CompositionLocal containing the current [SavedStateRegistryOwner].
 */
val LocalSavedStateRegistryOwner = staticCompositionLocalOf<SavedStateRegistryOwner> {
    noLocalProvidedFor("LocalSavedStateRegistryOwner")
}