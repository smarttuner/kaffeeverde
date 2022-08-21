package net.smarttuner.kaffeeverde.compose.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf
import net.smarttuner.kaffeeverde.lifecycle.LifecycleOwner

/**
 * The CompositionLocal containing the current [LifecycleOwner].
 */
val LocalLifecycleOwner = staticCompositionLocalOf<LifecycleOwner> {
    noLocalProvidedFor("LocalLifecycleOwner")
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}