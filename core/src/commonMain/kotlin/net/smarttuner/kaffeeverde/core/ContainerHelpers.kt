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
 * https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-arch-core-release/collection/collection/src/main/java/androidx/collection/ContainerHelpers.java
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.core

internal object ContainerHelpers {
    val EMPTY_INTS = IntArray(0)
    val EMPTY_LONGS = LongArray(0)
    val EMPTY_OBJECTS = arrayOfNulls<Any>(0)
    fun idealIntArraySize(need: Int): Int {
        return idealByteArraySize(need * 4) / 4
    }

    fun idealLongArraySize(need: Int): Int {
        return idealByteArraySize(need * 8) / 8
    }

    fun idealByteArraySize(need: Int): Int {
        for (i in 4..31) if (need <= (1 shl i) - 12) return (1 shl i) - 12
        return need
    }

    fun equal(a: Any?, b: Any): Boolean {
        return a === b || a != null && a == b
    }

    // This is Arrays.binarySearch(), but doesn't do any argument validation.
    fun binarySearch(array: IntArray, size: Int, value: Int): Int {
        var lo = 0
        var hi = size - 1
        while (lo <= hi) {
            val mid = lo + hi ushr 1
            val midVal = array[mid]
            if (midVal < value) {
                lo = mid + 1
            } else if (midVal > value) {
                hi = mid - 1
            } else {
                return mid // value found
            }
        }
        return lo.inv() // value not present
    }

    fun binarySearch(array: LongArray, size: Int, value: Long): Int {
        var lo = 0
        var hi = size - 1
        while (lo <= hi) {
            val mid = lo + hi ushr 1
            val midVal = array[mid]
            if (midVal < value) {
                lo = mid + 1
            } else if (midVal > value) {
                hi = mid - 1
            } else {
                return mid // value found
            }
        }
        return lo.inv() // value not present
    }
}