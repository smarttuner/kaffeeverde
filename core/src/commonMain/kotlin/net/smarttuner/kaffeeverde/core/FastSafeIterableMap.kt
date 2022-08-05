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
/**
 *
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/arch/core/core-common/src/main/java/androidx/arch/core/internal/FastSafeIterableMap.java
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.core


/**
 * Poor's man LinkedHashMap, which supports modifications during iterations.
 * Takes more memory that [SafeIterableMap]
 * It is NOT thread safe.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @hide
</V></K> */

class FastSafeIterableMap<K, V> : SafeIterableMap<K, V>() {
    private val mHashMap: HashMap<K, Entry<K, V>> = HashMap<K, Entry<K, V>>()
    override fun get(k: K): Entry<K, V>? {
        return mHashMap.get(k)
    }

    override fun putIfAbsent(key: K, v: V): V? {
        val current = get(key)
        if (current != null) {
            return current.value
        }
        mHashMap.put(key, put(key, v))
        return null
    }

    override fun remove(key: K): V? {
        val removed = super.remove(key)
        mHashMap.remove(key)
        return removed
    }

    /**
     * Returns `true` if this map contains a mapping for the specified
     * key.
     */
    operator fun contains(key: K): Boolean {
        return mHashMap.containsKey(key)
    }

    /**
     * Return an entry added to prior to an entry associated with the given key.
     *
     * @param k the key
     */
    fun ceil(k: K): Map.Entry<K, V>? {
        return if (contains(k)) {
            mHashMap[k]?.mPrevious
        } else null
    }
}