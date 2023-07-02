/*
 * Copyright 2019 The Android Open Source Project
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
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/savedstate/savedstate/src/main/java/androidx/savedstate/Recreator.kt
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.lifecycle

import net.smarttuner.kaffeeverde.core.Bundle
import net.smarttuner.kaffeeverde.core.putStringArrayList

internal class Recreator(
    private val owner: SavedStateRegistryOwner
) : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event != Lifecycle.Event.ON_CREATE) {
            throw AssertionError("Next event must be ON_CREATE")
        }
        source.platformLifecycle.removeObserver(this)
        // The following code is commented because Kotlin/Native does not support full-reflection
//        val bundle: Bundle = owner.savedStateRegistry
//            .consumeRestoredStateForKey(COMPONENT_KEY) ?: return
//        val classes: MutableList<String> = bundle.getStringArrayList(CLASSES_KEY)
//            ?: throw IllegalStateException(
//                "Bundle with restored state for the component " +
//                        "\"$COMPONENT_KEY\" must contain list of strings by the key " +
//                        "\"$CLASSES_KEY\""
//            )
//        for (className: String in classes) {
//            reflectiveNew(className)
//        }
    }
//    private fun reflectiveNew(className: String) {
//        val clazz: KClass<out SavedStateRegistry.AutoRecreated> =
//            try {
//                KClass.forName(className, false, Recreator::class)
//                    .asSubclass(SavedStateRegistry.AutoRecreated::class)
//            } catch (e: ClassNotFoundException) {
//                throw RuntimeException("Class $className wasn't found", e)
//            }
//        val constructor =
//            try {
//                clazz.getDeclaredConstructor()
//            } catch (e: NoSuchMethodException) {
//                throw IllegalStateException(
//                    "Class ${clazz.simpleName} must have " +
//                            "default constructor in order to be automatically recreated", e
//                )
//            }
//        constructor.isAccessible = true
//        val newInstance: AutoRecreated =
//            try {
//                constructor.newInstance()
//            } catch (e: Exception) {
//                throw RuntimeException("Failed to instantiate $className", e)
//            }
//        newInstance.onRecreated(owner)
//    }
    internal class SavedStateProvider(registry: SavedStateRegistry) :
    SavedStateRegistry.SavedStateProvider {
        private val classes: MutableSet<String> = mutableSetOf()
        init {
            registry.registerSavedStateProvider(COMPONENT_KEY, this)
        }
        override fun saveState(): Bundle {
            return Bundle().apply {
                putStringArrayList(CLASSES_KEY, ArrayList(classes))
            }
        }
        fun add(className: String) {
            classes.add(className)
        }
    }
    companion object {
        const val CLASSES_KEY = "classes_to_restore"
        const val COMPONENT_KEY = "androidx.savedstate.Restarter"
    }
}