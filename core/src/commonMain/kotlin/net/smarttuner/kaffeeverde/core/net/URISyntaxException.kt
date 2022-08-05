package net.smarttuner.kaffeeverde.core.net

import kotlin.jvm.JvmOverloads

/* URISyntaxException.java -- a string could not be parsed as a URI
   Copyright (C) 2002 Free Software Foundation, Inc.

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
exception statement from your version. */ /**
 * This exception is thrown when a String cannot be parsed as a URI.
 *
 * @author Eric Blake (ebb9@email.byu.edu)
 * @see URI
 *
 * @since 1.4
 * @status updated to 1.4
 */
/**
 *
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://developer.classpath.org/doc/java/net/URISyntaxException-source.html
 *
 * The content of this file is a port of the original work with some additions
 *
 */
class URISyntaxException @JvmOverloads constructor(input: String, msg: String, index: Int = -1) :
    Exception(
        msg
    ) {
    /**
     * Returns the bad input string.
     *
     * @return the bad URI, guaranteed non-null
     */
    /**
     * The failed input.
     *
     * @serial the bad URI
     */
    val input: String
    /**
     * Returns the index of the failure, or -1.
     *
     * @return the index of failure
     */
    /**
     * The index of failure.
     *
     * @serial the location of the problem
     */
    val index: Int
    /**
     * Create an exception from the invalid string, with the index of the
     * point of failure.
     *
     * @param input the bad URI
     * @param msg the descriptive error message
     * @param index the index of the parse error, or -1
     * @throws NullPointerException if input or msg are null
     * @throws IllegalArgumentException if index &lt; -1
     */
    /**
     * Create an exception from the invalid string, with the index set to -1.
     *
     * @param input the bad URI
     * @param msg the descriptive error message
     * @throws NullPointerException if input or msg are null
     */
    init {
        // The toString() hack checks for null.
        this.input = input
        this.index = index
        if (index < -1) throw IllegalArgumentException()
    }

    /**
     * Returns the reason for the failure.
     *
     * @return the message, guaranteed non-null
     */
    val reason: String?
        get() = super.message

    /**
     * Returns a message describing the parse error, as if by
     * `getReason() + (getIndex() >= 0 ? " at index " + getIndex() : "")
     * + ": " + getInput()`.
     *
     * @return the message string
     */
    override val message: String
        get() = (super.message + (if (index >= 0) " at index $index" else "")
                + ": " + input)

    companion object {
        /**
         * Compatible with JDK 1.4+.
         */
        private const val serialVersionUID = 2137979680897488891L
    }
}