/*
 * Copyright (c) 1995, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package net.smarttuner.kaffeeverde.core.net

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import net.smarttuner.kaffeeverde.core.io.UnsupportedEncodingException

/* URLEncoder.java -- Class to convert strings to a properly encoded URL
   Copyright (C) 1998, 1999, 2001, 2002, 2003 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version. */
/**
 *
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://developer.classpath.org/doc/java/net/URLEncoder-source.html
 *
 * The content of this file is a port of the original work with some additions
 *
 */

/*
 * Written using on-line Java Platform 1.2/1.4 API Specification, as well
 * as "The Java Class Libraries", 2nd edition (Addison-Wesley, 1998).
 * Status Believed complete and correct.
 */

/*
 * Written using on-line Java Platform 1.2/1.4 API Specification, as well
 * as "The Java Class Libraries", 2nd edition (Addison-Wesley, 1998).
 * Status Believed complete and correct.
 */
/**
 * This utility class contains static methods that converts a
 * string into a fully encoded URL string in x-www-form-urlencoded
 * format.  This format replaces certain disallowed characters with
 * encoded equivalents.  All upper case and lower case letters in the
 * US alphabet remain as is, the space character (' ') is replaced with
 * '+' sign, and all other characters are converted to a "%XX" format
 * where XX is the hexadecimal representation of that character in a
 * certain encoding (by default, the platform encoding, though the
 * standard is "UTF-8").
 *
 *
 * This method is very useful for encoding strings to be sent to CGI scripts
 *
 * @author Aaron M. Renn (arenn@urbanophile.com)
 * @author Warren Levy (warrenl@cygnus.com)
 * @author Mark Wielaard (mark@klomp.org)
 */
object URLEncoder {


    /**
     * This method translates the passed in string into x-www-form-urlencoded
     * format using the character encoding to hex-encode the unsafe characters.
     *
     * @param s The String to convert
     * @param encoding The encoding to use for unsafe characters
     *
     * @return The converted String
     *
     * @exception UnsupportedEncodingException If the named encoding is not
     * supported
     *
     * @since 1.4
     */
    @Throws(UnsupportedEncodingException::class)
    fun encode(s: String, encoding: String = "UTF-8"): String {
        val length = s.length
        var start = 0
        var i = 0
        val result: StringBuilder = StringBuilder(length)
        while (true) {
            while (i < length && isSafe(s[i])) i++

            // Safe character can just be added
            result.append(s.substring(start, i))

            // Are we done?
            if (i >= length) return result.toString() else if (s[i] == ' ') {
                result.append('+') // Replace space char with plus symbol.
                i++
            } else {
                // Get all unsafe characters
                start = i
                var c: Char = s[i]
                while (i < length && s[i].also { c = it } != ' ' && !isSafe(c)) i++

                // Convert them to %XY encoded strings
                val unsafe = s.substring(start, i)
                val bytes: ByteArray = unsafe.toByteArray(Charset.forName(encoding))
                for (j in bytes.indices) {
                    result.append('%')
                    val `val` = bytes[j].toInt()
                    result.append(hex[`val` and 0xf0 shr 4])
                    result.append(hex[`val` and 0x0f])
                }
            }
            start = i
        }
    }

    /**
     * Private static method that returns true if the given char is either
     * a uppercase or lowercase letter from 'a' till 'z', or a digit froim
     * '0' till '9', or one of the characters '-', '_', '.' or '*'. Such
     * 'safe' character don't have to be url encoded.
     */
    private fun isSafe(c: Char): Boolean {
        return (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-' || c == '_' || c == '.' || c == '*')
    }

    /**
     * Used to convert to hex.  We don't use Integer.toHexString, since
     * it converts to lower case (and the Sun docs pretty clearly
     * specify upper case here), and because it doesn't provide a
     * leading 0.
     */
    private const val hex = "0123456789ABCDEF"
}