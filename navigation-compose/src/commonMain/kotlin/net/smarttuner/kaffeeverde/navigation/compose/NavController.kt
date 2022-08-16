package net.smarttuner.kaffeeverde.navigation.compose

import net.smarttuner.kaffeeverde.navigation.NavController
import net.smarttuner.kaffeeverde.navigation.NavGraph
import net.smarttuner.kaffeeverde.navigation.NavGraphBuilder
import net.smarttuner.kaffeeverde.navigation.navigation

/**
 * Construct a new [NavGraph]
 *
 * @param startDestination the route for the start destination
 * @param route the route for the graph
 * @param builder the builder used to construct the graph
 */
public inline fun NavController.createGraph(
    startDestination: String,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(startDestination, route, builder)