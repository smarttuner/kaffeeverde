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
 * A mapping from String keys to various [Serializable] values.
 *
 *
 * **Warning:** Note that [Bundle] is a lazy container and as such it does NOT implement
 * [.equals] or [.hashCode].
 *
 * @see PersistableBundle
 */

typealias Bundle = HashMap<String?,Any?>



    val Bundle.isEmpty get() = this.isEmpty()
    val Bundle.keySet get() = this.keys



    /**
     * Clones the current Bundle. The internal map is cloned, but the keys and
     * values to which it refers are copied by reference.
     */
    fun Bundle.clone(): Bundle {
        return Bundle(this)
    }
    

    /**
     * Removes all elements from the mapping of this Bundle.
     */
    fun Bundle.clear() {
        this.clear()
    }

    fun Bundle.containsKey(key: String): Boolean{
        return this.containsKey(key)
    }

    /**
     * Removes any entry with the given key from the mapping of this Bundle.
     *
     * @param key a String key
     */
    fun Bundle.remove(key: String?) {
        this.remove(key)
    }

    /**
     * Inserts all mappings from the given Bundle into this Bundle.
     *
     * @param bundle a Bundle
     */
    fun Bundle.putAll(bundle: Bundle?) {
        if(bundle==null) return
        this.putAll(bundle)
    }



    /** {@hide}  */
    fun Bundle.putObject(key: String?, value: Any?) {
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
            else -> this[key] = value
        }
    }

    /**
     * Inserts a byte value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a byte
     */
    fun Bundle.putByte(key: String?, value: Byte) {
        this.put(key, value)
    }

    /**
     * Inserts a char value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a char
     */
    fun Bundle.putChar(key: String?, value: Char) {
        this.put(key, value)
    }

    /**
     * Inserts a short value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a short
     */
    fun Bundle.putShort(key: String?, value: Short) {
        this[key] = value
    }

    /**
     * Inserts a float value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a float
     */
    fun Bundle.putFloat(key: String?, value: Float) {
        this[key] = value
    }
    /**
     * Inserts a double value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a double
     */
    fun Bundle.putDouble(key: String?, value: Double) {
        this[key] = value
    }

    /**
     * Inserts a long value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a long
     */
    fun Bundle.putLong(key: String?, value: Long) {
        this[key] = value
    }

    /**
     * Inserts a Boolean value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a Boolean
     */
    fun Bundle.putBoolean(key: String?, value: Boolean) {
        this[key] = value
    }

    /**
     * Inserts a Int value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a Int
     */
    fun Bundle.putInt(key: String?, value: Int) {
        this[key] = value
    }

    /**
     * Inserts a String value into the mapping of this Bundle, replacing
     * any existing value for the given key.
     *
     * @param key a String, or null
     * @param value a String
     */
    fun Bundle.putString(key: String?, value: String?) {
        this[key] = value
    }

    /**
     * Inserts a CharSequence value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a CharSequence, or null
     */
    fun Bundle.putCharSequence(key: String?, value: CharSequence?) {
        this[key] = value
    }

    /**
     * Inserts an Array<String> value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an Array<String> object, or null
    </String></String> */
    fun Bundle.putStringArray(key: String, value: Array<String>?) {
        this[key] = value
    }


    fun Bundle.getStringArrayList(key: String): ArrayList<String>? = this[key] as? ArrayList<String>?

    /**
     * Inserts an Array<String> value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an Array<Any> object, or null
    </String></String> */
    fun Bundle.putAnyArray(key: String, value: Array<Any?>?) {
        this[key] = value
    }


    fun Bundle.getAnyArrayList(key: String): ArrayList<Any>? = this[key] as? ArrayList<Any>?


    /**
     * Inserts an ArrayList<String> value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an ArrayList<String> object, or null
    </String></String> */
    fun Bundle.putStringArrayList(key: String, value: ArrayList<String>?) {
        this[key] = value
    }

    /**
     * Inserts an ArrayList<CharSequence> value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an ArrayList<CharSequence> object, or null
    </CharSequence></CharSequence> */
    fun Bundle.putCharSequenceArrayList(
        key: String,
        value: ArrayList<CharSequence?>?
    ) {
        this.put(key, value)
    }


    /**
     * Inserts a byte array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a byte array object, or null
     */
    fun Bundle.putByteArray(key: String?, value: ByteArray?) {
        this[key] = value
    }

    /**
     * Inserts a short array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a short array object, or null
     */
    fun Bundle.putShortArray(key: String?, value: ShortArray?) {
        this[key] = value
    }

    /**
     * Inserts a char array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a char array object, or null
     */
    fun Bundle.putCharArray(key: String?, value: CharArray?) {
        this[key] = value
    }

    /**
     * Inserts a float array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a float array object, or null
     */
    fun Bundle.putFloatArray(key: String?, value: FloatArray?) {
        this[key] = value
    }

    /**
     * Inserts a double array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a double array object, or null
     */
    fun Bundle.putDoubleArray(key: String?, value: DoubleArray?) {
        this[key] = value
    }

    /**
     * Inserts a long array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a long array object, or null
     */
    fun Bundle.putLongArray(key: String?, value: LongArray?) {
        this[key] = value
    }

    /**
     * Inserts a integer array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a integer array object, or null
     */
    fun Bundle.putIntArray(key: String?, value: IntArray?) {
        this[key] = value
    }

    /**
     * Inserts a boolean array value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a boolean array object, or null
     */
    fun Bundle.putBooleanArray(key: String?, value: BooleanArray?) {
        this[key] = value
    }


    /**
     * Inserts a Bundle value into the mapping of this Bundle, replacing
     * any existing value for the given key.  Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a Bundle object, or null
     */
    fun Bundle.putBundle(key: String?, value: HashMap<*,*>?) {
        this[key] = value
    }

    fun Bundle.getBundle(key: String?): Bundle? {
        return this[key] as Bundle?
    }

    fun Bundle.getByte(key: String?): Byte {
        return this[key] as Byte
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a byte value
     */
    fun Bundle.getByte(key: String?, defaultValue: Byte): Byte {
        return this.get(key) as? Byte ?: defaultValue
    }

    /**
     * Returns the value associated with the given key, or (char) 0 if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a char value
     */
    fun Bundle.getChar(key: String?): Char {
        return this[key] as Char 
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a char value
     */
    fun Bundle.getChar(key: String?, defaultValue: Char): Char {
        return this[key] as? Char ?: defaultValue 
    }

    /**
     * Returns the value associated with the given key, or (short) 0 if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a Int value
     */
    fun Bundle.getInt(key: String?): Int {
        return this[key] as Int
    }

    /**
     * Returns the value associated with the given key, or (short) 0 if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a String value
     */
    fun Bundle.getString(key: String?): String {
        return this[key] as String
    }

    /**
     * Returns the value associated with the given key, or (short) 0 if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a Boolean value
     */
    fun Bundle.getBoolean(key: String?): Boolean {
        return this[key] as Boolean
    }


    /**
     * Returns the value associated with the given key, or (short) 0 if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a short value
     */
    fun Bundle.getShort(key: String?): Short {
        return this[key] as Short
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a short value
     */
    fun Bundle.getShort(key: String?, defaultValue: Short): Short {
        return this[key] as? Short ?: defaultValue 
    }

    /**
     * Returns the value associated with the given key, or 0.0f if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @return a float value
     */
    fun Bundle.getFloat(key: String?): Float {
        return this[key] as Float 
    }

    /**
     * Returns the value associated with the given key, or defaultValue if
     * no mapping of the desired type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a float value
     */
    fun Bundle.getFloat(key: String?, defaultValue: Float): Float {
        return this[key] as? Float ?: defaultValue 
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a CharSequence value, or null
     */
        fun Bundle.getCharSequence(key: String?): CharSequence {
        return this[key] as CharSequence 
    }


    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a byte[] value, or null
     */
    fun Bundle.getByteArray(key: String?): ByteArray {
        return this[key] as ByteArray 
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a IntArray value, or null
     */
    fun Bundle.getIntArray(key: String?): IntArray {
        return this[key] as IntArray
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a short[] value, or null
     */
        fun Bundle.getShortArray(key: String?): ShortArray {
        return this[key] as ShortArray 
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a char[] value, or null
     */
        fun Bundle.getCharArray(key: String?): CharArray {
        return this[key] as CharArray 
    }

    /**
     * Returns the value associated with the given key, or null if
     * no mapping of the desired type exists for the given key or a null
     * value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a float[] value, or null
     */
        fun Bundle.getFloatArray(key: String?): FloatArray {
        return this[key] as FloatArray 
    }



  

    /**
     * @hide
     */
    @Synchronized
    fun Bundle.toShortString(): String {
        return "this.size=" + this.size
    }
