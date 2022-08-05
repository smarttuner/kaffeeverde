/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/net/UriCodec.java
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.core.net

import com.ditchoom.buffer.*
import com.ditchoom.buffer.ByteOrder
import net.smarttuner.kaffeeverde.core.net.URISyntaxException

/**
 * Decodes “application/x-www-form-urlencoded” content.
 *
 * @hide
 */
object UriCodec {
    /**
     * Interprets a char as hex digits, returning a number from -1 (invalid char) to 15 ('f').
     */
    private fun hexCharToValue(c: Char): Int {
        if (c in '0'..'9') {
            return c.code - '0'.code
        }
        if (c in 'a'..'f') {
            return 10 + c.code - 'a'.code
        }
        return if (c in 'A'..'F') {
            10 + c.code - 'A'.code
        } else -1
    }

    private fun unexpectedCharacterException(
        uri: String, name: String?, unexpected: Char, index: Int
    ): URISyntaxException {
        val nameString = if (name == null) "" else " in [$name]"
        return URISyntaxException(
            uri, "Unexpected character$nameString: $unexpected", index
        )
    }

    @Throws(URISyntaxException::class)
    private fun getNextCharacter(uri: String, index: Int, end: Int, name: String?): Char {
        if (index >= end) {
            val nameString = if (name == null) "" else " in [$name]"
            throw URISyntaxException(
                uri, "Unexpected end of string$nameString", index
            )
        }
        return uri[index]
    }

    /**
     * Decode a string according to the rules of this decoder.
     *
     * - if `convertPlus == true` all ‘+’ chars in the decoded output are converted to ‘ ‘
     * (white space)
     * - if `throwOnFailure == true`, an [IllegalArgumentException] is thrown for
     * invalid inputs. Else, U+FFFd is emitted to the output in place of invalid input octets.
     */
    fun decode(
        s: String, convertPlus: Boolean, throwOnFailure: Boolean
    ): String {
        val builder: StringBuilder = StringBuilder(s.length)
        appendDecoded(builder, s, convertPlus, throwOnFailure)
        return builder.toString()
    }

    /**
     * Character to be output when there's an error decoding an input.
     */
    private const val INVALID_INPUT_CHARACTER = '\ufffd'
    private fun appendDecoded(
        builder: StringBuilder,
        s: String,
        convertPlus: Boolean,
        throwOnFailure: Boolean
    ) {
        // Holds the bytes corresponding to the escaped chars being read (empty if the last char
        // wasn't a escaped char).
        val byteBuffer = PlatformBuffer.allocate(s.length, zone = AllocationZone.Direct, byteOrder = ByteOrder.BIG_ENDIAN)
        var i = 0
        while (i < s.length) {
            var c = s[i]
            i++
            when (c) {
                '+' -> {
                    flushDecodingByteAccumulator(
                        builder, byteBuffer, throwOnFailure
                    )
                    builder.append(if (convertPlus) ' ' else '+')
                }
                '%' -> {
                    // Expect two characters representing a number in hex.
                    var hexValue: Byte = 0
                    var j = 0
                    while (j < 2) {
                        c = try {
                            getNextCharacter(s, i, s.length, null /* name */)
                        } catch (e: URISyntaxException) {
                            // Unexpected end of input.
                            if (throwOnFailure) {
                                throw IllegalArgumentException(e)
                            } else {
                                flushDecodingByteAccumulator(
                                    builder, byteBuffer, throwOnFailure
                                )
                                builder.append(INVALID_INPUT_CHARACTER)
                                return
                            }
                        }
                        i++
                        val newDigit = hexCharToValue(c)
                        if (newDigit < 0) {
                            if (throwOnFailure) {
                                throw IllegalArgumentException(
                                    unexpectedCharacterException(s, null /* name */, c, i - 1)
                                )
                            } else {
                                flushDecodingByteAccumulator(
                                    builder, byteBuffer, throwOnFailure
                                )
                                builder.append(INVALID_INPUT_CHARACTER)
                                break
                            }
                        }
                        hexValue = (hexValue * 0x10 + newDigit).toByte()
                        j++
                    }
                    byteBuffer.write(hexValue)
                }
                else -> {
                    flushDecodingByteAccumulator(builder, byteBuffer, throwOnFailure)
                    builder.append(c)
                }
            }
        }
        flushDecodingByteAccumulator(builder, byteBuffer, throwOnFailure)
    }

    private fun flushDecodingByteAccumulator(
        builder: StringBuilder,
        byteBuffer: PlatformBuffer,
        throwOnFailure: Boolean
    ) {
        if (byteBuffer.position() == 0) {
            return
        }
        val byteWritten = byteBuffer.position()
        byteBuffer.flip()
        try {
            builder.append(byteBuffer.readUtf8(byteWritten))
        } catch (e: CharacterCodingException) {
            if (throwOnFailure) {
                throw IllegalArgumentException(e)
            } else {
                builder.append(INVALID_INPUT_CHARACTER)
            }
        } finally {
            // Use the byte buffer to write again.
            byteBuffer.flip()
            byteBuffer.limit()
        }
    }
}

fun PlatformBuffer.flip(): PlatformBuffer{
    this.setLimit(position())
    this.position(0)
    return this
}
