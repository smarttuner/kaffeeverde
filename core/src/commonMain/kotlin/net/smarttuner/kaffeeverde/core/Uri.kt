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
 * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/net/Uri.java
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.core

import io.github.aakira.napier.Napier
import io.ktor.utils.io.core.*
import net.smarttuner.kaffeeverde.core.io.UnsupportedEncodingException
import net.smarttuner.kaffeeverde.core.net.URLEncoder
import net.smarttuner.kaffeeverde.core.net.UriCodec.decode
import kotlin.jvm.Volatile


abstract class Uri private constructor(){

    companion object{
        /** Index of a component which was not found.  */
        private const val NOT_FOUND = -1

        /** Placeholder value for an index which hasn't been calculated yet.  */
        private const val NOT_CALCULATED = -2

        /**
         * The empty URI, equivalent to "".
         */
        val EMPTY: Uri = HierarchicalUri(
            null, Part.NULL,
            PathPart.EMPTY, Part.NULL, Part.NULL
        )

        /**
         * Decodes '%'-escaped octets in the given string using the UTF-8 scheme.
         * Replaces invalid octets with the unicode replacement character
         * ("\\uFFFD").
         *
         * @param s encoded string to decode
         * @return the given string with escaped octets decoded, or null if
         * s is null
         */
        fun decode(s: String?): String? {
            return if (s == null) {
                null
            } else decode(
                s, false , false /* throwOnFailure */
            )
        }

        /**
         * Creates a Uri which parses the given encoded URI string.
         *
         * @param uriString an RFC 2396-compliant, encoded URI
         * @throws NullPointerException if uriString is null
         * @return Uri for this given uri string
         */
        fun parse(uriString: String): Uri? {
            return StringUri(uriString)
        }



        /**
         * Returns true if the given character is allowed.
         *
         * @param c character to check
         * @param allow characters to allow
         * @return true if the character is allowed or false if it should be
         * encoded
         */
        private fun isAllowed(c: Char, allow: String?): Boolean {
            return (
                    (((c in ('A'..'Z')) || (c in ('a'..'z')) || (c >= '0')) && (c <= '9')) || ("_-!.~'()*".indexOf(
                        c
                    ) != NOT_FOUND) || (allow != null)) && (allow?.indexOf(c) != NOT_FOUND)
        }

        private val HEX_DIGITS = "0123456789ABCDEF".toCharArray()

        /**
         * Encodes characters in the given string as '%'-escaped octets
         * using the UTF-8 scheme. Leaves letters ("A-Z", "a-z"), numbers
         * ("0-9"), and unreserved characters ("_-!.~'()*") intact. Encodes
         * all other characters.
         *
         * @param s string to encode
         * @return an encoded version of s suitable for use as a URI component,
         * or null if s is null
         */
        open fun encode(s: String?): String? {
            return encode(s, null)
        }

        /**
         * Encodes characters in the given string as '%'-escaped octets
         * using the UTF-8 scheme. Leaves letters ("A-Z", "a-z"), numbers
         * ("0-9"), and unreserved characters ("_-!.~'()*") intact. Encodes
         * all other characters with the exception of those specified in the
         * allow argument.
         *
         * @param s string to encode
         * @param allow set of additional characters to allow in the encoded form,
         * null if no characters should be skipped
         * @return an encoded version of s suitable for use as a URI component,
         * or null if s is null
         */
        open fun encode(s: String?, allow: String?): String? {
            if (s == null) {
                return null
            }
            // Lazily-initialized buffers.
            var encoded: StringBuilder? = null
            val oldLength = s.length
            // This loop alternates between copying over allowed characters and
            // encoding in chunks. This results in fewer method calls and
            // allocations than encoding one character at a time.
            var current = 0
            while (current < oldLength) {
                // Start in "copying" mode where we copy over allowed chars.
                // Find the next character which needs to be encoded.
                var nextToEncode = current
                while (nextToEncode < oldLength
                    && isAllowed(s[nextToEncode], allow)
                ) {
                    nextToEncode++
                }
                // If there's nothing more to encode...
                if (nextToEncode == oldLength) {
                    return if (current == 0) {
                        // We didn't need to encode anything!
                        s
                    } else {
                        // Presumably, we've already done some encoding.
                        encoded?.append(s, current, oldLength)
                        encoded.toString()
                    }
                }
                if (encoded == null) {
                    encoded = StringBuilder()
                }
                if (nextToEncode > current) {
                    // Append allowed characters leading up to this point.
                    encoded.append(s, current, nextToEncode)
                } else {
                    // assert nextToEncode == current
                }
                // Switch to "encoding" mode.
                // Find the next allowed character.
                current = nextToEncode
                var nextAllowed = current + 1
                while (nextAllowed < oldLength
                    && !isAllowed(s[nextAllowed], allow)
                ) {
                    nextAllowed++
                }
                // Convert the substring to bytes and encode the bytes as
                // '%'-escaped octets.
                val toEncode = s.substring(current, nextAllowed)
                try {
                    val bytes: ByteArray = toEncode.toByteArray()
                    val bytesLength = bytes.size
                    for (i in 0 until bytesLength) {
                        encoded.append('%')
                        encoded.append(HEX_DIGITS[bytes[i].toInt() and 0xf0 shr 4])
                        encoded.append(HEX_DIGITS[bytes[i].toInt() and 0xf])
                    }
                } catch (e: IndexOutOfBoundsException) {
                    throw AssertionError(e)
                }
                current = nextAllowed
            }
            // Encoded could still be null at this point if s is empty.
            return encoded?.toString() ?: s
        }
    }

    /*
    This class aims to do as little up front work as possible. To accomplish
    that, we vary the implementation depending on what the user passes in.
    For example, we have one implementation if the user passes in a
    URI string (StringUri) and another if the user passes in the
    individual components (OpaqueUri).
    *Concurrency notes*: Like any truly immutable object, this class is safe
    for concurrent use. This class uses a caching pattern in some places where
    it doesn't use volatile or synchronized. This is safe to do with ints
    because getting or setting an int is atomic. It's safe to do with a String
    because the internal fields are final and the memory model guarantees other
    threads won't see a partially initialized instance. We are not guaranteed
    that some threads will immediately see changes from other threads on
    certain platforms, but we don't mind if those threads reconstruct the
    cached result. As a result, we get thread safe caching with no concurrency
    overhead, which means the most common case, access from a single thread,
    is as fast as possible.
    From the Java Language spec.:
    "17.5 Final Field Semantics
    ... when the object is seen by another thread, that thread will always
    see the correctly constructed version of that object's final fields.
    It will also see versions of any object or array referenced by
    those final fields that are at least as up-to-date as the final fields
    are."
    In that same vein, all non-transient fields within Uri
    implementations should be final and immutable so as to ensure true
    immutability for clients even when they don't use proper concurrency
    control.
    For reference, from RFC 2396:
    "4.3. Parsing a URI Reference
       A URI reference is typically parsed according to the four main
       components and fragment identifier in order to determine what
       components are present and whether the reference is relative or
       absolute.  The individual components are then parsed for their
       subparts and, if not opaque, to verify their validity.
       Although the BNF defines what is allowed in each component, it is
       ambiguous in terms of differentiating between an authority component
       and a path component that begins with two slash characters.  The
       greedy algorithm is used for disambiguation: the left-most matching
       rule soaks up as much of the URI reference string as it is capable of
       matching.  In other words, the authority component wins."
    The "four main components" of a hierarchical URI consist of
    <scheme>://<authority><path>?<query>
    */
    /*
    This class aims to do as little up front work as possible. To accomplish
    that, we vary the implementation depending on what the user passes in.
    For example, we have one implementation if the user passes in a
    URI string (StringUri) and another if the user passes in the
    individual components (OpaqueUri).
    *Concurrency notes*: Like any truly immutable object, this class is safe
    for concurrent use. This class uses a caching pattern in some places where
    it doesn't use volatile or synchronized. This is safe to do with ints
    because getting or setting an int is atomic. It's safe to do with a String
    because the internal fields are final and the memory model guarantees other
    threads won't see a partially initialized instance. We are not guaranteed
    that some threads will immediately see changes from other threads on
    certain platforms, but we don't mind if those threads reconstruct the
    cached result. As a result, we get thread safe caching with no concurrency
    overhead, which means the most common case, access from a single thread,
    is as fast as possible.
    From the Java Language spec.:
    "17.5 Final Field Semantics
    ... when the object is seen by another thread, that thread will always
    see the correctly constructed version of that object's final fields.
    It will also see versions of any object or array referenced by
    those final fields that are at least as up-to-date as the final fields
    are."
    In that same vein, all non-transient fields within Uri
    implementations should be final and immutable so as to ensure true
    immutability for clients even when they don't use proper concurrency
    control.
    For reference, from RFC 2396:
    "4.3. Parsing a URI Reference
       A URI reference is typically parsed according to the four main
       components and fragment identifier in order to determine what
       components are present and whether the reference is relative or
       absolute.  The individual components are then parsed for their
       subparts and, if not opaque, to verify their validity.
       Although the BNF defines what is allowed in each component, it is
       ambiguous in terms of differentiating between an authority component
       and a path component that begins with two slash characters.  The
       greedy algorithm is used for disambiguation: the left-most matching
       rule soaks up as much of the URI reference string as it is capable of
       matching.  In other words, the authority component wins."
    The "four main components" of a hierarchical URI consist of
    <scheme>://<authority><path>?<query>
    */
    /** Log tag.  */
    internal val LOG: String = "Uri"





    /**
     * Returns true if this URI is hierarchical like "http://google.com".
     * Absolute URIs are hierarchical if the scheme-specific part starts with
     * a '/'. Relative URIs are always hierarchical.
     */
    abstract fun isHierarchical(): Boolean

    /**
     * Returns true if this URI is opaque like "mailto:nobody@google.com". The
     * scheme-specific part of an opaque URI cannot start with a '/'.
     */
    open fun isOpaque(): Boolean {
        return !isHierarchical()
    }

    /**
     * Returns true if this URI is relative, i.e.&nbsp;if it doesn't contain an
     * explicit scheme.
     *
     * @return true if this URI is relative, false if it's absolute
     */
    abstract fun isRelative(): Boolean

    /**
     * Returns true if this URI is absolute, i.e.&nbsp;if it contains an
     * explicit scheme.
     *
     * @return true if this URI is absolute, false if it's relative
     */
    open fun isAbsolute(): Boolean {
        return !isRelative()
    }

    /**
     * Gets the scheme of this URI. Example: "http"
     *
     * @return the scheme or null if this is a relative URI
     */
    
    abstract fun getScheme(): String?

    /**
     * Gets the scheme-specific part of this URI, i.e.&nbsp;everything between
     * the scheme separator ':' and the fragment separator '#'. If this is a
     * relative URI, this method returns the entire URI. Decodes escaped octets.
     *
     *
     * Example: "//www.google.com/search?q=android"
     *
     * @return the decoded scheme-specific-part
     */
    abstract fun getSchemeSpecificPart(): String?

    /**
     * Gets the scheme-specific part of this URI, i.e.&nbsp;everything between
     * the scheme separator ':' and the fragment separator '#'. If this is a
     * relative URI, this method returns the entire URI. Leaves escaped octets
     * intact.
     *
     *
     * Example: "//www.google.com/search?q=android"
     *
     * @return the encoded scheme-specific-part
     */
    abstract fun getEncodedSchemeSpecificPart(): String?

    /**
     * Gets the decoded authority part of this URI. For
     * server addresses, the authority is structured as follows:
     * `[ userinfo '@' ] host [ ':' port ]`
     *
     *
     * Examples: "google.com", "bob@google.com:80"
     *
     * @return the authority for this URI or null if not present
     */
    
    abstract fun getAuthority(): String?

    /**
     * Gets the encoded authority part of this URI. For
     * server addresses, the authority is structured as follows:
     * `[ userinfo '@' ] host [ ':' port ]`
     *
     *
     * Examples: "google.com", "bob@google.com:80"
     *
     * @return the authority for this URI or null if not present
     */
    
    abstract fun getEncodedAuthority(): String?

    /**
     * Gets the decoded user information from the authority.
     * For example, if the authority is "nobody@google.com", this method will
     * return "nobody".
     *
     * @return the user info for this URI or null if not present
     */
    
    abstract fun getUserInfo(): String?

    /**
     * Gets the encoded user information from the authority.
     * For example, if the authority is "nobody@google.com", this method will
     * return "nobody".
     *
     * @return the user info for this URI or null if not present
     */
    
    abstract fun getEncodedUserInfo(): String?

    /**
     * Gets the encoded host from the authority for this URI. For example,
     * if the authority is "bob@google.com", this method will return
     * "google.com".
     *
     * @return the host for this URI or null if not present
     */
    
    abstract fun getHost(): String?

    /**
     * Gets the port from the authority for this URI. For example,
     * if the authority is "google.com:80", this method will return 80.
     *
     * @return the port for this URI or -1 if invalid or not present
     */
    abstract fun getPort(): Int

    /**
     * Gets the decoded path.
     *
     * @return the decoded path, or null if this is not a hierarchical URI
     * (like "mailto:nobody@google.com") or the URI is invalid
     */
    
    abstract fun getPath(): String?

    /**
     * Gets the encoded path.
     *
     * @return the encoded path, or null if this is not a hierarchical URI
     * (like "mailto:nobody@google.com") or the URI is invalid
     */
    
    abstract fun getEncodedPath(): String?

    /**
     * Gets the decoded query component from this URI. The query comes after
     * the query separator ('?') and before the fragment separator ('#'). This
     * method would return "q=android" for
     * "http://www.google.com/search?q=android".
     *
     * @return the decoded query or null if there isn't one
     */
    
    abstract fun getQuery(): String?

    /**
     * Gets the encoded query component from this URI. The query comes after
     * the query separator ('?') and before the fragment separator ('#'). This
     * method would return "q=android" for
     * "http://www.google.com/search?q=android".
     *
     * @return the encoded query or null if there isn't one
     */
    
    abstract fun getEncodedQuery(): String?

    /**
     * Gets the decoded fragment part of this URI, everything after the '#'.
     *
     * @return the decoded fragment or null if there isn't one
     */
    
    abstract fun getFragment(): String?

    /**
     * Gets the encoded fragment part of this URI, everything after the '#'.
     *
     * @return the encoded fragment or null if there isn't one
     */
    
    abstract fun getEncodedFragment(): String?

    /**
     * Gets the decoded path segments.
     *
     * @return decoded path segments, each without a leading or trailing '/'
     */
    abstract fun getPathSegments(): Array<String?>?

    /**
     * Gets the decoded last segment in the path.
     *
     * @return the decoded last segment or null if the path is empty
     */
    
    abstract fun getLastPathSegment(): String?

    /**
     * Compares this Uri to another object for equality. Returns true if the
     * encoded string representations of this Uri and the given Uri are
     * equal. Case counts. Paths are not normalized. If one Uri specifies a
     * default port explicitly and the other leaves it implicit, they will not
     * be considered equal.
     */
    override fun equals( o: Any?): Boolean {
        if (o !is Uri) {
            return false
        }
        return toString() == o.toString()
    }

    /**
     * Hashes the encoded string represention of this Uri consistently with
     * [.equals].
     */
    override fun hashCode(): Int {
        return toString().hashCode()
    }

    /**
     * Compares the string representation of this Uri with that of
     * another.
     */
    open operator fun compareTo(other: Uri): Int {
        return toString().compareTo(other.toString())
    }

    /**
     * Returns the encoded string representation of this URI.
     * Example: "http://google.com/"
     */
    abstract override fun toString(): String

    /**
     * Return a string representation of this URI that has common forms of PII redacted,
     * making it safer to use for logging purposes.  For example, `tel:800-466-4411` is
     * returned as `tel:xxx-xxx-xxxx` and `http://example.com/path/to/item/` is
     * returned as `http://example.com/...`.
     * @return the common forms PII redacted string of this URI
     * @hide
     */
    
    
    open fun toSafeString(): String? {
        val scheme = getScheme()
        var ssp = getSchemeSpecificPart()
        if (scheme != null) {
            if (scheme.equals("tel", ignoreCase = true) || scheme.equals("sip", ignoreCase = true)
                || scheme.equals("sms", ignoreCase = true) || scheme.equals(
                    "smsto",
                    ignoreCase = true
                )
                || scheme.equals("mailto", ignoreCase = true) || scheme.equals(
                    "nfc",
                    ignoreCase = true
                )
            ) {
                val builder: StringBuilder = StringBuilder(64)
                builder.append(scheme)
                builder.append(':')
                if (ssp != null) {
                    for (i in 0 until ssp.length) {
                        val c = ssp[i]
                        if (c == '-' || c == '@' || c == '.') {
                            builder.append(c)
                        } else {
                            builder.append('x')
                        }
                    }
                }
                return builder.toString()
            } else if (scheme.equals("http", ignoreCase = true) || scheme.equals(
                    "https",
                    ignoreCase = true
                )
                || scheme.equals("ftp", ignoreCase = true) || scheme.equals(
                    "rtsp",
                    ignoreCase = true
                )
            ) {
                ssp = ("//" + (if (getHost() != null) getHost() else "")
                        + (if (getPort() != -1) ":" + getPort() else "")
                        + "/...")
            }
        }
        // Not a sensitive scheme, but let's still be conservative about
        // the data we include -- only the ssp, not the query params or
        // fragment, because those can often have sensitive info.
        val builder: StringBuilder = StringBuilder(64)
        if (scheme != null) {
            builder.append(scheme)
            builder.append(':')
        }
        if (ssp != null) {
            builder.append(ssp)
        }
        return builder.toString()
    }

    /**
     * Constructs a new builder, copying the attributes from this Uri.
     */
    abstract fun buildUpon(): Builder?

    /** Index of a component which was not found.  */
    private val NOT_FOUND = -1

    /** Placeholder value for an index which hasn't been calculated yet.  */
    private val NOT_CALCULATED = -2

    /**
     * Error message presented when a user tries to treat an opaque URI as
     * hierarchical.
     */
    private val NOT_HIERARCHICAL = "This isn't a hierarchical URI."

    /** Default encoding.  */
    private val DEFAULT_ENCODING = "UTF-8"

    /**
     * Creates a Uri which parses the given encoded URI string.
     *
     * @param uriString an RFC 2396-compliant, encoded URI
     * @throws NullPointerException if uriString is null
     * @return Uri for this given uri string
     */
    open fun parse(uriString: String?): Uri? {
        return StringUri(uriString!!)
    }



    private class StringUri constructor(val uriString: String) :
        AbstractHierarchicalUri() {

        private fun getQueryPart(): Part? {
            return if (_query == null) Part.fromEncoded(parseQuery()).also {
                _query = it
            } else _query
        }

        override fun getEncodedQuery(): String? {
            return getQueryPart()?.getEncoded()
        }

        /** Cached scheme separator index.  */
        @Volatile
        private var cachedSsi: Int = NOT_CALCULATED

        /** Finds the first ':'. Returns -1 if none found.  */
        private fun findSchemeSeparator(): Int {
            return if (cachedSsi == NOT_CALCULATED) uriString.indexOf(':')
                .also { cachedSsi = it } else cachedSsi
        }

        /** Cached fragment separator index.  */
        @Volatile
        private var cachedFsi: Int = NOT_CALCULATED

        /** Finds the first '#'. Returns -1 if none found.  */
        private fun findFragmentSeparator(): Int {
            return if (cachedFsi == NOT_CALCULATED) uriString.indexOf('#', findSchemeSeparator())
                .also {
                    cachedFsi = it
                } else cachedFsi
        }// No ssp.

        // If the ssp starts with a '/', this is hierarchical.
        // All relative URIs are hierarchical.
        override fun isHierarchical(): Boolean {
                val ssi: Int = findSchemeSeparator()
                if (ssi == NOT_FOUND) {
                    // All relative URIs are hierarchical.
                    return true
                }
                if (uriString.length == ssi + 1) {
                    // No ssp.
                    return false
                }
                // If the ssp starts with a '/', this is hierarchical.
                return uriString[ssi + 1] == '/'
            }

        // Note: We return true if the index is 0
        override fun isRelative(): Boolean
            =// Note: We return true if the index is 0
                findSchemeSeparator() == NOT_FOUND

        @Volatile
        private var _scheme: String = NotCachedHolder.NOT_CACHED
        override fun getScheme(): String {
            val scheme = _scheme
            val cached: Boolean = (scheme !== NotCachedHolder.NOT_CACHED)
            return if (cached) scheme else ((parseScheme().also { this._scheme = (it)!! })!!)
        }

        private fun parseScheme(): String? {
            val ssi: Int = findSchemeSeparator()
            return if (ssi == NOT_FOUND) null else uriString.substring(0, ssi)
        }

        private var ssp: Part? = null
            get() = if (field == null) Part.fromEncoded(parseSsp())
                .also { field = it } else field

        override fun getEncodedSchemeSpecificPart(): String? = ssp?.getEncoded()
        override fun getSchemeSpecificPart(): String? = ssp?.getDecoded()

        private fun parseSsp(): String {
            val ssi: Int = findSchemeSeparator()
            val fsi: Int = findFragmentSeparator()
            // Return everything between ssi and fsi.
            return if (fsi == NOT_FOUND) uriString.substring(ssi + 1) else uriString.substring(
                ssi + 1,
                fsi
            )
        }

        private var authority: Part? = null
        private val authorityPart: Part
            private get() {
                val authority = authority
                if (authority == null) {
                    val encodedAuthority: String? = parseAuthority(
                        uriString, findSchemeSeparator()
                    )
                    return Part.fromEncoded(encodedAuthority).also { this.authority = it }
                }
                return authority
            }

        override fun getEncodedAuthority(): String? = authorityPart.getEncoded()

        override fun getAuthority(): String? {
            return authorityPart.getDecoded()
        }

        private var path: PathPart? = null
        private val pathPart: PathPart?
            get() = if (path == null) PathPart.fromEncoded(parsePath())
                .also { path = it } else path

        override fun getPath(): String? {
            return pathPart?.getDecoded()
        }

        override fun getEncodedPath(): String? = pathPart?.getEncoded()
        override fun getPathSegments(): Array<String?>? = pathPart?.pathSegments?.segments

        private fun parsePath(): String? {
            val uriString: String = uriString
            val ssi: Int = findSchemeSeparator()
            // If the URI is absolute.
            if (ssi > -1) {
                // Is there anything after the ':'?
                val schemeOnly: Boolean = ssi + 1 == uriString.length
                if (schemeOnly) {
                    // Opaque URI.
                    return null
                }
                // A '/' after the ':' means this is hierarchical.
                if (uriString.get(ssi + 1) != '/') {
                    // Opaque URI.
                    return null
                }
            } else {
                // All relative URIs are hierarchical.
            }
            return parsePath(uriString, ssi)
        }

        var _query: Part? = null
        private val _queryPart: Part
            get() {
                return _query ?: return Part.fromEncoded(parseQuery())
                    .also { _query = it }
            }

//        override fun getEncodedQuery(): String? = queryPart?.getEncoded()

        private fun parseQuery(): String? {
            // It doesn't make sense to cache this index. We only ever
            // calculate it once.
            val qsi: Int = uriString.indexOf('?', findSchemeSeparator())
            if (qsi == NOT_FOUND) {
                return null
            }
            val fsi: Int = findFragmentSeparator()
            if (fsi == NOT_FOUND) {
                return uriString.substring(qsi + 1)
            }
            if (fsi < qsi) {
                // Invalid.
                return null
            }
            return uriString.substring(qsi + 1, fsi)
        }

        override fun getQuery(): String? {
            return _queryPart.getDecoded()
        }

        private var fragment: Part? = null

        init {
            if (uriString == null) {
                throw NullPointerException("uriString")
            }
        }

        private val fragmentPart: Part
            get() {
                return fragment ?: Part.fromEncoded(parseFragment()).also {
                    fragment = it
                }
            }

        override fun getEncodedFragment(): String? {
            return fragmentPart.getEncoded()
        }

        private fun parseFragment(): String? {
            val fsi: Int = findFragmentSeparator()
            return if (fsi == NOT_FOUND) null else uriString.substring(fsi + 1)
        }

        override fun getFragment(): String? {
            return fragmentPart.getDecoded()
        }

        override fun toString(): String {
            return uriString
        }

        override fun buildUpon(): Builder {
            if (isHierarchical()) {
                return Builder()
                    .scheme(_scheme)
                    .authority(authorityPart)
                    .path(pathPart)
                    .query(_queryPart)
                    .fragment(fragmentPart)
            } else {
                return Builder()
                    .scheme(_scheme)
                    .opaquePart(ssp)
                    .fragment(fragmentPart)
            }
        }

        companion object {
            /** Used in parcelling.  */
            val TYPE_ID: Int = 1

            /**
             * Parses an authority out of the given URI string.
             *
             * @param uriString URI string
             * @param ssi scheme separator index, -1 for a relative URI
             *
             * @return the authority or null if none is found
             */
            fun parseAuthority(uriString: String, ssi: Int): String? {
                val length: Int = uriString.length
                // If "//" follows the scheme separator, we have an authority.
                if ((length > ssi + 2
                            ) && (uriString.get(ssi + 1) == '/'
                            ) && (uriString.get(ssi + 2) == '/')
                ) {
                    // We have an authority.
                    // Look for the start of the path, query, or fragment, or the
                    // end of the string.
                    var end: Int = ssi + 3
                    LOOP@ while (end < length) {
                        when (uriString.get(end)) {
                            '/', '\\', '?', '#' -> break@LOOP
                        }
                        end++
                    }
                    return uriString.substring(ssi + 3, end)
                } else {
                    return null
                }
            }

            /**
             * Parses a path out of this given URI string.
             *
             * @param uriString URI string
             * @param ssi scheme separator index, -1 for a relative URI
             *
             * @return the path
             */
            fun parsePath(uriString: String, ssi: Int): String {
                val length: Int = uriString.length
                // Find start of path.
                var pathStart: Int
                if ((length > ssi + 2
                            ) && (uriString.get(ssi + 1) == '/'
                            ) && (uriString.get(ssi + 2) == '/')
                ) {
                    // Skip over authority to path.
                    pathStart = ssi + 3
                    LOOP@ while (pathStart < length) {
                        when (uriString.get(pathStart)) {
                            '?', '#' -> return "" // Empty path.
                            '/', '\\' ->                           // Per http://url.spec.whatwg.org/#host-state, the \ character
                                // is treated as if it were a / character when encountered in a
                                // host
                                break@LOOP
                        }
                        pathStart++
                    }
                } else {
                    // Path starts immediately after scheme separator.
                    pathStart = ssi + 1
                }
                // Find end of path.
                var pathEnd: Int = pathStart
                LOOP@ while (pathEnd < length) {
                    when (uriString.get(pathEnd)) {
                        '?', '#' -> break@LOOP
                    }
                    pathEnd++
                }
                return uriString.substring(pathStart, pathEnd)
            }
        }
    }


    internal object NotCachedHolder {
        const val NOT_CACHED = "NOT CACHED"
    }

    /**
     * Support for part implementations.
     */
    abstract class AbstractPart(encoded: String?, decoded: String?) {
        @Volatile
        var _encoded: String? = null

        @Volatile
        var _decoded: String? = null
        private var mCanonicalRepresentation = 0

        init {
            if (encoded !== NotCachedHolder.NOT_CACHED) {
                mCanonicalRepresentation = REPRESENTATION_ENCODED
                this._encoded = encoded
                this._decoded = NotCachedHolder.NOT_CACHED
            } else if (decoded !== NotCachedHolder.NOT_CACHED) {
                mCanonicalRepresentation = REPRESENTATION_DECODED
                this._encoded = NotCachedHolder.NOT_CACHED
                this._decoded = decoded
            } else {
                throw IllegalArgumentException("Neither encoded nor decoded")
            }
        }

        abstract fun getEncoded(): String?
        fun getDecoded(): String? {
            val hasDecoded = _decoded !== NotCachedHolder.NOT_CACHED
            return if (hasDecoded) _decoded else decode(_encoded).also { _decoded = it }
        }


        companion object {
            // Possible values of mCanonicalRepresentation.
            val REPRESENTATION_ENCODED = 1
            val REPRESENTATION_DECODED = 2
        }
    }



    /**
     * Immutable wrapper of encoded and decoded versions of a URI part. Lazily
     * creates the encoded or decoded version from the other.
     */
    open class Part private constructor(encoded: String?, decoded: String?) :
        AbstractPart(encoded, decoded) {
        open val isEmpty: Boolean
            get() = false

        override fun getEncoded(): String? {
            val hasEncoded = _encoded !== NotCachedHolder.NOT_CACHED
            return if (hasEncoded) _encoded!! else encode(_decoded).also { _encoded = it }
        }

        private class EmptyPart(value: String?) : Part(value, value) {
            init {
                if (value != null && value.isNotEmpty()) {
                    throw IllegalArgumentException("Expected empty value, got: $value")
                }
                // Avoid having to re-calculate the non-canonical value.
                _decoded = value
                _encoded = _decoded
            }

            override val isEmpty: Boolean
                get(){
                    return true
                }
        }

        companion object {
            /** A part with null values.  */
            val NULL: Part = EmptyPart(null)

            /** A part with empty strings for values.  */
            val EMPTY: Part = EmptyPart("")


            /**
             * Returns given part or [.NULL] if the given part is null.
             */
            fun nonNull(part: Part?): Part {
                return part ?: NULL
            }

            /**
             * Creates a part from the encoded string.
             *
             * @param encoded part string
             */
            fun fromEncoded(encoded: String?): Part {
                return from(encoded, NotCachedHolder.NOT_CACHED)
            }

            /**
             * Creates a part from the decoded string.
             *
             * @param decoded part string
             */
            fun fromDecoded(decoded: String?): Part {
                return from(NotCachedHolder.NOT_CACHED, decoded)
            }

            /**
             * Creates a part from the encoded and decoded strings.
             *
             * @param encoded part string
             * @param decoded part string
             */
            fun from(encoded: String?, decoded: String?): Part {
                // We have to check both encoded and decoded in case one is
                // NotCachedHolder.NOT_CACHED.
                if (encoded == null) {
                    return NULL
                }
                if (encoded.isEmpty()) {
                    return EMPTY
                }
                if (decoded == null) {
                    return NULL
                }
                return if (decoded.isEmpty()) {
                    EMPTY
                } else Part(encoded, decoded)
            }
        }
    }

    /**
     * Immutable wrapper of encoded and decoded versions of a path part. Lazily
     * creates the encoded or decoded version from the other.
     */
    class PathPart private constructor(encoded: String?, decoded: String?) :
        AbstractPart(encoded, decoded) {
        override fun getEncoded(): String? {
            val hasEncoded = _encoded !== NotCachedHolder.NOT_CACHED
            // Don't encode '/'.
            return if (hasEncoded) (_encoded)!! else (encode(_decoded, "/").also { _encoded = it })
        }// This check keeps us from adding a segment if the path starts
        // '/' and an empty segment for "//".
        // Add in the final path segment.
        /**
         * Gets the individual path segments. Parses them if necessary.
         *
         * @return parsed path segments or null if this isn't a hierarchical
         * URI
         */
        /**
         * Cached path segments. This doesn't need to be volatile--we don't
         * care if other threads see the result.
         */
        var pathSegments: PathSegments? = null
            get() {
                if (field != null) {
                    return field
                }
                val path: String = getEncoded() ?: return PathSegments.EMPTY.also { field = it }
                val segmentBuilder = PathSegmentsBuilder()
                var previous = 0
                var current: Int
                while ((path.indexOf('/', previous).also { current = it }) > -1) {
                    // This check keeps us from adding a segment if the path starts
                    // '/' and an empty segment for "//".
                    if (previous < current) {
                        val decodedSegment = decode(path.substring(previous, current))
                        segmentBuilder.add(decodedSegment)
                    }
                    previous = current + 1
                }
                // Add in the final path segment.
                if (previous < path.length) {
                    segmentBuilder.add(decode(path.substring(previous)))
                }
                return segmentBuilder.build().also { field = it }
            }
            private set

        companion object {
            /** A part with null values.  */
            val NULL = PathPart(null, null)

            /** A part with empty strings for values.  */
            val EMPTY = PathPart("", "")
            fun appendEncodedSegment(
                oldPart: PathPart?,
                newSegment: String
            ): PathPart {
                // If there is no old path, should we make the new path relative
                // or absolute? I pick absolute.
                // No old path.
                if (oldPart == null) return fromEncoded("/$newSegment")
                val encoded = oldPart.getEncoded() ?: return fromEncoded("/$newSegment")

                val oldPath: String = encoded
                val oldPathLength = oldPath.length
                val newPath: String
                if (oldPathLength == 0) {
                    // No old path.
                    newPath = "/$newSegment"
                } else if (oldPath[oldPathLength - 1] == '/') {
                    newPath = oldPath + newSegment
                } else {
                    newPath = "$oldPath/$newSegment"
                }
                return fromEncoded(newPath)
            }

            fun appendDecodedSegment(oldPart: PathPart?, decoded: String?): PathPart? {
                val encoded: String = encode(decoded) ?: return null
                // TODO: Should we reuse old PathSegments? Probably not.
                return appendEncodedSegment(oldPart, encoded)
            }

           

            /**
             * Creates a path from the encoded string.
             *
             * @param encoded part string
             */
            fun fromEncoded(encoded: String?): PathPart {
                return from(encoded, NotCachedHolder.NOT_CACHED)
            }

            /**
             * Creates a path from the decoded string.
             *
             * @param decoded part string
             */
            fun fromDecoded(decoded: String?): PathPart {
                return from(NotCachedHolder.NOT_CACHED, decoded)
            }

            /**
             * Creates a path from the encoded and decoded strings.
             *
             * @param encoded part string
             * @param decoded part string
             */
            fun from(encoded: String?, decoded: String?): PathPart {
                if (encoded == null) {
                    return NULL
                }
                return if (encoded.length == 0) {
                    EMPTY
                } else PathPart(encoded, decoded)
            }

            /**
             * Prepends path values with "/" if they're present, not empty, and
             * they don't already start with "/".
             */
            fun makeAbsolute(oldPart: PathPart): PathPart {
                val encodedCached = oldPart._encoded !== NotCachedHolder.NOT_CACHED
                // We don't care which version we use, and we don't want to force
                // unneccessary encoding/decoding.
                val oldPath = if (encodedCached) oldPart._encoded else oldPart._decoded
                if ((oldPath == null) || (oldPath.length == 0
                            ) || oldPath.startsWith("/")
                ) {
                    return oldPart
                }
                // Prepend encoded string if present.
                val newEncoded =
                    if (encodedCached) "/" + oldPart._encoded else NotCachedHolder.NOT_CACHED
                // Prepend decoded string if present.
                val decodedCached = oldPart._decoded !== NotCachedHolder.NOT_CACHED
                val newDecoded =
                    if (decodedCached) "/" + oldPart._decoded else NotCachedHolder.NOT_CACHED
                return PathPart(newEncoded, newDecoded)
            }
        }
    }

    /**
     * Wrapper for path segment array.
     */
    class PathSegments(val segments: Array<String?>?, override val size: Int) :
        AbstractList<String?>(), RandomAccess {
        override fun get(index: Int): String? {
            if (index >= size) {
                throw IndexOutOfBoundsException()
            }
            return segments!![index]
        }

        fun size(): Int {
            return size
        }

        companion object {
            val EMPTY = PathSegments(null, 0)
        }
    }

    /**
     * Builds PathSegments.
     */
    internal class PathSegmentsBuilder {
        var segments: Array<String?>? = arrayOf()
        var size = 0
        fun add(segment: String?) {
            var segments = segments
            if (segments == null) {
                segments = arrayOfNulls(4)
                this.segments = segments
            } else if (size + 1 == segments.size) {
                val expanded = arrayOfNulls<String>(segments.size * 2)
                segments.copyInto(expanded, 0, 0, segments.size)
                this.segments = expanded
            }
            segments[size++] = segment
        }

        fun build(): PathSegments {
            return if (segments == null) {
                PathSegments.EMPTY
            } else try {
                PathSegments(segments, size)
            } finally {
                // Makes sure this doesn't get reused.
                segments = null
            }
        }
    }

    /**
     * Returns a set of the unique names of all query parameters. Iterating
     * over the set will return the names in order of their first occurrence.
     *
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
     *
     * @return a set of decoded names
     */
    open fun getQueryParameterNames(): Set<String?>? {
        if (isOpaque()) {
            throw UnsupportedOperationException(NOT_HIERARCHICAL)
        }
        val query = getEncodedQuery() ?: return setOf()
        val names: MutableSet<String?> = LinkedHashSet()
        var start = 0
        do {
            val next = query.indexOf('&', start)
            val end = if (next == -1) query.length else next
            var separator = query.indexOf('=', start)
            if (separator > end || separator == -1) {
                separator = end
            }
            val name = query.substring(start, separator)
            names.add(decode(name))
            // Move start to end of name.
            start = end + 1
        } while (start < query.length)
        return names
    }

    /**
     * Searches the query string for parameter values with the given key.
     *
     * @param key which will be encoded
     *
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
     * @throws NullPointerException if key is null
     * @return a list of decoded values
     */
    open fun getQueryParameters(key: String?): List<String?>? {
        if (isOpaque()) {
            throw UnsupportedOperationException(NOT_HIERARCHICAL)
        }
        if (key == null) {
            throw NullPointerException("key")
        }
        val query = getEncodedQuery() ?: return listOf()
        val encodedKey: String
        encodedKey = try {
            URLEncoder.encode(key, DEFAULT_ENCODING)
        } catch (e: UnsupportedEncodingException) {
            throw AssertionError(e)
        }
        val values = mutableListOf<String?>()
        var start = 0
        do {
            val nextAmpersand = query.indexOf('&', start)
            val end = if (nextAmpersand != -1) nextAmpersand else query.length
            var separator = query.indexOf('=', start)
            if (separator > end || separator == -1) {
                separator = end
            }
            if (separator - start == encodedKey.length
                && query.regionMatches(start, encodedKey, 0, encodedKey.length)
            ) {
                if (separator == end) {
                    values.add("")
                } else {
                    values.add(decode(query.substring(separator + 1, end)))
                }
            }
            // Move start to end of name.
            start = if (nextAmpersand != -1) {
                nextAmpersand + 1
            } else {
                break
            }
        } while (true)
        return values
    }





    /**
     * Helper class for building or manipulating URI references. Not safe for
     * concurrent use.
     *
     *
     * An absolute hierarchical URI reference follows the pattern:
     * `<scheme>://<authority><absolute path>?<query>#<fragment>`
     *
     *
     * Relative URI references (which are always hierarchical) follow one
     * of two patterns: `<relative or absolute path>?<query>#<fragment>`
     * or `//<authority><absolute path>?<query>#<fragment>`
     *
     *
     * An opaque URI follows this pattern:
     * `<scheme>:<opaque part>#<fragment>`
     *
     *
     * Use [Uri.buildUpon] to obtain a builder representing an existing URI.
     */
    class Builder
    /**
     * Constructs a new Builder.
     */
    {
        private var scheme: String? = null
        private var opaquePart: Part? = null
        private var authority: Part? = null
        private var path: PathPart? = null
        private var query: Part? = null
        private var fragment: Part? = null

        /**
         * Sets the scheme.
         *
         * @param scheme name or `null` if this is a relative Uri
         */
        fun scheme(scheme: String?): Builder {
            this.scheme = scheme
            return this
        }

        fun opaquePart(opaquePart: Part?): Builder {
            this.opaquePart = opaquePart
            return this
        }

        /**
         * Encodes and sets the given opaque scheme-specific-part.
         *
         * @param opaquePart decoded opaque part
         */
        fun opaquePart(opaquePart: String?): Builder {
            return opaquePart(Part.fromDecoded(opaquePart))
        }

        /**
         * Sets the previously encoded opaque scheme-specific-part.
         *
         * @param opaquePart encoded opaque part
         */
        fun encodedOpaquePart(opaquePart: String?): Builder {
            return opaquePart(Part.fromEncoded(opaquePart))
        }

        fun authority(authority: Part?): Builder {
            // This URI will be hierarchical.
            opaquePart = null
            this.authority = authority
            return this
        }

        /**
         * Encodes and sets the authority.
         */
        fun authority(authority: String?): Builder {
            return authority(Part.fromDecoded(authority))
        }

        /**
         * Sets the previously encoded authority.
         */
        fun encodedAuthority(authority: String?): Builder {
            return authority(Part.fromEncoded(authority))
        }

        fun path(path: PathPart?): Builder {
            // This URI will be hierarchical.
            opaquePart = null
            this.path = path
            return this
        }

        /**
         * Sets the path. Leaves '/' characters intact but encodes others as
         * necessary.
         *
         *
         * If the path is not null and doesn't start with a '/', and if
         * you specify a scheme and/or authority, the builder will prepend the
         * given path with a '/'.
         */
        fun path(path: String?): Builder {
            return path(PathPart.fromDecoded(path))
        }

        /**
         * Sets the previously encoded path.
         *
         *
         * If the path is not null and doesn't start with a '/', and if
         * you specify a scheme and/or authority, the builder will prepend the
         * given path with a '/'.
         */
        fun encodedPath(path: String?): Builder {
            return path(PathPart.fromEncoded(path))
        }

        /**
         * Encodes the given segment and appends it to the path.
         */
        fun appendPath(newSegment: String?): Builder {
            return path(PathPart.appendDecodedSegment(path, newSegment))
        }

        /**
         * Appends the given segment to the path.
         */
        fun appendEncodedPath(newSegment: String?): Builder {
            return path(PathPart.appendEncodedSegment(path, newSegment!!))
        }

        fun query(query: Part?): Builder {
            // This URI will be hierarchical.
            opaquePart = null
            this.query = query
            return this
        }

        /**
         * Encodes and sets the query.
         */
        fun query(query: String?): Builder {
            return query(Part.fromDecoded(query))
        }

        /**
         * Sets the previously encoded query.
         */
        fun encodedQuery(query: String?): Builder {
            return query(Part.fromEncoded(query))
        }

        fun fragment(fragment: Part?): Builder {
            this.fragment = fragment
            return this
        }

        /**
         * Encodes and sets the fragment.
         */
        fun fragment(fragment: String?): Builder {
            return fragment(Part.fromDecoded(fragment))
        }

        /**
         * Sets the previously encoded fragment.
         */
        fun encodedFragment(fragment: String?): Builder {
            return fragment(Part.fromEncoded(fragment))
        }

        /**
         * Encodes the key and value and then appends the parameter to the
         * query string.
         *
         * @param key which will be encoded
         * @param value which will be encoded
         */
        fun appendQueryParameter(key: String?, value: String?): Builder {
            // This URI will be hierarchical.
            opaquePart = null
            val encodedParameter: String = (encode(key, null) + "="
                    + encode(value, null))
            if (query == null) {
                query = Part.fromEncoded(encodedParameter)
                return this
            }
            val oldQuery = query!!.getEncoded()
            query = if (oldQuery == null || oldQuery.length == 0) {
                Part.fromEncoded(encodedParameter)
            } else {
                Part.fromEncoded("$oldQuery&$encodedParameter")
            }
            return this
        }

        /**
         * Clears the the previously set query.
         */
        fun clearQuery(): Builder {
            return query(null as Part?)
        }

        /**
         * Constructs a Uri with the current attributes.
         *
         * @throws UnsupportedOperationException if the URI is opaque and the
         * scheme is null
         */
        fun build(): Uri? {
            val opaquePart = opaquePart
            val scheme = scheme
            return if (opaquePart != null) {
                if (scheme == null) {
                    throw UnsupportedOperationException(
                        "An opaque URI must have a scheme."
                    )
                }
                OpaqueUri(scheme, opaquePart, fragment)
            } else {
                // Hierarchical URIs should not return null for getPath().
                var path = path
                if (path == null || path == PathPart.NULL) {
                    path = PathPart.EMPTY
                } else {
                    // If we have a scheme and/or authority, the path must
                    // be absolute. Prepend it with a '/' if necessary.
                    if (hasSchemeOrAuthority()) {
                        path = PathPart.makeAbsolute(path)
                    }
                }

                val scheme = scheme ?: return null
                val authority = authority ?: return null
                if(path==null) return null
                val query = query ?: return null
                val fragment = fragment ?: return null

                HierarchicalUri(
                    scheme, authority, path, query, fragment
                )
            }
        }

        private fun hasSchemeOrAuthority(): Boolean {
            return scheme != null || (authority != null && authority !== Part.NULL)
        }

        override fun toString(): String {
            return build().toString()
        }
    }

    /**
     * Opaque URI.
     */
    private class OpaqueUri constructor(val _scheme: String?, val _ssp: Part, fragment: Part?) :
        Uri() {
        private val fragment: Part

        override fun getScheme(): String? = _scheme
        fun getSsp(): Part = _ssp
        override fun getFragment(): String? = fragment.getDecoded()

        override fun isHierarchical(): Boolean = false
        override fun isRelative(): Boolean = getScheme() == null
        override fun getEncodedSchemeSpecificPart(): String? = getSsp().getEncoded()
        override fun getSchemeSpecificPart(): String? = getSsp().getDecoded()
        override fun getAuthority(): String? = null
        override fun getEncodedAuthority(): String? = null
        override fun getPath(): String? = null
        override fun getEncodedPath(): String? = null
        override fun getQuery(): String? = null

        override fun getEncodedQuery(): String? {
            return null
        }

        override fun getEncodedFragment(): String? = fragment.getEncoded()
        override fun getPathSegments(): Array<String?> = arrayOf()
        override fun getLastPathSegment(): String? = null
        override fun getUserInfo(): String? = null
        override fun getEncodedUserInfo(): String? = null
        override fun getHost(): String? = null
        override fun getPort(): Int = -1

        @Volatile
        private var cachedString = NotCachedHolder.NOT_CACHED

        init {
            this.fragment = fragment ?: Part.NULL
        }

        override fun toString(): String {
            val cached = cachedString !== NotCachedHolder.NOT_CACHED
            if (cached) {
                return cachedString
            }
            val sb = StringBuilder()
            sb.append(getScheme()).append(':')
            sb.append(getEncodedSchemeSpecificPart())
            if (!fragment.isEmpty) {
                sb.append('#').append(fragment.getEncoded())
            }
            return sb.toString().also { cachedString = it }
        }

        override fun buildUpon(): Builder {
            return Builder()
                .scheme(getScheme())
                .opaquePart(getSsp())
                .fragment(getFragment())
        }

    }

    /**
     * Support for hierarchical URIs.
     */
    abstract class AbstractHierarchicalUri() : Uri() {
        // TODO: If we haven't parsed all of the segments already, just
        // grab the last one directly so we only allocate one string.
            override fun getLastPathSegment(): String? {
                // TODO: If we haven't parsed all of the segments already, just
                // grab the last one directly so we only allocate one string.
                val segments: Array<String?> = getPathSegments() ?: return null
                val size: Int = segments.size
                if (size == 0) {
                    return null
                }
                return segments.get(size - 1)
            }
        private var userInfo: Part? = null
        private val userInfoPart: Part
            private get() = if (userInfo == null) Part.fromEncoded(parseUserInfo()).also {
                userInfo = it
            } else userInfo!!
        override fun getEncodedUserInfo(): String? = userInfoPart.getEncoded()

        private fun parseUserInfo(): String? {
            val authority: String = getEncodedAuthority() ?: return null
            val end: Int = authority.lastIndexOf('@')
            return if (end == NOT_FOUND) null else authority.substring(0, end)
        }

        override fun getUserInfo(): String? {
            return userInfoPart.getDecoded()
        }

        @Volatile
        private var host: String = NotCachedHolder.NOT_CACHED
        override fun getHost(): String {
            val cached: Boolean = (host !== NotCachedHolder.NOT_CACHED)
            return if (cached) host else ((parseHost().also { host = (it)!! })!!)
        }

        private fun parseHost(): String? {
            val authority: String = getEncodedAuthority() ?: return null
            // Parse out user info and then port.
            val userInfoSeparator: Int = authority.lastIndexOf('@')
            val portSeparator: Int = findPortSeparator(authority)
            val encodedHost: String =
                if (portSeparator == NOT_FOUND) authority.substring(userInfoSeparator + 1) else authority.substring(
                    userInfoSeparator + 1,
                    portSeparator
                )
            return decode(encodedHost)
        }

        @Volatile
        private var port: Int = NOT_CALCULATED
        override fun getPort(): Int {
            return if (port == NOT_CALCULATED) parsePort().also { port = it } else port
        }

        private fun parsePort(): Int {
            val authority: String = getEncodedAuthority() ?: return -1
            val portSeparator: Int = findPortSeparator(authority)
            if (portSeparator == NOT_FOUND) {
                return -1
            }
            val portString: String? = decode(authority.substring(portSeparator + 1))
            try {
                return portString!!.toInt()
            } catch (e: NumberFormatException) {
                Napier.w( "$LOG : Error parsing port string.", e)
                return -1
            }
        }

        private fun findPortSeparator(authority: String?): Int {
            if (authority == null) {
                return NOT_FOUND
            }
            // Reverse search for the ':' character that breaks as soon as a char that is neither
            // a colon nor an ascii digit is encountered. Thanks to the goodness of UTF-16 encoding,
            // it's not possible that a surrogate matches one of these, so this loop can just
            // look for characters rather than care about code points.
            for (i in authority.length - 1 downTo 0) {
                val character: Int = authority[i].code
                if (':'.code == character) return i
                // Character.isDigit would include non-ascii digits
                if (character < '0'.code || character > '9'.code) return NOT_FOUND
            }
            return NOT_FOUND
        }
    }


    /**
     * Hierarchical Uri.
     */
    class HierarchicalUri constructor(
        scheme: String?,
        authority: Part,
        path: PathPart?,
        query: Part,
        fragment: Part
    ) : AbstractHierarchicalUri() {
        val _scheme : String?
        private val authority: Part
        private val path: PathPart
        private val query: Part
        private val fragment: Part

        override fun isHierarchical(): Boolean = true
        override fun isRelative(): Boolean = _scheme == null
        private var ssp: Part? = null
            private get() = if (field == null) Part.fromEncoded(makeSchemeSpecificPart()).also {
                field = it
            } else field
        override fun getEncodedSchemeSpecificPart(): String? = ssp!!.getEncoded()
        override fun getSchemeSpecificPart(): String? {
                return ssp!!.getDecoded()
            }

        /**
         * Creates the encoded scheme-specific part from its sub parts.
         */
        private fun makeSchemeSpecificPart(): String {
            val builder: StringBuilder = StringBuilder()
            appendSspTo(builder)
            return builder.toString()
        }

        private fun appendSspTo(builder: StringBuilder) {
            val encodedAuthority: String? = authority.getEncoded()
            if (encodedAuthority != null) {
                // Even if the authority is "", we still want to append "//".
                builder.append("//").append(encodedAuthority)
            }
            val encodedPath: String? = path.getEncoded()
            if (encodedPath != null) {
                builder.append(encodedPath)
            }
            if (!query.isEmpty) {
                builder.append('?').append(query.getEncoded())
            }
        }

        override fun getAuthority(): String? {
            return authority.getDecoded()
        }

        override fun getEncodedAuthority(): String? = authority.getEncoded()

        override fun getEncodedPath(): String? {
            return path.getEncoded()
        }

        override fun getPath(): String? {
            return path.getDecoded()
        }

        override fun getQuery(): String? {
            return query.getDecoded()
        }

        override fun getEncodedQuery(): String? {
            return query.getEncoded()
        }

        override fun getFragment(): String? {
            return fragment.getDecoded()
        }
        override fun getScheme(): String? {
            return _scheme
        }

        override fun getEncodedFragment(): String? {
            return fragment.getEncoded()
        }

        override fun getPathSegments(): Array<String?>? {
            return path.pathSegments?.segments
        }

        @Volatile
        var uriString: String = NotCachedHolder.NOT_CACHED

        init {
            this._scheme = scheme
            this.authority = Part.nonNull(authority)
            this.path = path ?: PathPart.NULL
            this.query = Part.nonNull(query)
            this.fragment = Part.nonNull(fragment)
        }

        override fun toString(): String {
            val cached: Boolean = (uriString !== NotCachedHolder.NOT_CACHED)
            return if (cached) uriString else (makeUriString().also { uriString = it })
        }

        private fun makeUriString(): String {
            val builder = StringBuilder()
            if (_scheme != null) {
                builder.append(_scheme).append(':')
            }
            appendSspTo(builder)
            if (!fragment.isEmpty) {
                builder.append('#').append(fragment.getEncoded())
            }
            return builder.toString()
        }

        override fun buildUpon(): Builder {
            return Builder()
                .scheme(_scheme)
                .authority(authority)
                .path(path)
                .query(query)
                .fragment(fragment)
        }

    }


}

