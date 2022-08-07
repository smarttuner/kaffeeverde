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
/*
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/navigation/navigation-common/src/main/java/androidx/navigation/NavDeepLink.kt
 *
 */
package net.smarttuner.kaffeeverde.navigation

import net.smarttuner.kaffeeverde.core.Bundle
import net.smarttuner.kaffeeverde.core.Uri
import net.smarttuner.kaffeeverde.core.util.regex.Pattern

/**
 * NavDeepLink encapsulates the parsing and matching of a navigation deep link.
 *
 * This should be added to a [NavDestination] using
 * [NavDestination.addDeepLink].
 */
class NavDeepLink internal constructor(
    /**
     * The uri pattern from the NavDeepLink.
     *
     * @see NavDeepLinkRequest.uri
     */
    val uriPattern: String?,
    /**
     * The action from the NavDeepLink.
     *
     * @see NavDeepLinkRequest.action
     */
    val action: String?,
    /**
     * The mimeType from the NavDeepLink.
     *
     * @see NavDeepLinkRequest.mimeType
     */
    val mimeType: String?
) {
    private val arguments = mutableListOf<String>()
    private val paramArgMap = mutableMapOf<String, ParamQuery>()
    private var patternFinalRegexString: String? = null
    private val regex by lazy {
        patternFinalRegexString?.let { Regex(it, RegexOption.IGNORE_CASE) }
    }
    private var isParameterizedQuery = false
    private var isSingleQueryParamValueOnly = false
    private var mimeTypeFinalRegexString: String? = null
    private val mimeTypeRegex by lazy {
        mimeTypeFinalRegexString?.let { Regex(it) }
    }
    /** Arguments present in the deep link, including both path and query arguments. */
    internal val argumentsNames: List<String>
        get() = arguments + paramArgMap.values.flatMap { it.arguments }
    var isExactDeepLink: Boolean = false
        internal set
    /** @suppress */

    constructor(uri: String) : this(uri, null, null)
    private fun buildPathRegex(
        uri: String,
        uriRegex: StringBuilder,
        fillInPattern: Regex
    ): Boolean {
        val matcher = fillInPattern.findAll(uri)
        var appendPos = 0
        // Track whether this is an exact deep link
        var exactDeepLink = !uri.contains(".*")
        for (result in matcher) {
            val argName = result.groupValues[1]
            arguments.add(argName)
            // Use Pattern.quote() to treat the input string as a literal
            uriRegex.append(Pattern.quote(uri.substring(appendPos,result.range.first)))
            uriRegex.append("([^/]+?)")
            appendPos = result.range.last +1
            exactDeepLink = false
        }
        if (appendPos < uri.length) {
            // Use Pattern.quote() to treat the input string as a literal
            uriRegex.append(Pattern.quote(uri.substring(appendPos)))
        }
        // Match either the end of string if all params are optional or match the
        // question mark (or pound symbol) and 0 or more characters after it
        // We do not use '.*' here because the finalregex would replace it with a quoted
        // version below.
        uriRegex.append("($|(\\?(.)*)|(\\#(.)*))")
        return exactDeepLink
    }
    internal fun matches(uri: Uri): Boolean {
        return matches(NavDeepLinkRequest(uri, null, null))
    }
    internal fun matches(deepLinkRequest: NavDeepLinkRequest): Boolean {
        if (!matchUri(deepLinkRequest.uri)) {
            return false
        }
        return if (!matchAction(deepLinkRequest.action)) {
            false
        } else matchMimeType(deepLinkRequest.mimeType)
    }
    private fun matchUri(uri: Uri?): Boolean {
        // If the null status of both are not the same return false.
        return if (uri == null == (regex != null)) {
            false
        } else uri == null || regex!!.matches(uri.toString())
        // If both are null return true, otherwise see if they match
    }
    private fun matchAction(action: String?): Boolean {
        // If the null status of both are not the same return false.
        return if (action == null == (this.action != null)) {
            false
        } else action == null || this.action == action
        // If both are null return true, otherwise see if they match
    }
    private fun matchMimeType(mimeType: String?): Boolean {
        // If the null status of both are not the same return false.
        return if (mimeType == null == (this.mimeType != null)) {
            false
        } else mimeType == null || mimeTypeRegex!!.matches(mimeType)
        // If both are null return true, otherwise see if they match
    }
    /** @suppress */

    fun getMimeTypeMatchRating(mimeType: String): Int {
        return if (this.mimeType == null || !mimeTypeRegex!!.matches(mimeType)) {
            -1
        } else MimeType(this.mimeType)
            .compareTo(MimeType(mimeType))
    }
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "NullableCollection")
    /** Regex has no nullability for the regex parameter
     * @suppress
     */
    fun getMatchingArguments(
        deepLink: Uri,
        arguments: Map<String, NavArgument?>
    ): Bundle? {
        val matcher = regex?.find(deepLink.toString()) ?: return null
        val bundle = Bundle()
        val size = this.arguments.size
        for (index in 0 until size) {
            val argumentName = this.arguments[index]
            val value = Uri.decode(matcher.groupValues[index + 1]) ?: return null
            val argument = arguments[argumentName]
            try {
                if (parseArgument(bundle, argumentName, value, argument)) {
                    return null
                }
            } catch (e: IllegalArgumentException) {
                // Failed to parse means this isn't a valid deep link
                // for the given URI - i.e., the URI contains a non-integer
                // value for an integer argument
                return null
            }
        }
        if (isParameterizedQuery) {
            for (paramName in paramArgMap.keys) {
                var argMatcher: MatchResult? = null
                val storedParam = paramArgMap[paramName] ?: continue
                val paramRegex = storedParam.paramRegex ?: continue
                var inputParams = deepLink.getQueryParameters(paramName) ?: continue
                if (isSingleQueryParamValueOnly) {
                    // If the deep link contains a single query param with no value,
                    // we will treat everything after the '?' as the input parameter
                    val deepLinkString = deepLink.toString()
                    val argValue = deepLinkString.substringAfter('?')
                    if (argValue != deepLinkString) {
                        inputParams = listOf(argValue)
                    }
                }
                // If the input query param is repeated, we want to do all the
                // matching and parsing for each value
                for (inputParam in inputParams) {
                    if (inputParam != null) {
                        // Match the input arguments with the saved regex
                        argMatcher = Regex(
                            paramRegex
                        ).matchEntire(inputParam) ?: return null
                    }
                    val queryParamBundle = Bundle()
                    try {
                        // Params could have multiple arguments, we need to handle them all
                        for (index in 0 until storedParam.size()) {
                            var value: String? = null
                            if (argMatcher != null) {
                                value = argMatcher.groupValues[index + 1]
                            }
                            val argName = storedParam.getArgumentName(index)
                            val argument = arguments[argName]
                            // If we have a repeated param, treat it as such
                            if (parseArgumentForRepeatedParam(bundle, argName, value, argument)) {
                                // Passing in a value the exact same as the placeholder will be treated the
                                // as if no value was passed, being replaced if it is optional or throwing an
                                // error if it is required.
                                if (value != null && value != "{$argName}" &&
                                    parseArgument(queryParamBundle, argName, value, argument)
                                ) {
                                    return null
                                }
                            }
                        }
                        bundle.putAll(queryParamBundle)
                    } catch (e: IllegalArgumentException) {
                        // Failed to parse means that at least one of the arguments that were supposed
                        // to fill in the query parameter was not valid and therefore, we will exclude
                        // that particular parameter from the argument bundle.
                    }
                }
            }
        }
        // Check that all required arguments are present in bundle
        for ((argName, argument) in arguments.entries) {
            val argumentIsRequired = argument != null && !argument.isNullable &&
                    !argument.isDefaultValuePresent
            if (argumentIsRequired && !bundle.containsKey(argName)) return null
        }
        return bundle
    }
    private fun parseArgument(
        bundle: Bundle,
        name: String,
        value: String,
        argument: NavArgument?
    ): Boolean {
        if (argument != null) {
            val type = argument.type
            type.parseAndPut(bundle, name, value)
        } else {
            bundle.putString(name, value)
        }
        return false
    }
    private fun parseArgumentForRepeatedParam(
        bundle: Bundle,
        name: String,
        value: String?,
        argument: NavArgument?
    ): Boolean {
        if (!bundle.containsKey(name)) {
            return true
        }
        if (argument != null) {
            val type = argument.type
            val previousValue = type[bundle, name]
            type.parseAndPut(bundle, name, value, previousValue)
        }
        return false
    }
    /**
     * Used to maintain query parameters and the mArguments they match with.
     */
    private class ParamQuery {
        var paramRegex: String? = null
        val arguments = mutableListOf<String>()
        fun addArgumentName(name: String) {
            arguments.add(name)
        }
        fun getArgumentName(index: Int): String {
            return arguments[index]
        }
        fun size(): Int {
            return arguments.size
        }
    }
    private class MimeType(mimeType: String) : Comparable<MimeType> {
        var type: String
        var subType: String
        override fun compareTo(other: MimeType): Int {
            var result = 0
            // matching just subtypes is 1
            // matching just types is 2
            // matching both is 3
            if (type == other.type) {
                result += 2
            }
            if (subType == other.subType) {
                result++
            }
            return result
        }
        init {
            val typeAndSubType =
                mimeType.split("/".toRegex()).dropLastWhile { it.isEmpty() }
            type = typeAndSubType[0]
            subType = typeAndSubType[1]
        }
    }
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is NavDeepLink) return false
        return uriPattern == other.uriPattern &&
                action == other.action &&
                mimeType == other.mimeType
    }
    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + uriPattern.hashCode()
        result = 31 * result + action.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
    /**
     * A builder for constructing [NavDeepLink] instances.
     */
    class Builder {
        /** @suppress */

        constructor()
        private var uriPattern: String? = null
        private var action: String? = null
        private var mimeType: String? = null
        /**
         * Set the uri pattern for the [NavDeepLink].
         *
         * @param uriPattern The uri pattern to add to the NavDeepLink
         *
         * @return This builder.
         */
        fun setUriPattern(uriPattern: String): Builder {
            this.uriPattern = uriPattern
            return this
        }
        /**
         * Set the action for the [NavDeepLink].
         *
         * @throws IllegalArgumentException if the action is empty.
         *
         * @param action the intent action for the NavDeepLink
         *
         * @return This builder.
         */
        fun setAction(action: String): Builder {
            // if the action given at runtime is empty we should throw
            require(action.isNotEmpty()) { "The NavDeepLink cannot have an empty action." }
            this.action = action
            return this
        }
        /**
         * Set the mimeType for the [NavDeepLink].
         *
         * @param mimeType the mimeType for the NavDeepLink
         *
         * @return This builder.
         */
        fun setMimeType(mimeType: String): Builder {
            this.mimeType = mimeType
            return this
        }
        /**
         * Build the [NavDeepLink] specified by this builder.
         *
         * @return the newly constructed NavDeepLink.
         */
        fun build(): NavDeepLink {
            return NavDeepLink(uriPattern, action, mimeType)
        }
        internal companion object {
            /**
             * Creates a [NavDeepLink.Builder] with a set uri pattern.
             *
             * @param uriPattern The uri pattern to add to the NavDeepLink
             * @return a [Builder] instance
             */
            
            fun fromUriPattern(uriPattern: String): Builder {
                val builder = Builder()
                builder.setUriPattern(uriPattern)
                return builder
            }
            /**
             * Creates a [NavDeepLink.Builder] with a set action.
             *
             * @throws IllegalArgumentException if the action is empty.
             *
             * @param action the intent action for the NavDeepLink
             * @return a [Builder] instance
             */
            
            fun fromAction(action: String): Builder {
                // if the action given at runtime is empty we should throw
                require(action.isNotEmpty()) { "The NavDeepLink cannot have an empty action." }
                val builder = Builder()
                builder.setAction(action)
                return builder
            }
            /**
             * Creates a [NavDeepLink.Builder] with a set mimeType.
             *
             * @param mimeType the mimeType for the NavDeepLink
             * @return a [Builder] instance
             */
            
            fun fromMimeType(mimeType: String): Builder {
                val builder = Builder()
                builder.setMimeType(mimeType)
                return builder
            }
        }
    }
    private companion object {
        private val SCHEME_PATTERN = Regex("^[a-zA-Z]+[+\\w\\-.]*:")
    }
    init {
        if (uriPattern != null) {
            val parameterizedUri = Uri.parse(uriPattern)
            isParameterizedQuery = parameterizedUri?.getQuery() != null
            val uriRegex = StringBuilder("^")
            if (!SCHEME_PATTERN.containsMatchIn(uriPattern)) {
                uriRegex.append("http[s]?://")
            }
            @Suppress("RegExpRedundantEscape")
            val fillInPattern = Regex("\\{(.+?)\\}")
            if (isParameterizedQuery) {
                var matcher = Regex("(\\?)").find(uriPattern)
                if (matcher!=null) {
                    isExactDeepLink = buildPathRegex(
                        uriPattern.substring(0, matcher.range.first),
                        uriRegex,
                        fillInPattern
                    )
                }
                parameterizedUri?.getQueryParameterNames()?.iterator()?.forEach { paramName ->
                    if(paramName==null) return@forEach
                    val argRegex = StringBuilder()
                    val queryParams = parameterizedUri.getQueryParameters(paramName)
                    require(queryParams!= null && queryParams.size <= 1) {
                        "Query parameter $paramName must only be present once in $uriPattern." +
                                "To support repeated query parameters, use an array type for your" +
                                "argument and the pattern provided in your URI will be used to" +
                                "parse each query parameter instance."
                    }
                    val queryParam = queryParams.firstOrNull()
                        ?: paramName.apply { isSingleQueryParamValueOnly = true }
                    var results = fillInPattern.findAll(queryParam)
                    var appendPos = 0
                    val param = ParamQuery()
                    // Build the regex for each query param
                    results.iterator().forEach { result ->
                        param.addArgumentName(result.groupValues[1])
                        argRegex.append(
                            Pattern.quote(
                                queryParam.substring(
                                    appendPos,
                                    result.range.first
                                )
                            )
                        )
                        argRegex.append("(.+?)?")
                        appendPos = result.range.last + 1
                    }
                    if (appendPos < queryParam.length) {
                        argRegex.append(
                            (Pattern.quote(queryParam.substring(appendPos)))
                        )
                    }
                    // Save the regex with wildcards unquoted, and add the param to the map with its
                    // name as the key
                    param.paramRegex = argRegex.toString().replace(".*", "\\E.*\\Q")
                    paramArgMap[paramName] = param
                }
            } else {
                isExactDeepLink = buildPathRegex(uriPattern, uriRegex, fillInPattern)
            }
            // Since we've used Pattern.quote() above, we need to
            // specifically escape any .* instances to ensure
            // they are still treated as wildcards in our final regex
            patternFinalRegexString = uriRegex.toString().replace(".*", "\\E.*\\Q")
        }
        if (mimeType != null) {
            val mimeTypePattern = Regex("^[\\s\\S]+/[\\s\\S]+$")
            val mimeTypeMatcher = mimeTypePattern.matches(mimeType)
            require(!mimeTypeMatcher) {
                "The given mimeType $mimeType does not match to required \"type/subtype\" format"
            }
            // get the type and subtype of the mimeType
            val splitMimeType = MimeType(
                mimeType
            )
            // the matching pattern can have the exact name or it can be wildcard literal (*)
            val mimeTypeRegex = "^(${splitMimeType.type}|[*]+)/(${splitMimeType.subType}|[*]+)$"
            // if the deep link type or subtype is wildcard, allow anything
            mimeTypeFinalRegexString = mimeTypeRegex.replace("*|[*]", "[\\s\\S]")
        }
    }
}