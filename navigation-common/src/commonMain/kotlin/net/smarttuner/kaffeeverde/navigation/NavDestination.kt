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

import androidx.annotation.RestrictTo
import androidx.collection.SparseArrayCompat
import androidx.collection.valueIterator
import net.smarttuner.kaffeeverde.core.Bundle
import net.smarttuner.kaffeeverde.core.Uri
import net.smarttuner.kaffeeverde.core.annotation.IdRes
import net.smarttuner.kaffeeverde.core.keySet
import net.smarttuner.kaffeeverde.core.toHexString
import net.smarttuner.kaffeeverde.core.toUri
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
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
public open class NavDestination(
    /**
     * The name associated with this destination's [Navigator].
     */
    public val navigatorName: String
) {
    /**
     * This optional annotation allows tooling to offer auto-complete for the
     * `android:name` attribute. This should match the class type passed to
     * [parseClassFromName] when parsing the
     * `android:name` attribute.
     */
    @kotlin.annotation.Retention(AnnotationRetention.BINARY)
    @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
    public annotation class ClassType(val value: KClass<*>)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class DeepLinkMatch(
        public val destination: NavDestination,
        @get:Suppress("NullableCollection") // Needed for nullable bundle
        public val matchingArgs: Bundle?,
        private val isExactDeepLink: Boolean,
        private val matchingPathSegments: Int,
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
            // Then prefer most exact match path segments
            val pathSegmentDifference = matchingPathSegments - other.matchingPathSegments
            if (pathSegmentDifference > 0) {
                return 1
            } else if (pathSegmentDifference < 0) {
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
        /**
         * Returns true if all args from [DeepLinkMatch.matchingArgs] can be found within
         * the [arguments].
         *
         * This returns true in these edge cases:
         * 1. If the [arguments] contain more args than [DeepLinkMatch.matchingArgs].
         * 2. If [DeepLinkMatch.matchingArgs] is empty
         * 3. Argument has null value in both [DeepLinkMatch.matchingArgs] and [arguments]
         * i.e. arguments/params with nullable values
         *
         * @param [arguments] The arguments to match with the matchingArgs stored in this
         * DeepLinkMatch.
         */
        public fun hasMatchingArgs(arguments: Bundle?): Boolean {
            if (arguments == null || matchingArgs == null) return false
            matchingArgs.keySet.forEach { key ->
                // the arguments must at least contain every argument stored in this deep link
                if (!arguments.containsKey(key)) return false
                val type = destination.arguments[key]?.type
                val matchingArgValue = type?.get(matchingArgs, key)
                val entryArgValue = type?.get(arguments, key)
                // fine if both argValues are null, i.e. arguments/params with nullable values
                if (matchingArgValue != entryArgValue) return false
            }
            return true
        }
    }
    /**
     * Gets the [NavGraph] that contains this destination. This will be set when a
     * destination is added to a NavGraph via [NavGraph.addDestination].
     */
    public var parent: NavGraph? = null
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public set
    private var idName: String? = null
    /**
     * The descriptive label of this destination.
     */
    public var label: CharSequence? = null
    private val deepLinks = mutableListOf<NavDeepLink>()
    private val actions: SparseArrayCompat<NavAction> = SparseArrayCompat()
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
    public val arguments: Map<String, NavArgument>
        get() = _arguments.toMap()
    /**
     * NavDestinations should be created via [Navigator.createDestination].
     *
     * This constructor requires that the given Navigator has a [Navigator.Name] annotation.
     */
    public constructor(navigator: Navigator<out NavDestination>) : this(
        NavigatorProvider.getNameForNavigator(
            navigator::class
        )
    )
    /**
     * The destination's unique ID. This should be an ID resource generated by
     * the Android resource system.
     */
    @get:IdRes
    public var id: Int = 0
        set(@IdRes id) {
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
    public var route: String? = null
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
    public open val displayName: String
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
    public open fun hasDeepLink(deepLink: Uri): Boolean {
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
    public open fun hasDeepLink(deepLinkRequest: NavDeepLinkRequest): Boolean {
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
    public fun addDeepLink(uriPattern: String) {
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
    public fun addDeepLink(navDeepLink: NavDeepLink) {
        val missingRequiredArguments = arguments.missingRequiredArguments { key ->
            key !in navDeepLink.argumentsNames
        }
        require(missingRequiredArguments.isEmpty()) {
            "Deep link ${navDeepLink.uriPattern} can't be used to open destination $this.\n" +
                "Following required arguments are missing: $missingRequiredArguments"
        }
        deepLinks.add(navDeepLink)
    }
    /**
     * Determines if this NavDestination has a deep link of this route.
     *
     * @param [route] The route to match against this [NavDestination.route]
     * @return The matching [DeepLinkMatch], or null if no match was found.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun matchDeepLink(route: String): DeepLinkMatch? {
        val request = NavDeepLinkRequest.Builder.fromUri(createRoute(route).toUri()).build()
        val matchingDeepLink = if (this is NavGraph) {
            matchDeepLinkExcludingChildren(request)
        } else {
            matchDeepLink(request)
        }
        return matchingDeepLink
    }
    /**
     * Determines if this NavDestination has a deep link matching the given Uri.
     * @param navDeepLinkRequest The request to match against all deep links added in
     * [addDeepLink]
     * @return The matching [NavDestination] and the appropriate [Bundle] of arguments
     * extracted from the Uri, or null if no match was found.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun matchDeepLink(navDeepLinkRequest: NavDeepLinkRequest): DeepLinkMatch? {
        if (deepLinks.isEmpty()) {
            return null
        }
        var bestMatch: DeepLinkMatch? = null
        for (deepLink in deepLinks) {
            val uri = navDeepLinkRequest.uri
            // includes matching args for path, query, and fragment
            val matchingArguments =
                if (uri != null) deepLink.getMatchingArguments(uri, arguments) else null
            val matchingPathSegments = deepLink.calculateMatchingPathSegments(uri)
            val requestAction = navDeepLinkRequest.action
            val matchingAction = requestAction != null && requestAction ==
                deepLink.action
            val mimeType = navDeepLinkRequest.mimeType
            val mimeTypeMatchLevel =
                if (mimeType != null) deepLink.getMimeTypeMatchRating(mimeType) else -1
            if (matchingArguments != null || ((matchingAction || mimeTypeMatchLevel > -1) &&
                        (uri != null && hasRequiredArguments(deepLink, uri, arguments)))
            ) {
                val newMatch = DeepLinkMatch(
                    this, matchingArguments,
                    deepLink.isExactDeepLink, matchingPathSegments, matchingAction,
                    mimeTypeMatchLevel
                )
                if (bestMatch == null || newMatch > bestMatch) {
                    bestMatch = newMatch
                }
            }
        }
        return bestMatch
    }
    private fun hasRequiredArguments(
        deepLink: NavDeepLink,
        uri: Uri,
        arguments: Map<String, NavArgument>
    ): Boolean {
        val matchingArgs = deepLink.getMatchingArguments(uri, arguments) ?: return false
        val missingRequiredArguments = arguments.missingRequiredArguments { key ->
            !matchingArgs.containsKey(key)
        }
        return missingRequiredArguments.isEmpty()
    }
    /**
     * Build an array containing the hierarchy from the root down to this destination.
     *
     * @param previousDestination the previous destination we are starting at
     * @return An array containing all of the ids from the previous destination (or the root of
     * the graph if null) to this destination
     */
    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun buildDeepLinkIds(previousDestination: NavDestination? = null): IntArray {
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
     * Returns true if the [NavBackStackEntry.destination] contains the route.
     *
     * The route may be either:
     * 1. an exact route without arguments
     * 2. a route containing arguments where no arguments are filled in
     * 3. a route containing arguments where some or all arguments are filled in
     * 4. a partial route
     *
     * In the case of 3., it will only match if the entry arguments
     * match exactly with the arguments that were filled in inside the route.
     *
     * @param [route] The route to match with the route of this destination
     *
     * @param [arguments] The [NavBackStackEntry.arguments] that was used to navigate
     * to this destination
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun hasRoute(route: String, arguments: Bundle?): Boolean {
        // this matches based on routePattern
        if (this.route == route) return true
        // if no match based on routePattern, this means route contains filled in args or query
        // params
        val matchingDeepLink = matchDeepLink(route)
        // if no matchingDeepLink or mismatching destination, return false directly
        if (this != matchingDeepLink?.destination) return false
        // Any args (partially or completely filled in) must exactly match between
        // the route and entry's route.
        return matchingDeepLink.hasMatchingArgs(arguments)
    }
    /**
     * @return Whether this NavDestination supports outgoing actions
     * @see NavDestination.putAction
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun supportsActions(): Boolean {
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
    public fun getAction(id: Int): NavAction? {
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
    public fun putAction(@IdRes actionId: Int, @IdRes destId: Int) {
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
    public fun putAction(@IdRes actionId: Int, action: NavAction) {
        if (!supportsActions()) {
            throw UnsupportedOperationException(
                "Cannot add action $actionId to $this as it does not support actions, " +
                    "indicating that it is a terminal destination in your navigation graph and " +
                    "will never trigger actions."
            )
        }
        require(actionId != 0) { "Cannot have an action with actionId 0" }
        actions.put(actionId, action)
    }
    /**
     * Unsets the [NavAction] for an action ID.
     *
     * @param actionId action ID to remove
     */
    public fun removeAction(@IdRes actionId: Int) {
        actions.remove(actionId)
    }
    /**
     * Sets an argument type for an argument name
     *
     * @param argumentName argument object to associate with destination
     * @param argument argument object to associate with destination
     */
    public fun addArgument(argumentName: String, argument: NavArgument) {
        _arguments[argumentName] = argument
    }
    /**
     * Unsets the argument type for an argument name.
     *
     * @param argumentName argument to remove
     */
    public fun removeArgument(argumentName: String) {
        _arguments.remove(argumentName)
    }
    /**
     * Combines the default arguments for this destination with the arguments provided
     * to construct the final set of arguments that should be used to navigate
     * to this destination.
     */
    @Suppress("NullableCollection") // Needed for nullable bundle
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun addInDefaultArgs(args: Bundle?): Bundle? {
        if (args == null && _arguments.isEmpty()) {
            return null
        }
        val defaultArgs = Bundle()
        for ((key, value) in _arguments) {
            value.putDefaultValue(key, defaultArgs)
        }
        if (args != null) {
            defaultArgs.putAll(args)
            // Don't verify unknown default values - these default values are only available
            // during deserialization for safe args.
            for ((key, value) in _arguments) {
                if (!value.isDefaultValueUnknown) {
                    require(value.verify(key, defaultArgs)) {
                        "Wrong argument type for '$key' in argument bundle. ${value.type.name} " +
                            "expected."
                    }
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
        val equalDeepLinks = deepLinks.intersect(other.deepLinks).size == deepLinks.size
        val equalActions = actions.size() == other.actions.size() &&
            actions.valueIterator().asSequence().all { other.actions.containsValue(it) } &&
            other.actions.valueIterator().asSequence().all { actions.containsValue(it) }
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
        actions.valueIterator().forEach { value ->
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
         @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
         public fun createRoute(route: String?): String =
            if (route != null) "android-app://androidx.navigation/$route" else ""
        /**
         * Provides a sequence of the NavDestination's hierarchy. The hierarchy starts with this
         * destination itself and is then followed by this destination's [NavDestination.parent], then that
         * graph's parent, and up the hierarchy until you've reached the root navigation graph.
         */
        @JvmStatic
        public val NavDestination.hierarchy: Sequence<NavDestination>
            get() = generateSequence(this) { it.parent }
    }
}