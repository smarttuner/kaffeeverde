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
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/navigation/navigation-runtime/src/main/java/androidx/navigation/NavDeepLinkBuilder.kt
 */
package net.smarttuner.kaffeeverde.navigation

import net.smarttuner.kaffeeverde.core.Bundle
import net.smarttuner.kaffeeverde.navigation.NavDestination.Companion.createRoute

/**
 * Class used to construct deep links to a particular destination in a [NavGraph].
 *
 * When this deep link is triggered:
 *
 *  1. The task is cleared.
 *  2. The destination and all of its parents will be on the back stack.
 *  3. Calling [NavController.navigateUp] will navigate to the parent of the
 * destination.
 *
 * The parent of the destination is the [start destination][NavGraph.getStartDestination]
 * of the containing [navigation graph][NavGraph]. In the cases where the destination is
 * the start destination of its containing navigation graph, the start destination of its
 * grandparent is used.
 *
 * You can construct an instance directly with [NavDeepLinkBuilder] or build one
 * using an existing [NavController] via [NavController.createDeepLink].
 *
 * If the context passed in here is not an [Activity], this method will use
 * [android.content.pm.PackageManager.getLaunchIntentForPackage] as the
 * default activity to launch, if available.
 *
 * @param context Context used to create deep links
 * @see NavDeepLinkBuilder.setComponentName
 */
public class NavDeepLinkBuilder {
    private class DeepLinkDestination constructor(
        val destinationId: Int,
        val arguments: Bundle?
    )
    private var graph: NavGraph? = null
    private val destinations = mutableListOf<DeepLinkDestination>()
    private var globalArgs: Bundle? = null
    /**
     * @see NavController.createDeepLink
     */
    internal constructor(navController: NavController) {
        graph = navController.graph
    }
    /**
     * Sets the graph that contains the [deep link destination][setDestination].
     *
     * If you do not have access to a [NavController], you can create a
     * [NavigatorProvider] and use that to programmatically construct a navigation
     * graph or use [NavInflater][NavInflater].
     *
     * @param navGraph The [NavGraph] containing the deep link destination
     * @return this object for chaining
     */
    public fun setGraph(navGraph: NavGraph): NavDeepLinkBuilder {
        graph = navGraph
        verifyAllDestinations()
        return this
    }
    /**
     * Sets the destination id to deep link to. Any destinations previous added via
     * [addDestination] are cleared, effectively resetting this object
     * back to only this single destination.
     *
     * @param destId destination ID to deep link to.
     * @param args Arguments to pass to this destination and any synthetic back stack created
     * due to this destination being added.
     * @return this object for chaining
     */
    
    public fun setDestination(destId: Int, args: Bundle? = null): NavDeepLinkBuilder {
        destinations.clear()
        destinations.add(DeepLinkDestination(destId, args))
        if (graph != null) {
            verifyAllDestinations()
        }
        return this
    }
    /**
     * Sets the destination route to deep link to. Any destinations previous added via
     * [.addDestination] are cleared, effectively resetting this object
     * back to only this single destination.
     *
     * @param destRoute destination route to deep link to.
     * @param args Arguments to pass to this destination and any synthetic back stack created
     * due to this destination being added.
     * @return this object for chaining
     */
    
    public fun setDestination(destRoute: String, args: Bundle? = null): NavDeepLinkBuilder {
        destinations.clear()
        destinations.add(DeepLinkDestination(createRoute(destRoute).hashCode(), args))
        if (graph != null) {
            verifyAllDestinations()
        }
        return this
    }
    /**
     * Add a new destination id to deep link to. This builds off any previous calls to this method
     * or calls to [setDestination], building the minimal synthetic back stack of
     * start destinations between the previous deep link destination and the newly added
     * deep link destination.
     *
     * This means that if R.navigation.nav_graph has startDestination= R.id.start_destination,
     *
     * ```
     * navDeepLinkBuilder
     *    .setGraph(R.navigation.nav_graph)
     *    .addDestination(R.id.second_destination, null)
     * ```
     * is equivalent to
     * ```
     * navDeepLinkBuilder
     *    .setGraph(R.navigation.nav_graph)
     *    .addDestination(R.id.start_destination, null)
     *    .addDestination(R.id.second_destination, null)
     * ```
     *
     * Use the second form to assign specific arguments to the start destination.
     *
     * @param destId destination ID to deep link to.
     * @param args Arguments to pass to this destination and any synthetic back stack created
     * due to this destination being added.
     * @return this object for chaining
     */
    
    public fun addDestination(destId: Int, args: Bundle? = null): NavDeepLinkBuilder {
        destinations.add(DeepLinkDestination(destId, args))
        if (graph != null) {
            verifyAllDestinations()
        }
        return this
    }
    /**
     * Add a new destination route to deep link to. This builds off any previous calls to this
     * method or calls to [.setDestination], building the minimal synthetic back stack of
     * start destinations between the previous deep link destination and the newly added
     * deep link destination.
     *
     * @param route destination route to deep link to.
     * @param args Arguments to pass to this destination and any synthetic back stack created
     * due to this destination being added.
     * @return this object for chaining
     */
    
    public fun addDestination(route: String, args: Bundle? = null): NavDeepLinkBuilder {
        destinations.add(DeepLinkDestination(createRoute(route).hashCode(), args))
        if (graph != null) {
            verifyAllDestinations()
        }
        return this
    }
    private fun findDestination(destId: Int): NavDestination? {
        val possibleDestinations = ArrayDeque<NavDestination>()
        possibleDestinations.add(graph!!)
        while (!possibleDestinations.isEmpty()) {
            val destination = possibleDestinations.removeFirst()
            if (destination.id == destId) {
                return destination
            } else if (destination is NavGraph) {
                for (child in destination) {
                    if(child!=null) {
                        possibleDestinations.add(child)
                    }
                }
            }
        }
        return null
    }
    private fun verifyAllDestinations() {
        for (destination in destinations) {
            val destId = destination.destinationId
            val node = findDestination(destId)
            if (node == null) {
                throw IllegalArgumentException(
                    "Navigation destination cannot be found in the navigation graph $graph"
                )
            }
        }
    }


    /**
     * A [NavigatorProvider] that only parses the basics: [navigation graphs][NavGraph]
     * and [destinations][NavDestination], effectively only getting the base destination
     * information.
     */
    private class PermissiveNavigatorProvider : NavigatorProvider() {
        /**
         * A Navigator that only parses the [NavDestination] attributes.
         */
        private val mDestNavigator: Navigator<NavDestination> =
            object : Navigator<NavDestination>() {
                override fun createDestination(): NavDestination {
                    return NavDestination("permissive")
                }
                override fun navigate(
                    destination: NavDestination,
                    args: Bundle?,
                    navOptions: NavOptions?,
                    navigatorExtras: Extras?
                ): NavDestination? {
                    throw IllegalStateException("navigate is not supported")
                }
                override fun popBackStack(): Boolean {
                    throw IllegalStateException("popBackStack is not supported")
                }
            }
        @Suppress("UNCHECKED_CAST")
        override fun <T : Navigator<out NavDestination>> getNavigator(name: String): T {
            return try {
                super.getNavigator(name)
            } catch (e: IllegalStateException) {
                mDestNavigator as T
            }
        }
        init {
            addNavigator(NavGraphNavigator(this))
        }
    }
}