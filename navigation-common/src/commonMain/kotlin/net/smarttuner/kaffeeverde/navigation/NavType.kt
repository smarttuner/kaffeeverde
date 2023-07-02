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
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/navigation/navigation-common/src/main/java/androidx/navigation/NavType.kt
 */
package net.smarttuner.kaffeeverde.navigation

import net.smarttuner.kaffeeverde.core.Bundle
import net.smarttuner.kaffeeverde.core.putBoolean
import net.smarttuner.kaffeeverde.core.putBooleanArray
import net.smarttuner.kaffeeverde.core.putFloat
import net.smarttuner.kaffeeverde.core.putFloatArray
import net.smarttuner.kaffeeverde.core.putInt
import net.smarttuner.kaffeeverde.core.putIntArray
import net.smarttuner.kaffeeverde.core.putLong
import net.smarttuner.kaffeeverde.core.putLongArray
import net.smarttuner.kaffeeverde.core.putString
import net.smarttuner.kaffeeverde.core.putStringArray

/**
 * NavType denotes the type that can be used in a [NavArgument].
 *
 * There are built-in NavTypes for primitive types, such as int, long, boolean, float, and strings,
 * parcelable, and serializable classes (including Enums), as well as arrays of each supported type.
 *
 * You should only use one of the static NavType instances and subclasses defined in this class.
 *
 * @param T the type of the data that is supported by this NavType
 */
public abstract class NavType<T>(
    /**
     * Check if an argument with this type can hold a null value.
     * @return Returns true if this type allows null values, false otherwise.
     */
    public open val isNullableAllowed: Boolean
) {
    /**
     * Put a value of this type in the `bundle`
     *
     * @param bundle bundle to put value in
     * @param key    bundle key
     * @param value  value of this type
     */
    public abstract fun put(bundle: Bundle, key: String, value: T)
    /**
     * Get a value of this type from the `bundle`
     *
     * @param bundle bundle to get value from
     * @param key    bundle key
     * @return value of this type
     */
    public abstract operator fun get(bundle: Bundle, key: String): T?
    /**
     * Parse a value of this type from a String.
     *
     * @param value string representation of a value of this type
     * @return parsed value of the type represented by this NavType
     * @throws IllegalArgumentException if value cannot be parsed into this type
     */
    public abstract fun parseValue(value: String): T
    /**
     * Parse a value of this type from a String and then combine that
     * parsed value with the given previousValue of the same type to
     * provide a new value that contains both the new and previous value.
     *
     * By default, the given value will replace the previousValue.
     *
     * @param value string representation of a value of this type
     * @param previousValue previously parsed value of this type
     * @return combined parsed value of the type represented by this NavType
     * @throws IllegalArgumentException if value cannot be parsed into this type
     */
    public open fun parseValue(value: String, previousValue: T) = parseValue(value)
    /**
     * Parse a value of this type from a String and put it in a `bundle`
     *
     * @param bundle bundle to put value in
     * @param key    bundle key under which to put the value
     * @param value  string representation of a value of this type
     * @return parsed value of the type represented by this NavType
     * @suppress
     */
    
    public fun parseAndPut(bundle: Bundle, key: String, value: String): T {
        val parsedValue = parseValue(value)
        put(bundle, key, parsedValue)
        return parsedValue
    }
    /**
     * Parse a value of this type from a String, combine that parsed value
     * with the given previousValue, and then put that combined parsed
     * value in a `bundle`.
     *
     * @param bundle bundle to put value in
     * @param key    bundle key under which to put the value
     * @param value  string representation of a value of this type
     * @param previousValue previously parsed value of this type
     * @return combined parsed value of the type represented by this NavType
     * @suppress
     */
    
    public fun parseAndPut(bundle: Bundle, key: String, value: String?, previousValue: T): T {
        if (!bundle.containsKey(key)) {
            throw IllegalArgumentException("There is no previous value in this bundle.")
        }
        if (value != null) {
            val parsedCombinedValue = parseValue(value, previousValue)
            put(bundle, key, parsedCombinedValue)
            return parsedCombinedValue
        }
        return previousValue
    }
    /**
     * The name of this type.
     *
     * This is the same value that is used in Navigation XML `argType` attribute.
     *
     * @return name of this type
     */
    public open val name: String = "nav_type"
    override fun toString(): String {
        return name
    }
    companion object {
        /**
         * TODO: this piece of code is commented due to the lack of full-reflection feature in K/N
         * Parse an argType string into a NavType.
         *
         * @param type        argType string, usually parsed from the Navigation XML file
         * @param packageName package name of the R file,
         * used for parsing relative class names starting with a dot.
         * @return a NavType representing the type indicated by the argType string.
         * Defaults to StringType for null.
         * @throws IllegalArgumentException if there is no valid argType
         * @throws RuntimeException if the type class name cannot be found
         */
        @Suppress("NON_FINAL_MEMBER_IN_OBJECT", "UNCHECKED_CAST") // this needs to be open to
        // maintain api compatibility and type cast are unchecked

        public open fun fromArgType(type: String?, packageName: String?): NavType<*> {
            when {
                IntType.name == type -> return IntType
                IntArrayType.name == type -> return IntArrayType
                LongType.name == type -> return LongType
                LongArrayType.name == type -> return LongArrayType
                BoolType.name == type -> return BoolType
                BoolArrayType.name == type -> return BoolArrayType
                StringType.name == type -> return StringType
                StringArrayType.name == type -> return StringArrayType
                FloatType.name == type -> return FloatType
                FloatArrayType.name == type -> return FloatArrayType
                ReferenceType.name == type -> return ReferenceType
//                !type.isNullOrEmpty() -> {
//                    try {
//                        var className: String
//                        className = if (type.startsWith(".") && packageName != null) {
//                            packageName + type
//                        } else {
//                            type
//                        }
//                        if (type.endsWith("[]")) {
//                            className = className.substring(0, className.length - 2)
//                            val clazz = Class.forName(className)
//                            when {
//                                Parcelable::class.java.isAssignableFrom(clazz) -> {
//                                    return ParcelableArrayType(clazz as Class<Parcelable>)
//                                }
//                                Serializable::class.java.isAssignableFrom(clazz) -> {
//                                    return SerializableArrayType(clazz as Class<Serializable>)
//                                }
//                            }
//                        } else {
//                            val clazz = Class.forName(className)
//                            when {
//                                Parcelable::class.java.isAssignableFrom(clazz) -> {
//                                    return ParcelableType(clazz as Class<Any?>)
//                                }
//                                Enum::class.java.isAssignableFrom(clazz) -> {
//                                    return EnumType(clazz as Class<Enum<*>>)
//                                }
//                                Serializable::class.java.isAssignableFrom(clazz) -> {
//                                    return SerializableType(clazz as Class<Serializable>)
//                                }
//                            }
//                        }
//                        throw IllegalArgumentException(
//                            "$className is not Serializable or Parcelable."
//                        )
//                    } catch (e: ClassNotFoundException) {
//                        throw RuntimeException(e)
//                    }
//                }
            }
            return StringType
        }
        /** @suppress */
        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        
        
        public fun inferFromValue(value: String): NavType<Any> {
            // because we allow Long literals without the L suffix at runtime,
            // the order of IntType and LongType parsing has to be reversed compared to Safe Args
            try {
                IntType.parseValue(value)
                return IntType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            try {
                LongType.parseValue(value)
                return LongType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            try {
                FloatType.parseValue(value)
                return FloatType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            try {
                BoolType.parseValue(value)
                return BoolType as NavType<Any>
            } catch (e: IllegalArgumentException) {
                // ignored, proceed to check next type
            }
            return StringType as NavType<Any>
        }
        /**
         * @param value nothing
         * @throws IllegalArgumentException not real
         * @suppress
         */
        @Suppress("UNCHECKED_CAST") // needed for cast to NavType<Any>
        
        
        public fun inferFromValueType(value: Any?): NavType<Any> {
            return when {
                value is Int -> IntType as NavType<Any>
                value is IntArray -> IntArrayType as NavType<Any>
                value is Long -> LongType as NavType<Any>
                value is LongArray -> LongArrayType as NavType<Any>
                value is Float -> FloatType as NavType<Any>
                value is FloatArray -> FloatArrayType as NavType<Any>
                value is Boolean -> BoolType as NavType<Any>
                value is BooleanArray -> BoolArrayType as NavType<Any>
                value is String || value == null -> StringType as NavType<Any>
                value is Array<*> -> StringArrayType as NavType<Any>
                else -> {
                    throw IllegalArgumentException(
                        "Object is not supported for navigation " +
                            "arguments."
                    )
                }
            }
        }
        /**
         * NavType for storing integer values,
         * corresponding with the "integer" type in a Navigation XML file.
         *
         * Null values are not supported.
         */
        
        public val IntType: NavType<Int> = object : NavType<Int>(false) {
            override val name: String
                get() = "integer"
            override fun put(bundle: Bundle, key: String, value: Int) {
                bundle.putInt(key, value)
            }
            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): Int {
                return bundle[key] as Int
            }
            override fun parseValue(value: String): Int {
                return if (value.startsWith("0x")) {
                    value.substring(2).toInt(16)
                } else {
                    value.toInt()
                }
            }
        }
        /**
         * NavType for storing integer values representing resource ids,
         * corresponding with the "reference" type in a Navigation XML file.
         *
         * Null values are not supported.
         */
        
        public val ReferenceType: NavType<Int> = object : NavType<Int>(false) {
            override val name: String
                get() = "reference"
            override fun put(bundle: Bundle, key: String,  value: Int) {
                bundle.putInt(key, value)
            }
            
            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): Int {
                return bundle[key] as Int
            }
            override fun parseValue(value: String): Int {
                return if (value.startsWith("0x")) {
                    value.substring(2).toInt(16)
                } else {
                    value.toInt()
                }
            }
        }
        /**
         * NavType for storing integer arrays,
         * corresponding with the "integer[]" type in a Navigation XML file.
         *
         * Null values are supported.
         * Default values in Navigation XML files are not supported.
         */
        
        public val IntArrayType: NavType<IntArray?> = object : NavType<IntArray?>(true) {
            override val name: String
                get() = "integer[]"
            override fun put(bundle: Bundle, key: String, value: IntArray?) {
                bundle.putIntArray(key, value)
            }
            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): IntArray? {
                return bundle[key] as IntArray?
            }
            override fun parseValue(value: String): IntArray {
                return intArrayOf(IntType.parseValue(value))
            }
            override fun parseValue(value: String, previousValue: IntArray?): IntArray {
                return previousValue?.plus(parseValue(value)) ?: parseValue(value)
            }
        }
        /**
         * NavType for storing long values,
         * corresponding with the "long" type in a Navigation XML file.
         *
         * Null values are not supported.
         * Default values for this type in Navigation XML files must always end with an 'L' suffix, e.g.
         * `app:defaultValue="123L"`.
         */
        
        public val LongType: NavType<Long> = object : NavType<Long>(false) {
            override val name: String
                get() = "long"
            override fun put(bundle: Bundle, key: String, value: Long) {
                bundle.putLong(key, value)
            }
            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): Long {
                return bundle[key] as Long
            }
            override fun parseValue(value: String): Long {
                // At runtime the L suffix is optional, contrary to the Safe Args plugin.
                // This is in order to be able to parse long numbers passed as deep link URL
                // parameters
                var localValue = value
                if (value.endsWith("L")) {
                    localValue = localValue.substring(0, value.length - 1)
                }
                return if (value.startsWith("0x")) {
                    localValue.substring(2).toLong(16)
                } else {
                    localValue.toLong()
                }
            }
        }
        /**
         * NavType for storing long arrays,
         * corresponding with the "long[]" type in a Navigation XML file.
         *
         * Null values are supported.
         * Default values in Navigation XML files are not supported.
         */
        
        public val LongArrayType: NavType<LongArray?> = object : NavType<LongArray?>(true) {
            override val name: String
                get() = "long[]"
            override fun put(bundle: Bundle, key: String, value: LongArray?) {
                bundle.putLongArray(key, value)
            }
            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): LongArray? {
                return bundle[key] as LongArray?
            }
            override fun parseValue(value: String): LongArray {
                return longArrayOf(LongType.parseValue(value))
            }
            override fun parseValue(value: String, previousValue: LongArray?): LongArray? {
                return previousValue?.plus(parseValue(value)) ?: parseValue(value)
            }
        }
        /**
         * NavType for storing float values,
         * corresponding with the "float" type in a Navigation XML file.
         *
         * Null values are not supported.
         */
        
        public val FloatType: NavType<Float> = object : NavType<Float>(false) {
            override val name: String
                get() = "float"
            override fun put(bundle: Bundle, key: String, value: Float) {
                bundle.putFloat(key, value)
            }
            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): Float {
                return bundle[key] as Float
            }
            override fun parseValue(value: String): Float {
                return value.toFloat()
            }
        }
        /**
         * NavType for storing float arrays,
         * corresponding with the "float[]" type in a Navigation XML file.
         *
         * Null values are supported.
         * Default values in Navigation XML files are not supported.
         */
        
        public val FloatArrayType: NavType<FloatArray?> = object : NavType<FloatArray?>(true) {
            override val name: String
                get() = "float[]"
            override fun put(bundle: Bundle, key: String, value: FloatArray?) {
                bundle.putFloatArray(key, value)
            }
            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): FloatArray? {
                return bundle[key] as FloatArray?
            }
            override fun parseValue(value: String): FloatArray {
                return floatArrayOf(FloatType.parseValue(value))
            }
            override fun parseValue(value: String, previousValue: FloatArray?): FloatArray? {
                return previousValue?.plus(parseValue(value)) ?: parseValue(value)
            }
        }
        /**
         * NavType for storing boolean values,
         * corresponding with the "boolean" type in a Navigation XML file.
         *
         * Null values are not supported.
         */
        
        public val BoolType: NavType<Boolean> = object : NavType<Boolean>(false) {
            override val name: String
                get() = "boolean"
            override fun put(bundle: Bundle, key: String, value: Boolean) {
                bundle.putBoolean(key, value)
            }
            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): Boolean? {
                return bundle[key] as Boolean?
            }
            override fun parseValue(value: String): Boolean {
                return when (value) {
                    "true" -> true
                    "false" -> false
                    else -> {
                        throw IllegalArgumentException(
                            "A boolean NavType only accepts \"true\" or \"false\" values."
                        )
                    }
                }
            }
        }
        /**
         * NavType for storing boolean arrays,
         * corresponding with the "boolean[]" type in a Navigation XML file.
         *
         * Null values are supported.
         * Default values in Navigation XML files are not supported.
         */
        
        public val BoolArrayType: NavType<BooleanArray?> = object : NavType<BooleanArray?>(true) {
            override val name: String
                get() = "boolean[]"
            override fun put(bundle: Bundle, key: String, value: BooleanArray?) {
                bundle.putBooleanArray(key, value)
            }
            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): BooleanArray? {
                return bundle[key] as BooleanArray?
            }
            override fun parseValue(value: String): BooleanArray {
                return booleanArrayOf(BoolType.parseValue(value))
            }
            override fun parseValue(value: String, previousValue: BooleanArray?): BooleanArray? {
                return previousValue?.plus(parseValue(value)) ?: parseValue(value)
            }
        }
        /**
         * NavType for storing String values,
         * corresponding with the "string" type in a Navigation XML file.
         *
         * Null values are supported.
         */
        
        public val StringType: NavType<String?> = object : NavType<String?>(true) {
            override val name: String
                get() = "string"
            override fun put(bundle: Bundle, key: String, value: String?) {
                bundle.putString(key, value)
            }
            @Suppress("DEPRECATION")
            override fun get(bundle: Bundle, key: String): String? {
                return bundle[key] as String?
            }
            override fun parseValue(value: String): String {
                return value
            }
        }
        /**
         * NavType for storing String arrays,
         * corresponding with the "string[]" type in a Navigation XML file.
         *
         * Null values are supported.
         * Default values in Navigation XML files are not supported.
         */
        
        public val StringArrayType: NavType<Array<String>?> = object : NavType<Array<String>?>(
            true
        ) {
            override val name: String
                get() = "string[]"
            override fun put(bundle: Bundle, key: String, value: Array<String>?) {
                bundle.putStringArray(key, value)
            }
            @Suppress("UNCHECKED_CAST", "DEPRECATION")
            override fun get(bundle: Bundle, key: String): Array<String>? {
                return bundle[key] as Array<String>?
            }
            override fun parseValue(value: String): Array<String> {
                return arrayOf(value)
            }
            override fun parseValue(value: String, previousValue: Array<String>?): Array<String>? {
                return previousValue?.plus(parseValue(value)) ?: parseValue(value)
            }
        }
    }
}