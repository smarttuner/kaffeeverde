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
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/navigation/navigation-common/src/main/java/androidx/navigation/NavDestination.kt
 */
package net.smarttuner.kaffeeverde.navigation

import net.smarttuner.kaffeeverde.core.Bundle
import net.smarttuner.kaffeeverde.core.Uri
import net.smarttuner.kaffeeverde.core.toHexString
import kotlin.reflect.KClass
/**
 * NavDestination represents one node within an overall navigation graph.
 *
 * Each destination is associated with a [Navigator] which knows how to navigate to this
 * particular destination.
 *
 * Destinations declare a set of [actions][putAction] that they
 * support. These actions form a navigation API for the destination; the same actions declared
 * on different destinations that fill similar roles allow application code to navigate based
 * on semantic intent.
 *
 * Each destination has a set of [arguments][arguments] that will
 * be applied when [navigating][NavController.navigate] to that destination.
 * Any default values for those arguments can be overridden at the time of navigation.
 *
 * NavDestinations should be created via [Navigator.createDestination].
 */
open class NavDestination(
    /**
     * The name associated with this destination's [Navigator].
     */
    val navigatorName: String
) {
    /**
     * This optional annotation allows tooling to offer auto-complete for the
     * `android:name` attribute. This should match the class type passed to
     * [parseClassFromName] when parsing the
     * `android:name` attribute.
     */
    @Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
    annotation class ClassType(val value: KClass<*>)
    /** @suppress */
    class DeepLinkMatch(
        val destination: NavDestination,
        @get:Suppress("NullableCollection") // Needed for nullable bundle
        val matchingArgs: Bundle?,
        private val isExactDeepLink: Boolean,
        private val hasMatchingAction: Boolean,
        private val mimeTypeMatchLevel: Int
    ) : Comparable<DeepLinkMatch> {
        override fun compareTo(other: DeepLinkMatch): Int {
            // Prefer exact deep links
            if (isExactDeepLink && !other.isExactDeepLink) {
                return 1
            } else if (!isExactDeepLink && other.isExactDeepLink) {
                return -1
            }
            if (matchingArgs != null && other.matchingArgs == null) {
                return 1
            } else if (matchingArgs == null && other.matchingArgs != null) {
                return -1
            }
            if (matchingArgs != null) {
                val sizeDifference = matchingArgs.size - other.matchingArgs!!.size
                if (sizeDifference > 0) {
                    return 1
                } else if (sizeDifference < 0) {
                    return -1
                }
            }
            if (hasMatchingAction && !other.hasMatchingAction) {
                return 1
            } else if (!hasMatchingAction && other.hasMatchingAction) {
                return -1
            }
            return mimeTypeMatchLevel - other.mimeTypeMatchLevel
        }
    }
    /**
     * Gets the [NavGraph] that contains this destination. This will be set when a
     * destination is added to a NavGraph via [NavGraph.addDestination].
     */
    var parent: NavGraph? = null
    private var idName: String? = null
    /**
     * The descriptive label of this destination.
     */
    var label: CharSequence? = null
    private val deepLinks = mutableListOf<NavDeepLink>()
    private val actions = mutableListOf<NavAction>()
    private var _arguments: MutableMap<String, NavArgument> = mutableMapOf()
    /**
     * The arguments supported by this destination. Returns a read-only map of argument names
     * to [NavArgument] objects that can be used to check the type, default value
     * and nullability of the argument.
     *
     * To add and remove arguments for this NavDestination
     * use [addArgument] and [removeArgument].
     * @return Read-only map of argument names to arguments.
     */
    val arguments: Map<String, NavArgument>
        get() = _arguments.toMap()
    /**
     * NavDestinations should be created via [Navigator.createDestination].
     *
     * This constructor requires that the given Navigator has a [Navigator.Name] annotation.
     */
    constructor(navigator: Navigator<out NavDestination>) : this(
        NavigatorProvider.getNameForNavigator(
            navigator::class
        )
    )
    /**
     * The destination's unique ID. This should be an ID resource generated by
     * the Android resource system.
     */
    var id: Int = 0
        set(id) {
            field = id
            idName = null
        }
    /**
     * The destination's unique route. Setting this will also update the [id] of the destinations
     * so custom destination ids should only be set after setting the route.
     *
     * @return this destination's route, or null if no route is set
     *
     * @throws IllegalArgumentException is the given route is empty
     */
    var route: String? = null
        set(route) {
            if (route == null) {
                id = 0
            } else {
                require(route.isNotBlank()) { "Cannot have an empty route" }
                val internalRoute = createRoute(route)
                id = internalRoute.hashCode()
                addDeepLink(internalRoute)
            }
            deepLinks.remove(deepLinks.firstOrNull { it.uriPattern == createRoute(field) })
            field = route
        }
    /**
     * @hide
     */
    open val displayName: String
        get() = idName ?: id.toString()
    /**
     * Checks the given deep link [Uri], and determines whether it matches a Uri pattern added
     * to the destination by a call to [addDeepLink] . It returns `true`
     * if the deep link is a valid match, and `false` otherwise.
     *
     * This should be called prior to [NavController.navigate] to ensure the deep link
     * can be navigated to.
     *
     * @param deepLink to the destination reachable from the current NavGraph
     * @return True if the deepLink exists for the destination.
     * @see NavDestination.addDeepLink
     * @see NavController.navigate
     * @see NavDestination.hasDeepLink
     */
    open fun hasDeepLink(deepLink: Uri): Boolean {
        return hasDeepLink(NavDeepLinkRequest(deepLink, null, null))
    }
    /**
     * Checks the given [NavDeepLinkRequest], and determines whether it matches a
     * [NavDeepLink] added to the destination by a call to
     * [addDeepLink]. It returns `true` if the request is a valid
     * match, and `false` otherwise.
     *
     * This should be called prior to [NavController.navigate] to
     * ensure the deep link can be navigated to.
     *
     * @param deepLinkRequest to the destination reachable from the current NavGraph
     * @return True if the deepLink exists for the destination.
     * @see NavDestination.addDeepLink
     * @see NavController.navigate
     */
    open fun hasDeepLink(deepLinkRequest: NavDeepLinkRequest): Boolean {
        return matchDeepLink(deepLinkRequest) != null
    }
    /**
     * Add a deep link to this destination. Matching Uris sent to
     * [NavController.handleDeepLink] or [NavController.navigate] will
     * trigger navigating to this destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     *
     * - Uris without a scheme are assumed as http and https. For example,
     * `www.example.com` will match `http://www.example.com` and
     * `https://www.example.com`.
     * - Placeholders in the form of `{placeholder_name}` matches 1 or more
     * characters. The parsed value of the placeholder will be available in the arguments
     * [Bundle] with a key of the same name. For example,
     * `http://www.example.com/users/{id}` will match
     * `http://www.example.com/users/4`.
     * - The `.*` wildcard can be used to match 0 or more characters.
     *
     * These Uris can be declared in your navigation XML files by adding one or more
     * `<deepLink app:uri="uriPattern" />` elements as
     * a child to your destination.
     *
     * Deep links added in navigation XML files will automatically replace instances of
     * `${applicationId}` with the applicationId of your app.
     * Programmatically added deep links should use [Context.getPackageName] directly
     * when constructing the uriPattern.
     * @param uriPattern The uri pattern to add as a deep link
     * @see NavController.handleDeepLink
     * @see NavController.navigate
     * @see NavDestination.addDeepLink
     */
    fun addDeepLink(uriPattern: String) {
        addDeepLink(NavDeepLink.Builder().setUriPattern(uriPattern).build())
    }
    /**
     * Add a deep link to this destination. Uris that match the given [NavDeepLink] uri
     * sent to [NavController.handleDeepLink] or
     * [NavController.navigate] will trigger navigating to this
     * destination.
     *
     * In addition to a direct Uri match, the following features are supported:
     *
     * Uris without a scheme are assumed as http and https. For example,
     * `www.example.com` will match `http://www.example.com` and
     * `https://www.example.com`.
     * Placeholders in the form of `{placeholder_name}` matches 1 or more
     * characters. The String value of the placeholder will be available in the arguments
     * [Bundle] with a key of the same name. For example,
     * `http://www.example.com/users/{id}` will match
     * `http://www.example.com/users/4`.
     * The `.*` wildcard can be used to match 0 or more characters.
     *
     * These Uris can be declared in your navigation XML files by adding one or more
     * `<deepLink app:uri="uriPattern" />` elements as
     * a child to your destination.
     *
     * Custom actions and mimetypes are also supported by [NavDeepLink] and can be declared
     * in your navigation XML files by adding
     * `<app:action="android.intent.action.SOME_ACTION" />` or
     * `<app:mimetype="type/subtype" />` as part of your deepLink declaration.
     *
     * Deep link Uris, actions, and mimetypes added in navigation XML files will automatically
     * replace instances of `${applicationId}` with the applicationId of your app.
     * Programmatically added deep links should use [Context.getPackageName] directly
     * when constructing the uriPattern.
     *
     * When matching deep links for calls to [NavController.handleDeepLink] or
     * [NavController.navigate] the order of precedence is as follows:
     * the deep link with the most matching arguments will be chosen, followed by the deep link
     * with a matching action, followed by the best matching mimeType (e.i. when matching
     * mimeType image/jpg: image/ * > *\/jpg > *\/ *).
     * @param navDeepLink The NavDeepLink to add as a deep link
     * @see NavController.handleDeepLink
     * @see NavController.navigate
     */
    fun addDeepLink(navDeepLink: NavDeepLink) {
        val missingRequiredArguments =
            arguments.filterValues { !it.isNullable && !it.isDefaultValuePresent }
                .keys
                .filter { it !in navDeepLink.argumentsNames }
        require(missingRequiredArguments.isEmpty()) {
            "Deep link ${navDeepLink.uriPattern} can't be used to open destination $this.\n" +
                    "Following required arguments are missing: $missingRequiredArguments"
        }
        deepLinks.add(navDeepLink)
    }
    /**
     * Determines if this NavDestination has a deep link matching the given Uri.
     * @param navDeepLinkRequest The request to match against all deep links added in
     * [addDeepLink]
     * @return The matching [NavDestination] and the appropriate [Bundle] of arguments
     * extracted from the Uri, or null if no match was found.
     * @suppress
     */
    open fun matchDeepLink(navDeepLinkRequest: NavDeepLinkRequest): DeepLinkMatch? {
        if (deepLinks.isEmpty()) {
            return null
        }
        var bestMatch: DeepLinkMatch? = null
        for (deepLink in deepLinks) {
            val uri = navDeepLinkRequest.uri
            val matchingArguments =
                if (uri != null) deepLink.getMatchingArguments(uri, arguments) else null
            val requestAction = navDeepLinkRequest.action
            val matchingAction = requestAction != null && requestAction ==
                    deepLink.action
            val mimeType = navDeepLinkRequest.mimeType
            val mimeTypeMatchLevel =
                if (mimeType != null) deepLink.getMimeTypeMatchRating(mimeType) else -1
            if (matchingArguments != null || matchingAction || mimeTypeMatchLevel > -1) {
                val newMatch = DeepLinkMatch(
                    this, matchingArguments,
                    deepLink.isExactDeepLink, matchingAction, mimeTypeMatchLevel
                )
                if (bestMatch == null || newMatch > bestMatch) {
                    bestMatch = newMatch
                }
            }
        }
        return bestMatch
    }
    /**
     * Build an array containing the hierarchy from the root down to this destination.
     *
     * @param previousDestination the previous destination we are starting at
     * @return An array containing all of the ids from the previous destination (or the root of
     * the graph if null) to this destination
     * @suppress
     */

    fun buildDeepLinkIds(previousDestination: NavDestination? = null): IntArray {
        val hierarchy = ArrayDeque<NavDestination>()
        var current: NavDestination? = this
        do {
            val parent = current!!.parent
            if (
            // If the current destination is a sibling of the previous, just add it straightaway
                previousDestination?.parent != null &&
                previousDestination.parent!!.findNode(current.id) === current
            ) {
                hierarchy.addFirst(current)
                break
            }
            if (parent == null || parent.startDestinationId != current.id) {
                hierarchy.addFirst(current)
            }
            if (parent == previousDestination) {
                break
            }
            current = parent
        } while (current != null)
        return hierarchy.toList().map { it.id }.toIntArray()
    }
    /**
     * @return Whether this NavDestination supports outgoing actions
     * @see NavDestination.putAction
     * @suppress
     */
    open fun supportsActions(): Boolean {
        return true
    }
    /**
     * Returns the [NavAction] for the given action ID. This will recursively check the
     * [parent][getParent] of this destination if the action destination is not found in
     * this destination.
     *
     * @param id action ID to fetch
     * @return the [NavAction] mapped to the given action id, or null if one has not been set
     */
    fun getAction(id: Int): NavAction? {
        val destination = if (actions.isEmpty()) null else actions[id]
        // Search the parent for the given action if it is not found in this destination
        return destination ?: parent?.run { getAction(id) }
    }
    /**
     * Creates a [NavAction] for the given [destId] and associates it with the [actionId].
     *
     * @param actionId action ID to bind
     * @param destId destination ID for the given action
     */
    fun putAction(actionId: Int, destId: Int) {
        putAction(actionId, NavAction(destId))
    }
    /**
     * Sets the [NavAction] destination for an action ID.
     *
     * @param actionId action ID to bind
     * @param action action to associate with this action ID
     * @throws UnsupportedOperationException this destination is considered a terminal destination
     * and does not support actions
     */
    fun putAction(actionId: Int, action: NavAction) {
        if (!supportsActions()) {
            throw UnsupportedOperationException(
                "Cannot add action $actionId to $this as it does not support actions, " +
                        "indicating that it is a terminal destination in your navigation graph and " +
                        "will never trigger actions."
            )
        }
        require(actionId != 0) { "Cannot have an action with actionId 0" }
        actions.add(actionId, action)
    }
    /**
     * Unsets the [NavAction] for an action ID.
     *
     * @param actionId action ID to remove
     */
    fun removeAction(actionId: Int) {
        actions.removeAt(actionId)
    }
    /**
     * Sets an argument type for an argument name
     *
     * @param argumentName argument object to associate with destination
     * @param argument argument object to associate with destination
     */
    fun addArgument(argumentName: String, argument: NavArgument) {
        _arguments[argumentName] = argument
    }
    /**
     * Unsets the argument type for an argument name.
     *
     * @param argumentName argument to remove
     */
    fun removeArgument(argumentName: String) {
        _arguments.remove(argumentName)
    }
    /**
     * Combines the default arguments for this destination with the arguments provided
     * to construct the final set of arguments that should be used to navigate
     * to this destination.
     * @suppress
     */
    @Suppress("NullableCollection") // Needed for nullable bundle
    fun addInDefaultArgs(args: Bundle?): Bundle? {
        if (args == null && _arguments.isNullOrEmpty()) {
            return null
        }
        val defaultArgs = Bundle()
        for ((key, value) in _arguments) {
            value.putDefaultValue(key, defaultArgs)
        }
        if (args != null) {
            defaultArgs.putAll(args)
            for ((key, value) in _arguments) {
                require(value.verify(key, defaultArgs)) {
                    "Wrong argument type for '$key' in argument bundle. ${value.type.name} " +
                            "expected."
                }
            }
        }
        return defaultArgs
    }
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(this::class.simpleName)
        sb.append("(")
        if (idName == null) {
            sb.append("0x")
            sb.append(id.toHexString())
        } else {
            sb.append(idName)
        }
        sb.append(")")
        if (!route.isNullOrBlank()) {
            sb.append(" route=")
            sb.append(route)
        }
        if (label != null) {
            sb.append(" label=")
            sb.append(label)
        }
        return sb.toString()
    }
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is NavDestination) return false
        val equalDeepLinks = deepLinks.intersect(other.deepLinks.toSet()).size == deepLinks.size
        val equalActions = actions.size == other.actions.size &&
                actions.iterator().asSequence().all { other.actions.contains(it) } &&
                other.actions.iterator().asSequence().all { actions.contains(it) }
        val equalArguments = arguments.size == other.arguments.size &&
                arguments.asSequence().all {
                    other.arguments.containsKey(it.key) &&
                            other.arguments[it.key] == it.value
                } &&
                other.arguments.asSequence().all {
                    arguments.containsKey(it.key) &&
                            arguments[it.key] == it.value
                }
        return id == other.id &&
                route == other.route &&
                equalDeepLinks &&
                equalActions &&
                equalArguments
    }
    @Suppress("DEPRECATION")
    override fun hashCode(): Int {
        var result = id
        result = 31 * result + route.hashCode()
        deepLinks.forEach {
            result = 31 * result + it.uriPattern.hashCode()
            result = 31 * result + it.action.hashCode()
            result = 31 * result + it.mimeType.hashCode()
        }
        actions.iterator().forEach { value ->
            result = 31 * result + value.destinationId
            result = 31 * result + value.navOptions.hashCode()
            value.defaultArguments?.keySet?.forEach {
                it?.let {
                    result = 31 * result + value.defaultArguments!![it].hashCode()
                }
            }
        }
        arguments.keys.forEach {
            result = 31 * result + it.hashCode()
            result = 31 * result + arguments[it].hashCode()
        }
        return result
    }

    companion object {
         /**
         * @hide
         */

         fun createRoute(route: String?): String =
            if (route != null) "android-app://androidx.navigation/$route" else ""
        /**
         * Provides a sequence of the NavDestination's hierarchy. The hierarchy starts with this
         * destination itself and is then followed by this destination's [NavDestination.parent], then that
         * graph's parent, and up the hierarchy until you've reached the root navigation graph.
         */

        val NavDestination.hierarchy: Sequence<NavDestination>
            get() = generateSequence(this) { it.parent }
    }
}