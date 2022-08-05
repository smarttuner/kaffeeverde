/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/arch/core/core-common/src/main/java/androidx/arch/core/internal/SafeIterableMap.java
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.core

/**
 * LinkedList, which pretends to be a map and supports modifications during iterations.
 * It is NOT thread safe.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @hide
</V></K> */

open class SafeIterableMap<K, V> : Iterable<Map.Entry<K, V>?> {
    private var mStart: Entry<K, V>? = null
    private var mEnd: Entry<K, V>? = null

    // using WeakHashMap over List<WeakReference>, so we don't have to manually remove
    // WeakReferences that have null in them.
    private val mIterators: HashMap<SupportRemove<K, V>, Boolean> =
        HashMap<SupportRemove<K, V>, Boolean>()
    private var mSize = 0
    protected open operator fun get(k: K): Entry<K, V>? {
        var currentNode = mStart
        while (currentNode != null) {
            if (currentNode.key == k) {
                break
            }
            currentNode = currentNode.mNext
        }
        return currentNode
    }

    /**
     * If the specified key is not already associated
     * with a value, associates it with the given value.
     *
     * @param key key with which the specified value is to be associated
     * @param v   value to be associated with the specified key
     * @return the previous value associated with the specified key,
     * or `null` if there was no mapping for the key
     */
    open fun putIfAbsent(key: K, v: V): V? {
        val entry = get(key)
        if (entry != null) {
            return entry.value
        }
        put(key, v)
        return null
    }

    protected fun put(key: K, v: V): Entry<K, V> {
        val newEntry = Entry(key, v)
        mSize++
        if (mEnd == null) {
            mStart = newEntry
            mEnd = mStart
            return newEntry
        }
        mEnd!!.mNext = newEntry
        newEntry.mPrevious = mEnd
        mEnd = newEntry
        return newEntry
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with the specified key,
     * or `null` if there was no mapping for the key
     */
    open fun remove(key: K): V? {
        val toRemove = get(key) ?: return null
        mSize--
        if (!mIterators.isEmpty()) {
            for (iter in mIterators.keys) {
                iter.supportRemove(toRemove)
            }
        }
        if (toRemove.mPrevious != null) {
            toRemove.mPrevious!!.mNext = toRemove.mNext
        } else {
            mStart = toRemove.mNext
        }
        if (toRemove.mNext != null) {
            toRemove.mNext!!.mPrevious = toRemove.mPrevious
        } else {
            mEnd = toRemove.mPrevious
        }
        toRemove.mNext = null
        toRemove.mPrevious = null
        return toRemove.value
    }

    /**
     * @return the number of elements in this map
     */
    fun size(): Int {
        return mSize
    }

    /**
     * @return an ascending iterator, which doesn't include new elements added during an
     * iteration.
     */
    
    override fun iterator(): MutableIterator<Map.Entry<K, V>?> {
        val iterator: ListIterator<K, V> = AscendingIterator(mStart, mEnd)
        mIterators[iterator] = false
        return iterator
    }

    /**
     * @return an descending iterator, which doesn't include new elements added during an
     * iteration.
     */
    fun descendingIterator(): Iterator<Map.Entry<K, V>?> {
        val iterator = DescendingIterator(mEnd, mStart)
        mIterators[iterator] = false
        return iterator
    }

    /**
     * return an iterator with additions.
     */
    fun iteratorWithAdditions(): IteratorWithAdditions {
        val iterator = IteratorWithAdditions()
        mIterators[iterator] = false
        return iterator
    }

    /**
     * @return eldest added entry or null
     */
    fun eldest(): Map.Entry<K, V>? {
        return mStart
    }

    /**
     * @return newest added entry or null
     */
    fun newest(): Map.Entry<K, V>? {
        return mEnd
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }
        if (obj !is SafeIterableMap<*, *>) {
            return false
        }
        val map = obj
        if (size() != map.size()) {
            return false
        }
        val iterator1: Iterator<Map.Entry<K, V>?> = iterator()
        val iterator2: Iterator<*> = map.iterator()
        while (iterator1.hasNext() && iterator2.hasNext()) {
            val next1 = iterator1.next()
            val next2 = iterator2.next()
            if ((next1 == null && next2 != null || next1 != null) && next1 != next2) {
                return false
            }
        }
        return !iterator1.hasNext() && !iterator2.hasNext()
    }

    override fun toString(): String {
        val builder: StringBuilder = StringBuilder()
        builder.append("[")
        val iterator: Iterator<Map.Entry<K, V>?> = iterator()
        while (iterator.hasNext()) {
            builder.append(iterator.next().toString())
            if (iterator.hasNext()) {
                builder.append(", ")
            }
        }
        builder.append("]")
        return builder.toString()
    }

    internal abstract class ListIterator<K, V> constructor(
        var mNext: Entry<K, V>?,
        var mExpectedEnd: Entry<K, V>?
    ) : MutableIterator<Map.Entry<K, V>?>, SupportRemove<K, V> {
        override fun hasNext(): Boolean {
            return mNext != null
        }

        override fun supportRemove(entry: Entry<K, V>?) {
            if (mExpectedEnd === entry && entry === mNext) {
                mNext = null
                mExpectedEnd = null
            }
            if (mExpectedEnd === entry) {
                mExpectedEnd = backward(mExpectedEnd)
            }
            if (mNext === entry) {
                mNext = nextNode()
            }
        }

        private fun nextNode(): Entry<K, V>? {
            return if (mNext === mExpectedEnd || mExpectedEnd == null) {
                null
            } else forward(mNext)
        }

        override fun next(): Map.Entry<K, V>? {
            val result: Map.Entry<K, V>? = mNext
            mNext = nextNode()
            return result
        }

        abstract fun forward(entry: Entry<K, V>?): Entry<K, V>?
        abstract fun backward(entry: Entry<K, V>?): Entry<K, V>?
    }

    internal class AscendingIterator<K, V>(start: Entry<K, V>?, expectedEnd: Entry<K, V>?) :
        ListIterator<K, V>(
            start!!, expectedEnd
        ) {
        override fun forward(entry: Entry<K, V>?): Entry<K, V>? {
            return entry?.mNext
        }

        override fun backward(entry: Entry<K, V>?): Entry<K, V>? {
            return entry?.mPrevious
        }

        override fun remove() {
            // TODO: not implemented
        }
    }

    private class DescendingIterator<K, V> internal constructor(
        start: Entry<K, V>?,
        expectedEnd: Entry<K, V>?
    ) : ListIterator<K, V>(
        start!!, expectedEnd
    ) {
        override fun forward(entry: Entry<K, V>?): Entry<K, V>? {
            return entry?.mPrevious
        }

        override fun backward(entry: Entry<K, V>?): Entry<K, V>? {
            return entry?.mNext
        }

        override fun remove() {
            // TODO: not implemented
        }
    }

    inner class IteratorWithAdditions : MutableIterator<Map.Entry<K, V>?>, SupportRemove<K, V> {
        private var mCurrent: Entry<K, V>? = null
        private var mBeforeStart = true
        override fun supportRemove(entry: Entry<K, V>?) {
            if (entry === mCurrent) {
                mCurrent = mCurrent!!.mPrevious
                mBeforeStart = mCurrent == null
            }
        }

        override fun hasNext(): Boolean {
            return if (mBeforeStart) {
                mStart != null
            } else mCurrent != null && mCurrent!!.mNext != null
        }

        override fun next(): Map.Entry<K, V> {
            if (mBeforeStart) {
                mBeforeStart = false
                mCurrent = mStart
            } else {
                mCurrent = if (mCurrent != null) mCurrent!!.mNext else null
            }
            return mCurrent!!
        }

        override fun remove() {
            // TODO: not implemented
        }
    }

    internal interface SupportRemove<K, V> {
        fun supportRemove(entry: Entry<K, V>?)
    }

    class Entry<K, V>(
       override val key: K, override val value: V
    ) : MutableMap.MutableEntry<K, V> {

        var mNext: Entry<K, V>? = null
        var mPrevious: Entry<K, V>? = null
        override fun setValue(value: V): V {
            throw UnsupportedOperationException("An entry modification is not supported")
        }

        override fun toString(): String {
            return key.toString() + "=" + value
        }

        override fun equals(obj: Any?): Boolean {
            if (obj === this) {
                return true
            }
            if (obj !is Entry<*, *>) {
                return false
            }
            val (key1, value1) = obj
            return key == key1 && value == value1
        }
    }
}