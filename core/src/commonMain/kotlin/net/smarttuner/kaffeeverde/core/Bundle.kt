/*
 * Copyright (C) 2007 The Android Open Source Project
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
 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/os/Bundle.java
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.core

import kotlin.jvm.Synchronized

/**
 * A mapping from String keys to various [Parcelable] values.
 *
 *
 * **Warning:** Note that [Bundle] is a lazy container and as such it does NOT implement
 * [.equals] or [.hashCode].
 *
 * @see PersistableBundle
 */
class Bundle {

    private val mParcelledData = mutableMapOf<String?,Any?>()

    operator fun get(key: String): Any? = mParcelledData[key]
    operator fun set(key: String, value: Any?) { mParcelledData[key] = value }

    /**
     * Constructs a new, empty Bundle.
     */
    constructor()

    val isEmpty get() = mParcelledData.isEmpty()
    val keySet get() = mParcelledData.keys
    val size get() = mParcelledData.size


    /**
     * Constructs a Bundle containing a copy of the mappings from the given
     * Bundle.  Does only a shallow copy of the original Bundle -- see
     * [.deepCopy] if that is not what you want.
     *
     * @param b a Bundle to be copied.
     *
     * @see .deepCopy
     */
    constructor(b: Bundle) {
        mParcelledData.putAll(b.mParcelledData)
    }



    /**
     * Clones the current Bundle. The internal map is cloned, but the keys and
     * values to which it refers are copied by reference.
     */
    fun clone(): Bundle {
        return Bundle(this)
    }
    

    /**
     * Removes all elements from the mapping of this Bundle.
     */
    fun clear() {
        mParcelledData.clear()
    }

    fun containsKey(key: String): Boolean{
        return mParcelledData.containsKey(key)
    }

    /**
     * Removes any entry with the given key from the mapping of this Bundle.
     *
     * @param key a String key
     */
    fun remove(key: String?) {
        mParcelledData.remove(key)
    }

    /**
     * Inserts all mappings from the given Bundle into this Bundle.
     *
     * @param bundle a Bundle
     */
    fun putAll(bundle: Bundle?) {
        if(bundle==null) return
        mParcelledData.putAll(bundle.mParcelledData)
    }



    /** {@hide}  */
    fun putObject(key: String?, value: Any?) {
        when (value) {
            is Byte -> putByte(key, value)
            is Char -> putChar(key, value)
            is Short -> putShort(key, value)
            is Float -> putFloat(key, value)
            is Int -> putInt(key, value)
            is Boolean -> putBoolean(key, value)
            is CharSequence -> putCharSequence(key, value as CharSequence?)
            is ByteArray -> putByteArray(key, value as ByteArray?)
            is ShortArray -> putShortArray(key, value as ShortArray?)
            is CharArray -> putCharArray(key, value as CharArray?)
            is FloatArray -> putFloatArray(key, value as FloatArray?)
            is BooleanArray -> putBooleanArray(key, value as BooleanArray?)
            else -> mParcelledData[key] = value
        }
    }

    /**
     * Inserts a byte value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a byte
     */
    fun putByte(key: String?, value: Byte) {
        mParcelledData.put(key, value)
    }

    /**
     * Inserts a char value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a char
     */
    fun putChar(key: String?, value: Char) {
        mParcelledData.put(key, value)
    }

    /**
     * Inserts a short value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a short
     */
    fun putShort(key: String?, value: Short) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a float value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a float
     */
    fun putFloat(key: String?, value: Float) {
        mParcelledData[key] = value
    }
    /**
     * Inserts a double value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a double
     */
    fun putDouble(key: String?, value: Double) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a long value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a long
     */
    fun putLong(key: String?, value: Long) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a Boolean value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a Boolean
     */
    fun putBoolean(key: String?, value: Boolean) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a Int value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a Int
     */
    fun putInt(key: String?, value: Int) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a String value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a String
     */
    fun putString(key: String?, value: String?) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a CharSequence value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a CharSequence, or null
     */
    fun putCharSequence(key: String?, value: CharSequence?) {
        mParcelledData[key] = value
    }

    /**
     * Inserts an Array<String> value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an Array<String> object, or null
    </String></String> */
    fun putStringArray(key: String, value: Array<String>?) {
        mParcelledData[key] = value
    }


    fun getStringArrayList(key: String): ArrayList<String>? = mParcelledData[key] as? ArrayList<String>?

    /**
     * Inserts an Array<String> value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an Array<Any> object, or null
    </String></String> */
    fun putAnyArray(key: String, value: Array<Any?>?) {
        mParcelledData[key] = value
    }


    fun getAnyArrayList(key: String): ArrayList<Any>? = mParcelledData[key] as? ArrayList<Any>?


    /**
     * Inserts an ArrayList<String> value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an ArrayList<String> object, or null
    </String></String> */
    fun putStringArrayList(key: String, value: ArrayList<String>?) {
        mParcelledData[key] = value
    }

    /**
     * Inserts an ArrayList<CharSequence> value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an ArrayList<CharSequence> object, or null
    </CharSequence></CharSequence> */
    fun putCharSequenceArrayList(
        key: String,
        value: ArrayList<CharSequence?>?
    ) {
        mParcelledData.put(key, value)
    }


    /**
     * Inserts a byte array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a byte array object, or null
     */
    fun putByteArray(key: String?, value: ByteArray?) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a short array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a short array object, or null
     */
    fun putShortArray(key: String?, value: ShortArray?) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a char array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a char array object, or null
     */
    fun putCharArray(key: String?, value: CharArray?) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a float array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a float array object, or null
     */
    fun putFloatArray(key: String?, value: FloatArray?) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a double array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a double array object, or null
     */
    fun putDoubleArray(key: String?, value: DoubleArray?) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a long array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a long array object, or null
     */
    fun putLongArray(key: String?, value: LongArray?) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a integer array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a integer array object, or null
     */
    fun putIntArray(key: String?, value: IntArray?) {
        mParcelledData[key] = value
    }

    /**
     * Inserts a boolean array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a boolean array object, or null
     */
    fun putBooleanArray(key: String?, value: BooleanArray?) {
        mParcelledData[key] = value
    }


    /**
     * Inserts a Bundle value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a Bundle object, or null
     */
    fun putBundle(key: String?, value: Bundle?) {
        mParcelledData[key] = value
    }

    fun getBundle(key: String?): Bundle? {
        return mParcelledData[key] as Bundle?
    }

    fun getByte(key: String?): Byte {
        return mParcelledData[key] as Byte
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a byte value
     */
    fun getByte(key: String?, defaultValue: Byte): Byte {
        return mParcelledData.get(key) as? Byte ?: defaultValue
    }

    /**
     * Returns the value associated with the given key, or (char) 0 if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a char value
     */
    fun getChar(key: String?): Char {
        return mParcelledData[key] as Char 
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a char value
     */
    fun getChar(key: String?, defaultValue: Char): Char {
        return mParcelledData[key] as? Char ?: defaultValue 
    }

    /**
     * Returns the value associated with the given key, or (short) 0 if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a Int value
     */
    fun getInt(key: String?): Int {
        return mParcelledData[key] as Int
    }

    /**
     * Returns the value associated with the given key, or (short) 0 if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a String value
     */
    fun getString(key: String?): String {
        return mParcelledData[key] as String
    }

    /**
     * Returns the value associated with the given key, or (short) 0 if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a Boolean value
     */
    fun getBoolean(key: String?): Boolean {
        return mParcelledData[key] as Boolean
    }


    /**
     * Returns the value associated with the given key, or (short) 0 if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a short value
     */
    fun getShort(key: String?): Short {
        return mParcelledData[key] as Short
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a short value
     */
    fun getShort(key: String?, defaultValue: Short): Short {
        return mParcelledData[key] as? Short ?: defaultValue 
    }

    /**
     * Returns the value associated with the given key, or 0.0f if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a float value
     */
    fun getFloat(key: String?): Float {
        return mParcelledData[key] as Float 
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a float value
     */
    fun getFloat(key: String?, defaultValue: Float): Float {
        return mParcelledData[key] as? Float ?: defaultValue 
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a CharSequence value, or null
     */
        fun getCharSequence(key: String?): CharSequence {
        return mParcelledData[key] as CharSequence 
    }


    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a byte[] value, or null
     */
    fun getByteArray(key: String?): ByteArray {
        return mParcelledData[key] as ByteArray 
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a IntArray value, or null
     */
    fun getIntArray(key: String?): IntArray {
        return mParcelledData[key] as IntArray
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a short[] value, or null
     */
        fun getShortArray(key: String?): ShortArray {
        return mParcelledData[key] as ShortArray 
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a char[] value, or null
     */
        fun getCharArray(key: String?): CharArray {
        return mParcelledData[key] as CharArray 
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a float[] value, or null
     */
        fun getFloatArray(key: String?): FloatArray {
        return mParcelledData[key] as FloatArray 
    }



     /**
     * Returns a string representation of the [Bundle] that may be suitable for debugging. It
     * won't print the internal map if its content hasn't been unparcelled.
     */
    @Synchronized
    override fun toString(): String {
        return "Bundle[mParcelledData.size=" +
                        mParcelledData.size + "]"
    }

    /**
     * @hide
     */
    @Synchronized
    fun toShortString(): String {
        return "mParcelledData.size=" + mParcelledData.size
    }

}