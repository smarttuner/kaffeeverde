/*
 * Copyright 2018 The Android Open Source Project
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
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/lifecycle/lifecycle-viewmodel-savedstate/src/main/java/androidx/lifecycle/SavedStateHandle.kt
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.lifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.smarttuner.kaffeeverde.core.Bundle
import net.smarttuner.kaffeeverde.core.bundleOf

/**
 * A handle to saved state passed down to [androidx.lifecycle.ViewModel]. You should use
 * [SavedStateViewModelFactory] if you want to receive this object in `ViewModel`'s
 * constructor.
 *
 * This is a key-value map that will let you write and retrieve objects to and from the saved state.
 * These values will persist after the process is killed by the system
 * and remain available via the same object.
 *
 * You can read a value from it via [get] or observe it via [androidx.lifecycle.LiveData] returned
 * by [getLiveData].
 *
 * You can write a value to it via [set] or setting a value to
 * [androidx.lifecycle.MutableLiveData] returned by [getLiveData].
 */
class SavedStateHandle {
    private val regular = mutableMapOf<String, Any?>()
    private val savedStateProviders = mutableMapOf<String, SavedStateRegistry.SavedStateProvider>()
    private val flows = mutableMapOf<String, MutableStateFlow<Any?>>()
    private val savedStateProvider =
        SavedStateRegistry.SavedStateProvider {
            // Get the saved state from each SavedStateProvider registered with this
            // SavedStateHandle, iterating through a copy to avoid re-entrance
            // Convert the Map of current values into a Bundle
            val keySet: Set<String> = regular.keys
            val keys: ArrayList<String> = ArrayList(keySet.size)
            val value: ArrayList<Any?> = ArrayList(keys.size)
            for (key in keySet) {
                keys.add(key)
                value.add(regular[key])
            }
            bundleOf(KEYS to keys, VALUES to value)
        }
    /**
     * Creates a handle with the given initial arguments.
     *
     * @param initialState initial arguments for the SavedStateHandle
     */
    constructor(initialState: Map<String, Any?>) {
        regular.putAll(initialState)
    }
    /**
     * Creates a handle with the empty state.
     */
    constructor()
    
    fun savedStateProvider(): SavedStateRegistry.SavedStateProvider {
        return savedStateProvider
    }
    /**
     * @param key          The identifier for the value
     *
     * @return true if there is value associated with the given key.
     */
    
    operator fun contains(key: String): Boolean {
        return regular.containsKey(key)
    }

    /**
     * Returns a [StateFlow] that will emit the currently active value associated with the given
     * key.
     *
     * ```
     * val flow = savedStateHandle.getStateFlow(KEY, "defaultValue")
     * ```
     * Since this is a [StateFlow] there will always be a value available which, is why an initial
     * value must be provided. The value of this flow is changed by making a call to [set], passing
     * in the key that references this flow.
     *
     * If there is already a value associated with the given key, the initial value will be ignored.
     *
     * @param key The identifier for the flow
     * @param initialValue If no value exists with the given `key`, a new one is created
     * with the given `initialValue`.
     */
    
    fun <T> getStateFlow(key: String, initialValue: T): StateFlow<T> {
        @Suppress("UNCHECKED_CAST")
        // If a flow exists we should just return it, and since it is a StateFlow and a value must
        // always be set, we know a value must already be available
        return flows.getOrPut(key) {
            // If there is not a value associated with the key, add the initial value, otherwise,
            // use the one we already have.
            if (!regular.containsKey(key)) {
                regular[key] = initialValue
            }
            MutableStateFlow(regular[key]).apply { flows[key] = this }
        }.asStateFlow() as StateFlow<T>
    }

    /**
     * Set a [SavedStateProvider] that will have its state saved into this SavedStateHandle.
     * This provides a mechanism to lazily provide the [Bundle] of saved state for the given key.
     *
     * Calls to [get] with this same key will return the previously saved state as a [Bundle] if it
     * exists.
     *
     * ```
     * Bundle previousState = savedStateHandle.get("custom_object");
     * if (previousState != null) {
     *     // Convert the previousState into your custom object
     * }
     * savedStateHandle.setSavedStateProvider("custom_object", () -> {
     *     Bundle savedState = new Bundle();
     *     // Put your custom object into the Bundle, doing any conversion required
     *     return savedState;
     * });
     * ```
     *
     * Note: calling this method within [SavedStateProvider.saveState] is supported, but
     * will only affect future state saving operations.
     *
     * @param key a key which will populated with a [Bundle] produced by the provider
     * @param provider a SavedStateProvider which will receive a callback to
     * [SavedStateProvider.saveState] when the state should be saved
     */
    
    fun setSavedStateProvider(key: String, provider: SavedStateRegistry.SavedStateProvider) {
        savedStateProviders[key] = provider
    }
    /**
     * Clear any [SavedStateProvider] that was previously set via
     * [setSavedStateProvider].
     *
     * Note: calling this method within [SavedStateProvider.saveState] is supported, but
     * will only affect future state saving operations.
     *
     * @param key a key previously used with [setSavedStateProvider]
     */
    
    fun clearSavedStateProvider(key: String) {
        savedStateProviders.remove(key)
    }

    companion object {
        private const val VALUES = "values"
        private const val KEYS = "keys"
        
        
        @Suppress("DEPRECATION")
        fun createHandle(restoredState: Bundle?, defaultState: Bundle?): SavedStateHandle {
            if (restoredState == null) {
                return if (defaultState == null) {
                    // No restored state and no default state -> empty SavedStateHandle
                    SavedStateHandle()
                } else {
                    val state: MutableMap<String, Any?> = HashMap()
                    defaultState.let {
                        for (key in it.keySet) {
                            if(key==null) continue
                            state[key] = defaultState[key]
                        }
                    }
                    SavedStateHandle(state)
                }
            }
            // When restoring state, we use the restored state as the source of truth
            // and ignore any default state, thus ensuring we are exactly the same
            // state that was saved.
            val keys: ArrayList<*>? = restoredState.getStringArrayList(KEYS)
            val values: ArrayList<*>? = restoredState.getStringArrayList(VALUES)
            check(!(keys == null || values == null || keys.size != values.size)) {
                "Invalid bundle passed as restored state"
            }
            val state = mutableMapOf<String, Any?>()
            for (i in keys.indices) {
                state[keys[i] as String] = values[i]
            }
            return SavedStateHandle(state)
        }
        /**
         * @suppress
         */
        
        fun validateValue(value: Any?): Boolean {
            if (value == null) {
                return true
            }
            for (cl in ACCEPTABLE_CLASSES) {
                if (cl.isInstance(value)) {
                    return true
                }
            }
            return false
        }
        // doesn't have Integer, Long etc box types because they are "Serializable"
        private val ACCEPTABLE_CLASSES = arrayOf( // baseBundle
            Boolean::class,
            BooleanArray::class,
            Double::class,
            DoubleArray::class,
            Int::class,
            IntArray::class,
            Long::class,
            LongArray::class,
            String::class,
            Array::class, // bundle
            Bundle::class,
            Byte::class,
            ByteArray::class,
            Char::class,
            CharArray::class,
            CharSequence::class,
            ArrayList::class,
            Float::class,
            FloatArray::class,
            Short::class,
            ShortArray::class,
            Int::class
        )
    }
}