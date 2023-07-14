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
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/navigation/navigation-runtime/src/main/java/androidx/navigation/NavController.kt
 */
package net.smarttuner.kaffeeverde.navigation

import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import net.smarttuner.kaffeeverde.core.AtomicInteger
import net.smarttuner.kaffeeverde.core.Bundle
import net.smarttuner.kaffeeverde.core.Uri
import net.smarttuner.kaffeeverde.core.getAnyArrayList
import net.smarttuner.kaffeeverde.core.getBoolean
import net.smarttuner.kaffeeverde.core.getBundle
import net.smarttuner.kaffeeverde.core.getIntArray
import net.smarttuner.kaffeeverde.core.getStringArrayList
import net.smarttuner.kaffeeverde.core.net.URISyntaxException
import net.smarttuner.kaffeeverde.core.putAnyArray
import net.smarttuner.kaffeeverde.core.putBoolean
import net.smarttuner.kaffeeverde.core.putBundle
import net.smarttuner.kaffeeverde.core.putIntArray
import net.smarttuner.kaffeeverde.core.putObject
import net.smarttuner.kaffeeverde.core.putStringArrayList
import net.smarttuner.kaffeeverde.core.toUri
import net.smarttuner.kaffeeverde.lifecycle.ViewModelStore
import net.smarttuner.kaffeeverde.lifecycle.ViewModelStoreOwner
import net.smarttuner.kaffeeverde.lifecycle.*
import net.smarttuner.kaffeeverde.navigation.NavDestination.Companion.createRoute
import kotlin.jvm.JvmOverloads

/**
 * NavController manages app navigation within a [NavHost].
 *
 * Apps will generally obtain a controller directly from a host, or by using one of the utility
 * methods on the [Navigation] class rather than create a controller directly.
 *
 * Navigation flows and destinations are determined by the
 * [navigation graph][NavGraph] owned by the controller. These graphs are typically
 * [inflated][navInflater] from an Android resource, but, like views, they can also
 * be constructed or combined programmatically or for the case of dynamic navigation structure.
 * (For example, if the navigation structure of the application is determined by live data obtained'
 * from a remote server.)
 */
open class NavController{
    private var _graph: NavGraph? = null

    /**
     * The topmost navigation graph associated with this NavController.
     *
     * When this is set any current navigation graph data (including back stack) will be replaced.
     *
     * @see NavController.setGraph
     * @throws IllegalStateException if called before `setGraph()`.
     */
    open var graph: NavGraph
        
        get() {
            checkNotNull(_graph) { "You must call setGraph() before calling getGraph()" }
            return _graph as NavGraph
        }
        
        
        set(graph) {
            setGraph(graph, null)
        }

    private var navigatorStateToRestore: Bundle? = null
    private var backStackToRestore: ArrayList<Any>? = null
    private var deepLinkHandled = false

    /**
     * Retrieve the current back stack.
     *
     * @return The current back stack.
     * @hide
     */
    
    open val backQueue: ArrayDeque<NavBackStackEntry> = ArrayDeque()

    private val _visibleEntries: MutableStateFlow<List<NavBackStackEntry>> =
        MutableStateFlow(emptyList())

    /**
     * A [StateFlow] that will emit the currently visible [NavBackStackEntries][NavBackStackEntry]
     * whenever they change. If there is no visible [NavBackStackEntry], this will be set to an
     * empty list.
     *
     * - `CREATED` entries are listed first and include all entries that have been popped from
     * the back stack and are in the process of completing their exit transition
     * - `STARTED` entries on the back stack are next and include all entries that are running
     * their enter transition and entries whose destination is partially covered by a
     * `FloatingWindow` destination
     * - The last entry in the list is the topmost entry in the back stack and is in the `RESUMED`
     * state only if its enter transition has completed. Otherwise it too will be `STARTED`.
     *
     * Note that the `Lifecycle` of any entry cannot be higher than the containing
     * Activity/Fragment - if the Activity is not `RESUMED`, no entry will be `RESUMED`, no matter
     * what the transition state is.
     */
    val visibleEntries: StateFlow<List<NavBackStackEntry>> =
        _visibleEntries.asStateFlow()

    private val childToParentEntries = mutableMapOf<NavBackStackEntry, NavBackStackEntry>()
    private val parentToChildCount = mutableMapOf<NavBackStackEntry, AtomicInteger>()

    private fun linkChildToParent(child: NavBackStackEntry, parent: NavBackStackEntry) {
        childToParentEntries[child] = parent
        if (parentToChildCount[parent] == null) {
            parentToChildCount[parent] = AtomicInteger(0)
        }
        parentToChildCount[parent]!!.incrementAndGet()
    }

    internal fun unlinkChildFromParent(child: NavBackStackEntry): NavBackStackEntry? {
        val parent = childToParentEntries.remove(child) ?: return null
        val count = parentToChildCount[parent]?.decrementAndGet()
        if (count == 0) {
            val navGraphNavigator: Navigator<out NavGraph> =
                _navigatorProvider[parent.destination.navigatorName]
            navigatorState[navGraphNavigator]?.markTransitionComplete(parent)
            parentToChildCount.remove(parent)
        }
        return parent
    }

    private val backStackMap = mutableMapOf<Int, String?>()
    private val backStackStates = mutableMapOf<String, ArrayDeque<NavBackStackEntryState>>()
    private var lifecycleOwner: LifecycleOwner? = null
    private var viewModel: NavControllerViewModel? = null
    private val onDestinationChangedListeners = ArrayList<OnDestinationChangedListener>()
    internal var hostLifecycleState: Lifecycle.State = Lifecycle.State.INITIALIZED
        get() {
            // A LifecycleOwner is not required by NavController.
            // In the cases where one is not provided, always keep the host lifecycle at CREATED
            return if (lifecycleOwner == null) {
                Lifecycle.State.CREATED
            } else {
                field
            }
        }

    private val lifecycleObserver: LifecycleObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            hostLifecycleState = event.targetState
            if (_graph != null) {
                for (entry in backQueue) {
                    entry.handleLifecycleEvent(event)
                }
            }
        }
    }



    /**
     * OnDestinationChangedListener receives a callback when the
     * [currentDestination] or its arguments change.
     */
    fun interface OnDestinationChangedListener {
        /**
         * Callback for when the [currentDestination] or its arguments change.
         * This navigation may be to a destination that has not been seen before, or one that
         * was previously on the back stack. This method is called after navigation is complete,
         * but associated transitions may still be playing.
         *
         * @param controller the controller that navigated
         * @param destination the new destination
         * @param arguments the arguments passed to the destination
         */
        fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: Bundle?
        )
    }

    private var _navigatorProvider = NavigatorProvider()

    /**
     * The NavController's [NavigatorProvider]. All [Navigators][Navigator] used
     * to construct the [navigation graph][NavGraph] for this nav controller should be added
     * to this navigator provider before the graph is constructed.
     *
     * This can only be set before the graph is set via `setGraph()`.
     *
     * Generally, the Navigators are set for you by the [NavHost] hosting this NavController
     * and you do not need to manually interact with the navigator provider.
     *
     * @throws IllegalStateException If this set called after `setGraph()`
     */
    open var navigatorProvider: NavigatorProvider
        get() = _navigatorProvider
        /**
         * @hide
         */
        set(navigatorProvider) {
            check(backQueue.isEmpty()) { "NavigatorProvider must be set before setGraph call" }
            _navigatorProvider = navigatorProvider
        }

    private val navigatorState =
        mutableMapOf<Navigator<out NavDestination>, NavControllerNavigatorState>()
    private var addToBackStackHandler: ((backStackEntry: NavBackStackEntry) -> Unit)? = null
    private var popFromBackStackHandler: ((popUpTo: NavBackStackEntry) -> Unit)? = null
    private val entrySavedState = mutableMapOf<NavBackStackEntry, Boolean>()

    /**
     * Call [Navigator.navigate] while setting up a [handler] that receives callbacks
     * when [NavigatorState.push] is called.
     */
    private fun Navigator<out NavDestination>.navigateInternal(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
        handler: (backStackEntry: NavBackStackEntry) -> Unit = {}
    ) {
        addToBackStackHandler = handler
        navigate(entries, navOptions, navigatorExtras)
        addToBackStackHandler = null
    }

    /**
     * Call [Navigator.popBackStack] while setting up a [handler] that receives callbacks
     * when [NavigatorState.pop] is called.
     */
    private fun Navigator<out NavDestination>.popBackStackInternal(
        popUpTo: NavBackStackEntry,
        saveState: Boolean,
        handler: (popUpTo: NavBackStackEntry) -> Unit = {}
    ) {
        popFromBackStackHandler = handler
        popBackStack(popUpTo, saveState)
        popFromBackStackHandler = null
    }

    private inner class NavControllerNavigatorState(
        val navigator: Navigator<out NavDestination>
    ) : NavigatorState() {
        override fun push(backStackEntry: NavBackStackEntry) {
            val destinationNavigator: Navigator<out NavDestination> =
                _navigatorProvider[backStackEntry.destination.navigatorName]
            if (destinationNavigator == navigator) {
                val handler = addToBackStackHandler
                if (handler != null) {
                    handler(backStackEntry)
                    addInternal(backStackEntry)
                } else {
                    // TODO handle the Navigator calling add() outside of a call to navigate()
                    Napier.i(
                        "$TAG Ignoring add of destination ${backStackEntry.destination} " +
                                "outside of the call to navigate(). "
                    )
                }
            } else {
                val navigatorBackStack = checkNotNull(navigatorState[destinationNavigator]) {
                    "NavigatorBackStack for ${backStackEntry.destination.navigatorName} should " +
                            "already be created"
                }
                navigatorBackStack.push(backStackEntry)
            }
        }

        fun addInternal(backStackEntry: NavBackStackEntry) {
            super.push(backStackEntry)
        }

        override fun createBackStackEntry(
            destination: NavDestination,
            arguments: Bundle?
        ) = NavBackStackEntry.create(
            destination, arguments,
            hostLifecycleState, viewModel
        )

        override fun pop(popUpTo: NavBackStackEntry, saveState: Boolean) {
            val destinationNavigator: Navigator<out NavDestination> =
                _navigatorProvider[popUpTo.destination.navigatorName]
            if (destinationNavigator == navigator) {
                val handler = popFromBackStackHandler
                if (handler != null) {
                    handler(popUpTo)
                    super.pop(popUpTo, saveState)
                } else {
                    popBackStackFromNavigator(popUpTo) {
                        super.pop(popUpTo, saveState)
                    }
                }
            } else {
                navigatorState[destinationNavigator]!!.pop(popUpTo, saveState)
            }
        }

        override fun popWithTransition(popUpTo: NavBackStackEntry, saveState: Boolean) {
            super.popWithTransition(popUpTo, saveState)
            entrySavedState[popUpTo] = saveState
        }

        override fun markTransitionComplete(entry: NavBackStackEntry) {
            val savedState = entrySavedState[entry] == true
            super.markTransitionComplete(entry)
            entrySavedState.remove(entry)
            if (!backQueue.contains(entry)) {
                unlinkChildFromParent(entry)
                // If the entry is no longer part of the backStack, we need to manually move
                // it to DESTROYED, and clear its view model
                if (entry.platformLifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                    entry.maxLifecycle = Lifecycle.State.DESTROYED
                }
                if (backQueue.none { it.id == entry.id } && !savedState) {
                    viewModel?.clear(entry.id)
                }
                updateBackStackLifecycle()
                _visibleEntries.tryEmit(populateVisibleEntries())
            } else if (!this@NavControllerNavigatorState.isNavigating) {
                updateBackStackLifecycle()
                _visibleEntries.tryEmit(populateVisibleEntries())
            }
            // else, updateBackStackLifecycle() will be called after any ongoing navigate() call
            // completes
        }
    }

    /**
     * Constructs a new controller for a given [Context]. Controllers should not be
     * used outside of their context and retain a hard reference to the context supplied.
     * If you need a global controller, pass [Context.getApplicationContext].
     *
     * Apps should generally not construct controllers, instead obtain a relevant controller
     * directly from a navigation host via [NavHost.getNavController] or by using one of
     * the utility methods on the [Navigation] class.
     *
     * Note that controllers that are not constructed with an [Activity] context
     * (or a wrapped activity context) will only be able to navigate to
     * [new tasks][android.content.Intent.FLAG_ACTIVITY_NEW_TASK] or
     * [new document tasks][android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT] when
     * navigating to new activities.
     *
     * @param context context for this controller
     */
    init {
        _navigatorProvider.addNavigator(NavGraphNavigator(_navigatorProvider))
    }

    /**
     * Adds an [OnDestinationChangedListener] to this controller to receive a callback
     * whenever the [currentDestination] or its arguments change.
     *
     * The current destination, if any, will be immediately sent to your listener.
     *
     * @param listener the listener to receive events
     */
    open fun addOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        onDestinationChangedListeners.add(listener)

        // Inform the new listener of our current state, if any
        if (backQueue.isNotEmpty()) {
            val backStackEntry = backQueue.last()
            listener.onDestinationChanged(
                this,
                backStackEntry.destination,
                backStackEntry.arguments
            )
        }
    }

    /**
     * Removes an [OnDestinationChangedListener] from this controller.
     * It will no longer receive callbacks.
     *
     * @param listener the listener to remove
     */
    open fun removeOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        onDestinationChangedListeners.remove(listener)
    }

    /**
     * Attempts to pop the controller's back stack. Analogous to when the user presses
     * the system [Back][android.view.KeyEvent.KEYCODE_BACK] button when the associated
     * navigation host has focus.
     *
     * @return true if the stack was popped at least once and the user has been navigated to
     * another destination, false otherwise
     */
    
    open fun popBackStack(): Boolean {
        return if (backQueue.isEmpty()) {
            // Nothing to pop if the back stack is empty
            false
        } else {
            popBackStack(currentDestination!!.id, true)
        }
    }

    /**
     * Attempts to pop the controller's back stack back to a specific destination.
     *
     * @param destinationId The topmost destination to retain
     * @param inclusive Whether the given destination should also be popped.
     *
     * @return true if the stack was popped at least once and the user has been navigated to
     * another destination, false otherwise
     */
    
    open fun popBackStack( destinationId: Int, inclusive: Boolean): Boolean {
        return popBackStack(destinationId, inclusive, false)
    }

    /**
     * Attempts to pop the controller's back stack back to a specific destination.
     *
     * @param destinationId The topmost destination to retain
     * @param inclusive Whether the given destination should also be popped.
     * @param saveState Whether the back stack and the state of all destinations between the
     * current destination and the [destinationId] should be saved for later
     * restoration via [NavOptions.Builder.setRestoreState] or the `restoreState` attribute using
     * the same [destinationId] (note: this matching ID is true whether
     * [inclusive] is true or false).
     *
     * @return true if the stack was popped at least once and the user has been navigated to
     * another destination, false otherwise
     */
    
    open fun popBackStack(
         destinationId: Int,
        inclusive: Boolean,
        saveState: Boolean
    ): Boolean {
        val popped = popBackStackInternal(destinationId, inclusive, saveState)
        // Only return true if the pop succeeded and we've dispatched
        // the change to a new destination
        return popped && dispatchOnDestinationChanged()
    }

    /**
     * Attempts to pop the controller's back stack back to a specific destination.
     *
     * @param route The topmost destination to retain
     * @param inclusive Whether the given destination should also be popped.
     * @param saveState Whether the back stack and the state of all destinations between the
     * current destination and the [route] should be saved for later
     * restoration via [NavOptions.Builder.setRestoreState] or the `restoreState` attribute using
     * the same [route] (note: this matching ID is true whether
     * [inclusive] is true or false).
     *
     * @return true if the stack was popped at least once and the user has been navigated to
     * another destination, false otherwise
     */
    
    @JvmOverloads
    fun popBackStack(
        route: String,
        inclusive: Boolean,
        saveState: Boolean = false
    ): Boolean = popBackStack(createRoute(route).hashCode(), inclusive, saveState)

    /**
     * Attempts to pop the controller's back stack back to a specific destination. This does
     * **not** handle calling [dispatchOnDestinationChanged]
     *
     * @param destinationId The topmost destination to retain
     * @param inclusive Whether the given destination should also be popped.
     * @param saveState Whether the back stack and the state of all destinations between the
     * current destination and the [destinationId] should be saved for later
     * restoration via [NavOptions.Builder.setRestoreState] or the `restoreState` attribute using
     * the same [destinationId] (note: this matching ID is true whether
     * [inclusive] is true or false).
     *
     * @return true if the stack was popped at least once, false otherwise
     */
    
    private fun popBackStackInternal(
         destinationId: Int,
        inclusive: Boolean,
        saveState: Boolean = false
    ): Boolean {
        if (backQueue.isEmpty()) {
            // Nothing to pop if the back stack is empty
            return false
        }
        val popOperations = mutableListOf<Navigator<*>>()
        val iterator = backQueue.reversed().iterator()
        var foundDestination: NavDestination? = null
        while (iterator.hasNext()) {
            val destination = iterator.next().destination
            val navigator = _navigatorProvider.getNavigator<Navigator<*>>(
                destination.navigatorName
            )
            if (inclusive || destination.id != destinationId) {
                popOperations.add(navigator)
            }
            if (destination.id == destinationId) {
                foundDestination = destination
                break
            }
        }
        if (foundDestination == null) {
            return false
        }
        var popped = false
        val savedState = ArrayDeque<NavBackStackEntryState>()
        for (navigator in popOperations) {
            var receivedPop = false
            navigator.popBackStackInternal(backQueue.last(), saveState) { entry ->
                receivedPop = true
                popped = true
                popEntryFromBackStack(entry, saveState, savedState)
            }
            if (!receivedPop) {
                // The pop did not complete successfully, so stop immediately
                break
            }
        }
        if (saveState) {
            if (!inclusive) {
                // If this isn't an inclusive pop, we need to explicitly map the
                // saved state to the destination you've actually passed to popUpTo
                // as well as its parents (if it is the start destination)
                generateSequence(foundDestination) { destination ->
                    if (destination.parent?.startDestinationId == destination.id) {
                        destination.parent
                    } else {
                        null
                    }
                }.takeWhile { destination ->
                    // Only add the state if it doesn't already exist
                    !backStackMap.containsKey(destination.id)
                }.forEach { destination ->
                    backStackMap[destination.id] = savedState.firstOrNull()?.id
                }
            }
            if (savedState.isNotEmpty()) {
                val firstState = savedState.first()
                // Whether is is inclusive or not, we need to map the
                // saved state to the destination that was popped
                // as well as its parents (if it is the start destination)
                val firstStateDestination = findDestination(firstState.destinationId)
                generateSequence(firstStateDestination) { destination ->
                    if (destination.parent?.startDestinationId == destination.id) {
                        destination.parent
                    } else {
                        null
                    }
                }.takeWhile { destination ->
                    // Only add the state if it doesn't already exist
                    !backStackMap.containsKey(destination.id)
                }.forEach { destination ->
                    backStackMap[destination.id] = firstState.id
                }
                // And finally, store the actual state itself
                backStackStates[firstState.id] = savedState
            }
        }
        return popped
    }

    /**
     * Trigger a popBackStack() that originated from a Navigator specifically calling
     * [NavigatorState.pop] outside of a call to [popBackStack] (e.g., in response to some
     * user interaction that caused that destination to no longer be needed such as
     * dismissing a dialog destination).
     *
     * This method is responsible for popping all destinations above the given [popUpTo] entry and
     * popping the entry itself and removing it from the back stack before calling the
     * [onComplete] callback. Only after the processing here is done and the [onComplete]
     * callback completes does this method dispatch the destination change event.
     */
    internal fun popBackStackFromNavigator(popUpTo: NavBackStackEntry, onComplete: () -> Unit) {
        val popIndex = backQueue.indexOf(popUpTo)
        if (popIndex < 0) {
            Napier.i(
                "$TAG Ignoring pop of $popUpTo as it was not found on the current back stack"
            )
            return
        }
        if (popIndex + 1 != backQueue.size) {
            // There's other destinations stacked on top of this destination that
            // we need to pop first
            popBackStackInternal(
                backQueue[popIndex + 1].destination.id,
                inclusive = true,
                saveState = false
            )
        }
        // Now record the pop of the actual entry - we don't use popBackStackInternal
        // here since we're being called from the Navigator already
        popEntryFromBackStack(popUpTo)
        onComplete()
        dispatchOnDestinationChanged()
    }

    private fun popEntryFromBackStack(
        popUpTo: NavBackStackEntry,
        saveState: Boolean = false,
        savedState: ArrayDeque<NavBackStackEntryState> = ArrayDeque()
    ) {
        val entry = backQueue.last()
        check(entry == popUpTo) {
            "Attempted to pop ${popUpTo.destination}, which is not the top of the back stack " +
                    "(${entry.destination})"
        }
        backQueue.removeLast()
        val navigator = navigatorProvider
            .getNavigator<Navigator<NavDestination>>(entry.destination.navigatorName)
        val state = navigatorState[navigator]
        // If we pop an entry with transitions, but not the graph, we will not make a call to
        // popBackStackInternal, so the graph entry will not be marked as transitioning so we
        // need to check if it still has children.
        val transitioning = state?.transitionsInProgress?.value?.contains(entry) == true ||
                parentToChildCount.containsKey(entry)
        if (entry.platformLifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            if (saveState) {
                // Move the state through STOPPED
                entry.maxLifecycle = Lifecycle.State.CREATED
                // Then save the state of the NavBackStackEntry
                savedState.addFirst(NavBackStackEntryState(entry))
            }
            if (!transitioning) {
                entry.maxLifecycle = Lifecycle.State.DESTROYED
                unlinkChildFromParent(entry)
            } else {
                entry.maxLifecycle = Lifecycle.State.CREATED
            }
        }
        if (!saveState && !transitioning) {
            viewModel?.clear(entry.id)
        }
    }

    /**
     * Clears any saved state associated with [route] that was previously saved
     * via [popBackStack] when using a `saveState` value of `true`.
     *
     * @param route The route of the destination previously used with [popBackStack] with a
     * `saveState` value of `true`
     *
     * @return true if the saved state of the stack associated with [route] was cleared.
     */

    fun clearBackStack(
        route: String
    ): Boolean = clearBackStack(createRoute(route).hashCode())

    /**
     * Clears any saved state associated with [destinationId] that was previously saved
     * via [popBackStack] when using a `saveState` value of `true`.
     *
     * @param destinationId The ID of the destination previously used with [popBackStack] with a
     * `saveState`value of `true`
     *
     * @return true if the saved state of the stack associated with [destinationId] was cleared.
     */

    fun clearBackStack( destinationId: Int): Boolean {
        val cleared = clearBackStackInternal(destinationId)
        // Only return true if the clear succeeded and we've dispatched
        // the change to a new destination
        return cleared && dispatchOnDestinationChanged()
    }

    
    private fun clearBackStackInternal( destinationId: Int): Boolean {
        navigatorState.values.forEach { state ->
            state.isNavigating = true
        }
        val restored = restoreStateInternal(destinationId, null, null, null)
        navigatorState.values.forEach { state ->
            state.isNavigating = false
        }
        return restored && popBackStackInternal(destinationId, inclusive = true, saveState = false)
    }





    /**
     * Gets the number of non-NavGraph destinations on the back stack
     */
    private val destinationCountOnBackStack: Int
        get() = backQueue.count { entry ->
            entry.destination !is NavGraph
        }

    private var dispatchReentrantCount = 0
    private val backStackEntriesToDispatch = mutableListOf<NavBackStackEntry>()

    /**
     * Dispatch changes to all OnDestinationChangedListeners.
     *
     * If the back stack is empty, no events get dispatched.
     *
     * @return If changes were dispatched.
     */
    private fun dispatchOnDestinationChanged(): Boolean {
        // We never want to leave NavGraphs on the top of the stack
        while (!backQueue.isEmpty() && backQueue.last().destination is NavGraph) {
            popEntryFromBackStack(backQueue.last())
        }
        val lastBackStackEntry = backQueue.lastOrNull()
        if (lastBackStackEntry != null) {
            backStackEntriesToDispatch += lastBackStackEntry
        }
        // Track that we're updating the back stack lifecycle
        // just in case updateBackStackLifecycle() results in
        // additional calls to navigate() or popBackStack()
        dispatchReentrantCount++
        updateBackStackLifecycle()
        dispatchReentrantCount--

        if (dispatchReentrantCount == 0) {
            // Only the outermost dispatch should dispatch
            val dispatchList = backStackEntriesToDispatch.toMutableList()
            backStackEntriesToDispatch.clear()
            for (backStackEntry in dispatchList) {
                // Now call all registered OnDestinationChangedListener instances
                for (listener in onDestinationChangedListeners) {
                    listener.onDestinationChanged(
                        this,
                        backStackEntry.destination,
                        backStackEntry.arguments
                    )
                }
                _currentBackStackEntryFlow.tryEmit(backStackEntry)
            }
            _visibleEntries.tryEmit(populateVisibleEntries())
        }
        return lastBackStackEntry != null
    }

    internal fun updateBackStackLifecycle() {
        // Operate on a copy of the queue to avoid issues with reentrant
        // calls if updating the Lifecycle calls navigate() or popBackStack()
        val backStack = backQueue.toMutableList()
        if (backStack.isEmpty()) {
            // Nothing to update
            return
        }
        // First determine what the current resumed destination is and, if and only if
        // the current resumed destination is a FloatingWindow, what destination is
        // underneath it that must remain started.
        var nextResumed: NavDestination? = backStack.last().destination
        var nextStarted: NavDestination? = null

        // First iterate downward through the stack, applying downward Lifecycle
        // transitions and capturing any upward Lifecycle transitions to apply afterwards.
        // This ensures proper nesting where parent navigation graphs are started before
        // their children and stopped only after their children are stopped.
        val upwardStateTransitions = HashMap<NavBackStackEntry, Lifecycle.State>()
        var iterator = backStack.reversed().iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val currentMaxLifecycle = entry.maxLifecycle
            val destination = entry.destination
            if (nextResumed != null && destination.id == nextResumed.id) {
                // Upward Lifecycle transitions need to be done afterwards so that
                // the parent navigation graph is resumed before their children
                if (currentMaxLifecycle != Lifecycle.State.RESUMED) {
                    val navigator = navigatorProvider
                        .getNavigator<Navigator<*>>(entry.destination.navigatorName)
                    val state = navigatorState[navigator]
                    val transitioning = state?.transitionsInProgress?.value?.contains(entry)
                    if (transitioning != true && parentToChildCount[entry]?.get() != 0) {
                        upwardStateTransitions[entry] = Lifecycle.State.RESUMED
                    } else {
                        upwardStateTransitions[entry] = Lifecycle.State.STARTED
                    }
                }
                nextResumed = nextResumed.parent
            } else if (nextStarted != null && destination.id == nextStarted.id) {
                if (currentMaxLifecycle == Lifecycle.State.RESUMED) {
                    // Downward transitions should be done immediately so children are
                    // paused before their parent navigation graphs
                    entry.maxLifecycle = Lifecycle.State.STARTED
                } else if (currentMaxLifecycle != Lifecycle.State.STARTED) {
                    // Upward Lifecycle transitions need to be done afterwards so that
                    // the parent navigation graph is started before their children
                    upwardStateTransitions[entry] = Lifecycle.State.STARTED
                }
                nextStarted = nextStarted.parent
            } else {
                entry.maxLifecycle = Lifecycle.State.CREATED
            }
        }
        // Apply all upward Lifecycle transitions by iterating through the stack again,
        // this time applying the new lifecycle to the parent navigation graphs first
        iterator = backStack.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val newState = upwardStateTransitions[entry]
            if (newState != null) {
                entry.maxLifecycle = newState
            } else {
                // Ensure the state is up to date
                entry.updateState()
            }
        }
    }

    internal fun populateVisibleEntries(): List<NavBackStackEntry> {
        val entries = mutableListOf<NavBackStackEntry>()
        // Add any transitioning entries that are not at least STARTED
        navigatorState.values.forEach { state ->
            entries += state.transitionsInProgress.value.filter { entry ->
                !entries.contains(entry) &&
                        !entry.maxLifecycle.isAtLeast(Lifecycle.State.STARTED)
            }
        }
        // Add any STARTED entries from the backQueue. This will include the topmost
        // non-FloatingWindow destination plus every FloatingWindow destination above it.
        entries += backQueue.filter { entry ->
            !entries.contains(entry) &&
                    entry.maxLifecycle.isAtLeast(Lifecycle.State.STARTED)
        }
        return entries.filter {
            it.destination !is NavGraph
        }
    }

    /**
     * Sets the [navigation graph][NavGraph] to the specified graph.
     * Any current navigation graph data (including back stack) will be replaced.
     *
     * The graph can be retrieved later via [graph].
     *
     * @param graph graph to set
     * @see NavController.setGraph
     * @see NavController.graph
     */


    public open fun setGraph(graph: NavGraph, startDestinationArgs: Bundle?) {
        if (_graph != graph) {
            _graph?.let { previousGraph ->
                // Clear all saved back stacks by iterating through a copy of the saved keys,
                // thus avoiding any concurrent modification exceptions
                val savedBackStackIds = ArrayList(backStackMap.keys)
                savedBackStackIds.forEach { id ->
                    clearBackStackInternal(id)
                }
                // Pop everything from the old graph off the back stack
                popBackStackInternal(previousGraph.id, true)
            }
            _graph = graph
            onGraphCreated(startDestinationArgs)
        } else {
            for (i in 0 until graph.nodes.size()) {
                val newDestination = graph.nodes.valueAt(i)
                _graph!!.nodes.replace(i, newDestination)
                backQueue.filter { currentEntry ->
                    // Necessary since CI builds against ToT, can be removed once
                    // androidx.collection is updated to >= 1.3.*
                    @Suppress("UNNECESSARY_SAFE_CALL", "SAFE_CALL_WILL_CHANGE_NULLABILITY")
                    currentEntry.destination.id == newDestination?.id
                }.forEach { entry ->
                    if(newDestination!=null) {
                        entry.destination = newDestination
                    }
                }
            }
        }
    }


    private fun onGraphCreated(startDestinationArgs: Bundle?) {
        navigatorStateToRestore?.let { navigatorStateToRestore ->
            val navigatorNames = navigatorStateToRestore.getStringArrayList(
                KEY_NAVIGATOR_STATE_NAMES
            )
            if (navigatorNames != null) {
                for (name in navigatorNames) {
                    val navigator = _navigatorProvider.getNavigator<Navigator<*>>(name)
                    val bundle = navigatorStateToRestore.getBundle(name)
                    if (bundle != null) {
                        navigator.onRestoreState(bundle)
                    }
                }
            }
        }
        backStackToRestore?.let { backStackToRestore ->
            for (parcelable in backStackToRestore) {
                val state = parcelable as NavBackStackEntryState
                val node = findDestination(state.destinationId)
                if (node == null) {
                    throw IllegalStateException(
                        "Restoring the Navigation back stack failed: destination cannot be " +
                                "found from the current destination $currentDestination"
                    )
                }
                val entry = state.instantiate(node, hostLifecycleState, viewModel)
                val navigator = _navigatorProvider.getNavigator<Navigator<*>>(node.navigatorName)
                val navigatorBackStack = navigatorState.getOrPut(navigator) {
                    NavControllerNavigatorState(navigator)
                }
                backQueue.add(entry)
                navigatorBackStack.addInternal(entry)
                val parent = entry.destination.parent
                if (parent != null) {
                    linkChildToParent(entry, getBackStackEntry(parent.id))
                }
            }
            //updateOnBackPressedCallbackEnabled()
            this.backStackToRestore = null
        }
        // Mark all Navigators as attached
        _navigatorProvider.navigators.values.filterNot { it.isAttached }.forEach { navigator ->
            val navigatorBackStack = navigatorState.getOrPut(navigator) {
                NavControllerNavigatorState(navigator)
            }
            navigator.onAttach(navigatorBackStack)
        }
        if (_graph != null && backQueue.isEmpty()) {
            // Navigate to the first destination in the graph
            // if we haven't deep linked to a destination
            navigate(_graph!!, startDestinationArgs, null, null)
        } else {
            dispatchOnDestinationChanged()
        }
    }



    /**
     * Looks through the deep link for invalid destinations, returning the display name of
     * any invalid destinations in the deep link array.
     *
     * @param deepLink array of deep link IDs that are expected to match the graph
     * @return The display name of the first destination not found in the graph or null if
     * all destinations were found in the graph.
     */
    private fun findInvalidDestinationDisplayNameInDeepLink(deepLink: IntArray): String? {
        var graph = _graph
        for (i in deepLink.indices) {
            val destinationId = deepLink[i]
            val node =
                (
                        if (i == 0)
                            if (_graph!!.id == destinationId) _graph
                            else null
                        else
                            graph!!.findNode(destinationId)
                        ) ?: return null
            if (i != deepLink.size - 1) {
                // We're not at the final NavDestination yet, so keep going through the chain
                if (node is NavGraph) {
                    graph = node
                    // Automatically go down the navigation graph when
                    // the start destination is also a NavGraph
                    while (graph!!.findNode(graph.startDestinationId) is NavGraph) {
                        graph = graph.findNode(graph.startDestinationId) as NavGraph?
                    }
                }
            }
        }
        // We found every destination in the deepLink array, yay!
        return null
    }

    /**
     * The current destination.
     */
    open val currentDestination: NavDestination?
        get() {
            return currentBackStackEntry?.destination
        }

    /** @suppress */

    fun findDestination( destinationId: Int): NavDestination? {
        if (_graph == null) {
            return null
        }
        if (_graph!!.id == destinationId) {
            return _graph
        }
        val currentNode = backQueue.lastOrNull()?.destination ?: _graph!!
        return currentNode.findDestination(destinationId)
    }

    private fun NavDestination.findDestination( destinationId: Int): NavDestination? {
        if (id == destinationId) {
            return this
        }
        val currentGraph = if (this is NavGraph) this else parent!!
        return currentGraph.findNode(destinationId)
    }

    /** @suppress */

    fun findDestination(destinationRoute: String): NavDestination? {
        if (_graph == null) {
            return null
        }
        if (_graph!!.route == destinationRoute) {
            return _graph
        }
        val currentNode = backQueue.lastOrNull()?.destination ?: _graph!!
        val currentGraph = if (currentNode is NavGraph) currentNode else currentNode.parent!!
        return currentGraph.findNode(destinationRoute)
    }


    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to
     * navigate to
     * @param args arguments to pass to the destination
     *
     * @throws IllegalStateException if there is no current navigation node
     * @throws IllegalArgumentException if the desired destination cannot be found from the
     *                                  current destination
     */
    
    open fun navigate( resId: Int, args: Bundle? = null) {
        navigate(resId, args, null)
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to
     * navigate to
     * @param args arguments to pass to the destination
     * @param navOptions special options for this navigation operation
     *
     * @throws IllegalStateException if there is no current navigation node
     * @throws IllegalArgumentException if the desired destination cannot be found from the
     *                                  current destination
     */
    
    open fun navigate( resId: Int, args: Bundle?, navOptions: NavOptions?) {
        navigate(resId, args, navOptions, null)
    }

    /**
     * Navigate to a destination from the current navigation graph. This supports both navigating
     * via an [action][NavDestination.getAction] and directly navigating to a destination.
     *
     * @param resId an [action][NavDestination.getAction] id or a destination id to
     * navigate to
     * @param args arguments to pass to the destination
     * @param navOptions special options for this navigation operation
     * @param navigatorExtras extras to pass to the Navigator
     *
     * @throws IllegalStateException if there is no current navigation node
     * @throws IllegalArgumentException if the desired destination cannot be found from the
     *                                  current destination
     */
    
    open fun navigate(
         resId: Int,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        var finalNavOptions = navOptions
        val currentNode = (
                if (backQueue.isEmpty())
                    _graph
                else
                    backQueue.last().destination
                ) ?: throw IllegalStateException("no current navigation node")

        
        var destId = resId
        val navAction = currentNode.getAction(resId)
        var combinedArgs: Bundle? = null
        if (navAction != null) {
            if (finalNavOptions == null) {
                finalNavOptions = navAction.navOptions
            }
            destId = navAction.destinationId
            val navActionArgs = navAction.defaultArguments
            if (navActionArgs != null) {
                combinedArgs = Bundle()
                combinedArgs.putAll(navActionArgs)
            }
        }
        if (args != null) {
            if (combinedArgs == null) {
                combinedArgs = Bundle()
            }
            combinedArgs.putAll(args)
        }
        if (destId == 0 && finalNavOptions != null && finalNavOptions.popUpToId != -1) {
            popBackStack(finalNavOptions.popUpToId, finalNavOptions.isPopUpToInclusive())
            return
        }
        require(destId != 0) {
            "Destination id == 0 can only be used in conjunction with a valid navOptions.popUpTo"
        }
        val node = findDestination(destId)
        if (node == null) {
            require(navAction == null) {
                "Navigation destination referenced from action " +
                        " cannot be found from " +
                        "the current destination $currentNode"
            }
            throw IllegalArgumentException(
                "Navigation action/destination cannot be found from the current " +
                        "destination $currentNode"
            )
        }
        navigate(node, combinedArgs, finalNavOptions, navigatorExtras)
    }

    /**
     * Navigate to a destination via the given deep link [Uri].
     * [NavDestination.hasDeepLink] should be called on
     * [the navigation graph][graph] prior to calling this method to check if the deep
     * link is valid. If an invalid deep link is given, an [IllegalArgumentException] will be
     * thrown.
     *
     * @param deepLink deepLink to the destination reachable from the current NavGraph
     * @param navOptions special options for this navigation operation
     * @param navigatorExtras extras to pass to the Navigator
     * @see NavController.navigate
     */
    
    open fun navigate(
        deepLink: Uri,
        navOptions: NavOptions? = null,
        navigatorExtras: Navigator.Extras? = null
    ) {
        navigate(NavDeepLinkRequest(deepLink, null, null), navOptions, navigatorExtras)
    }

    /**
     * Navigate to a destination via the given [NavDeepLinkRequest].
     * [NavDestination.hasDeepLink] should be called on
     * [the navigation graph][graph] prior to calling this method to check if the deep
     * link is valid. If an invalid deep link is given, an [IllegalArgumentException] will be
     * thrown.
     *
     * @param request deepLinkRequest to the destination reachable from the current NavGraph
     * @param navOptions special options for this navigation operation
     *
     * @throws IllegalArgumentException if the given deep link request is invalid
     */
    
    open fun navigate(request: NavDeepLinkRequest, navOptions: NavOptions? = null) {
        navigate(request, navOptions, null)
    }

    /**
     * Navigate to a destination via the given [NavDeepLinkRequest].
     * [NavDestination.hasDeepLink] should be called on
     * [the navigation graph][graph] prior to calling this method to check if the deep
     * link is valid. If an invalid deep link is given, an [IllegalArgumentException] will be
     * thrown.
     *
     * @param request deepLinkRequest to the destination reachable from the current NavGraph
     * @param navOptions special options for this navigation operation
     * @param navigatorExtras extras to pass to the Navigator
     *
     * @throws IllegalArgumentException if the given deep link request is invalid
     */
    
    open fun navigate(
        request: NavDeepLinkRequest,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        val deepLinkMatch = _graph?.matchDeepLink(request)
        if (deepLinkMatch != null) {
            val destination = deepLinkMatch.destination
            val args = destination.addInDefaultArgs(deepLinkMatch.matchingArgs) ?: Bundle()
            val node = deepLinkMatch.destination
            args.putObject(KEY_DEEP_LINK_INTENT, request.uri)
            navigate(node, args, navOptions, navigatorExtras)
        } else {
            throw IllegalArgumentException(
                "Navigation destination that matches request $request cannot be found in the " +
                        "navigation graph $_graph"
            )
        }
    }

    
    private fun navigate(
        node: NavDestination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        navigatorState.values.forEach { state ->
            state.isNavigating = true
        }
        var popped = false
        var launchSingleTop = false
        var navigated = false
        if (navOptions != null) {
            if (navOptions.popUpToId != -1) {
                popped = popBackStackInternal(
                    navOptions.popUpToId,
                    navOptions.isPopUpToInclusive(),
                    navOptions.shouldPopUpToSaveState()
                )
            }
        }
        val finalArgs = node.addInDefaultArgs(args)
        // Now determine what new destinations we need to add to the back stack
        if (navOptions?.shouldRestoreState() == true && backStackMap.containsKey(node.id)) {
            navigated = restoreStateInternal(node.id, finalArgs, navOptions, navigatorExtras)
        } else {
            val currentBackStackEntry = currentBackStackEntry
            val navigator = _navigatorProvider.getNavigator<Navigator<NavDestination>>(
                node.navigatorName
            )
            if (navOptions?.shouldLaunchSingleTop() == true &&
                node.id == currentBackStackEntry?.destination?.id
            ) {
                unlinkChildFromParent(backQueue.removeLast())
                val newEntry = NavBackStackEntry(currentBackStackEntry, finalArgs)
                backQueue.addLast(newEntry)
                val parent = newEntry.destination.parent
                if (parent != null) {
                    linkChildToParent(newEntry, getBackStackEntry(parent.id))
                }
                navigator.onLaunchSingleTop(newEntry)
                launchSingleTop = true
            } else {
                // Not a single top operation, so we're looking to add the node to the back stack
                val backStackEntry = NavBackStackEntry.create(
                    node, finalArgs, hostLifecycleState, viewModel
                )
                navigator.navigateInternal(listOf(backStackEntry), navOptions, navigatorExtras) {
                    navigated = true
                    addEntryToBackStack(node, finalArgs, it)
                }
            }
        }
        navigatorState.values.forEach { state ->
            state.isNavigating = false
        }
        if (popped || navigated || launchSingleTop) {
            dispatchOnDestinationChanged()
        } else {
            updateBackStackLifecycle()
        }
    }

    private fun restoreStateInternal(
        id: Int,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ): Boolean {
        if (!backStackMap.containsKey(id)) {
            return false
        }
        val backStackId = backStackMap[id]
        // Clear out the state we're going to restore so that it isn't restored a second time
        backStackMap.values.removeAll { it == backStackId }
        val backStackState = backStackStates.remove(backStackId)
        // Now restore the back stack from its saved state
        val entries = instantiateBackStack(backStackState)
        // Split up the entries by Navigator so we can restore them as an atomic operation
        val entriesGroupedByNavigator = mutableListOf<MutableList<NavBackStackEntry>>()
        entries.filterNot { entry ->
            // Skip navigation graphs - they'll be added by addEntryToBackStack()
            entry.destination is NavGraph
        }.forEach { entry ->
            val previousEntryList = entriesGroupedByNavigator.lastOrNull()
            val previousNavigatorName = previousEntryList?.last()?.destination?.navigatorName
            if (previousNavigatorName == entry.destination.navigatorName) {
                // Group back to back entries associated with the same Navigator together
                previousEntryList += entry
            } else {
                // Create a new group for the new Navigator
                entriesGroupedByNavigator += mutableListOf(entry)
            }
        }
        var navigated = false
        // Now actually navigate to each set of entries
        for (entryList in entriesGroupedByNavigator) {
            val navigator = _navigatorProvider.getNavigator<Navigator<NavDestination>>(
                entryList.first().destination.navigatorName
            )
            var lastNavigatedIndex = 0
            navigator.navigateInternal(entryList, navOptions, navigatorExtras) { entry ->
                navigated = true
                // If this destination is part of the restored back stack,
                // pass all destinations between the last navigated entry and this one
                // to ensure that any navigation graphs are properly restored as well
                val entryIndex = entries.indexOf(entry)
                val restoredEntries = if (entryIndex != -1) {
                    entries.subList(lastNavigatedIndex, entryIndex + 1).also {
                        lastNavigatedIndex = entryIndex + 1
                    }
                } else {
                    emptyList()
                }
                addEntryToBackStack(entry.destination, args, entry, restoredEntries)
            }
        }
        return navigated
    }

    private fun instantiateBackStack(
        backStackState: ArrayDeque<NavBackStackEntryState>?
    ): List<NavBackStackEntry> {
        val backStack = mutableListOf<NavBackStackEntry>()
        var currentDestination = backQueue.lastOrNull()?.destination ?: graph
        backStackState?.forEach { state ->
            val node = currentDestination.findDestination(state.destinationId)
            checkNotNull(node) {
                "Restore State failed: destination cannot be found from the current " +
                        "destination $currentDestination"
            }
            backStack += state.instantiate(node, hostLifecycleState, viewModel)
            currentDestination = node
        }
        return backStack
    }

    private fun addEntryToBackStack(
        node: NavDestination,
        finalArgs: Bundle?,
        backStackEntry: NavBackStackEntry,
        restoredEntries: List<NavBackStackEntry> = emptyList()
    ) {
        val newDest = backStackEntry.destination
          // When you navigate() to a NavGraph, we need to ensure that a new instance
        // is always created vs reusing an existing copy of that destination
        val hierarchy = ArrayDeque<NavBackStackEntry>()
        var destination: NavDestination? = newDest
        if (node is NavGraph) {
            do {
                val parent = destination!!.parent
                if (parent != null) {
                    val entry = restoredEntries.lastOrNull { restoredEntry ->
                        restoredEntry.destination == parent
                    } ?: NavBackStackEntry.create(
                        parent,
                        finalArgs, hostLifecycleState, viewModel
                    )
                    hierarchy.addFirst(entry)
                    // Pop any orphaned copy of that navigation graph off the back stack
                    if (backQueue.isNotEmpty() && backQueue.last().destination === parent) {
                        popEntryFromBackStack(backQueue.last())
                    }
                }
                destination = parent
            } while (destination != null && destination !== node)
        }

        // Now collect the set of all intermediate NavGraphs that need to be put onto
        // the back stack
        destination = if (hierarchy.isEmpty()) newDest else hierarchy.first().destination
        while (destination != null && findDestination(destination.id) == null) {
            val parent = destination.parent
            if (parent != null) {
                val entry = restoredEntries.lastOrNull { restoredEntry ->
                    restoredEntry.destination == parent
                } ?: NavBackStackEntry.create(
                    parent, parent.addInDefaultArgs(finalArgs), hostLifecycleState,
                    viewModel
                )
                hierarchy.addFirst(entry)
            }
            destination = parent
        }
        val overlappingDestination: NavDestination =
            if (hierarchy.isEmpty())
                newDest
            else
                hierarchy.last().destination
        // Pop any orphaned navigation graphs that don't connect to the new destinations
        while (!backQueue.isEmpty() && backQueue.last().destination is NavGraph &&
            (backQueue.last().destination as NavGraph).findNode(
                overlappingDestination.id, false
            ) == null
        ) {
            popEntryFromBackStack(backQueue.last())
        }

        // The _graph should always be on the top of the back stack after you navigate()
        val firstEntry = backQueue.firstOrNull() ?: hierarchy.firstOrNull()
        if (firstEntry?.destination != _graph) {
            val entry = restoredEntries.lastOrNull { restoredEntry ->
                restoredEntry.destination == _graph!!
            } ?: NavBackStackEntry.create(
                _graph!!, _graph!!.addInDefaultArgs(finalArgs), hostLifecycleState,
                viewModel
            )
            hierarchy.addFirst(entry)
        }

        // Now add the parent hierarchy to the NavigatorStates and back stack
        hierarchy.forEach { entry ->
            val navigator = _navigatorProvider.getNavigator<Navigator<*>>(
                entry.destination.navigatorName
            )
            val navigatorBackStack = checkNotNull(navigatorState[navigator]) {
                "NavigatorBackStack for ${node.navigatorName} should already be created"
            }
            navigatorBackStack.addInternal(entry)
        }
        backQueue.addAll(hierarchy)

        // And finally, add the new destination
        backQueue.add(backStackEntry)

        // Link the newly added hierarchy and entry with the parent NavBackStackEntry
        // so that we can track how many destinations are associated with each NavGraph
        (hierarchy + backStackEntry).forEach {
            val parent = it.destination.parent
            if (parent != null) {
                linkChildToParent(it, getBackStackEntry(parent.id))
            }
        }
    }

    /**
     * Navigate via the given [NavDirections]
     *
     * @param directions directions that describe this navigation operation
     */
    
    open fun navigate(directions: NavDirections) {
        navigate(directions.actionId, directions.arguments, null)
    }

    /**
     * Navigate via the given [NavDirections]
     *
     * @param directions directions that describe this navigation operation
     * @param navOptions special options for this navigation operation
     */
    
    open fun navigate(directions: NavDirections, navOptions: NavOptions?) {
        navigate(directions.actionId, directions.arguments, navOptions)
    }

    /**
     * Navigate via the given [NavDirections]
     *
     * @param directions directions that describe this navigation operation
     * @param navigatorExtras extras to pass to the [Navigator]
     */
    
    open fun navigate(directions: NavDirections, navigatorExtras: Navigator.Extras) {
        navigate(directions.actionId, directions.arguments, null, navigatorExtras)
    }

    /**
     * Navigate to a route in the current NavGraph. If an invalid route is given, an
     * [IllegalArgumentException] will be thrown.
     *
     * @param route route for the destination
     * @param builder DSL for constructing a new [NavOptions]
     *
     * @throws IllegalArgumentException if the given route is invalid
     */
    fun navigate(route: String, builder: NavOptionsBuilder.() -> Unit) {
        navigate(route, navOptions(builder))
    }

    /**
     * Navigate to a route in the current NavGraph. If an invalid route is given, an
     * [IllegalArgumentException] will be thrown.
     *
     * @param route route for the destination
     * @param navOptions special options for this navigation operation
     * @param navigatorExtras extras to pass to the [Navigator]
     *
     * @throws IllegalArgumentException if the given route is invalid
     */
    @JvmOverloads
    fun navigate(
        route: String,
        navOptions: NavOptions? = null,
        navigatorExtras: Navigator.Extras? = null
    ) {
        val uri = createRoute(route).toUri() ?: throw URISyntaxException(route, "Parsing exception")
        navigate(
            NavDeepLinkRequest.Builder.fromUri(uri).build(), navOptions,
            navigatorExtras
        )
    }

    /**
     * Create a deep link to a destination within this NavController.
     *
     * @return a [NavDeepLinkBuilder] suitable for constructing a deep link
     */
    open fun createDeepLink(): NavDeepLinkBuilder {
        return NavDeepLinkBuilder(this)
    }

    /**
     * Saves all navigation controller state to a Bundle.java.
     *
     * State may be restored from a bundle returned from this method by calling
     * [restoreState]. Saving controller state is the responsibility
     * of a [NavHost].
     *
     * @return saved state for this controller
     */
    
    open fun saveState(): Bundle? {
        var b: Bundle? = null
        val navigatorNames = ArrayList<String>()
        val navigatorState = Bundle()
        for ((name, value) in _navigatorProvider.navigators) {
            val savedState = value.onSaveState()
            if (savedState != null) {
                navigatorNames.add(name)
                navigatorState.putBundle(name, savedState)
            }
        }
        if (navigatorNames.isNotEmpty()) {
            b = Bundle()
            navigatorState.putStringArrayList(KEY_NAVIGATOR_STATE_NAMES, navigatorNames)
            b.putBundle(KEY_NAVIGATOR_STATE, navigatorState)
        }
        if (backQueue.isNotEmpty()) {
            if (b == null) {
                b = Bundle()
            }
            val backStack = arrayOfNulls<Any>(backQueue.size)
            var index = 0
            for (backStackEntry in this.backQueue) {
                backStack[index++] = NavBackStackEntryState(backStackEntry)
            }
            b.putAnyArray(KEY_BACK_STACK, backStack)
        }
        if (backStackMap.isNotEmpty()) {
            if (b == null) {
                b = Bundle()
            }
            val backStackDestIds = IntArray(backStackMap.size)
            val backStackIds = ArrayList<String>()
            var index = 0
            for ((id,destId) in backStackMap) {
                backStackDestIds[index++] = id
                backStackIds += destId ?: ""
            }
            b.putIntArray(KEY_BACK_STACK_DEST_IDS, backStackDestIds)
            b.putStringArrayList(KEY_BACK_STACK_IDS, backStackIds)
        }
        if (backStackStates.isNotEmpty()) {
            if (b == null) {
                b = Bundle()
            }
            val backStackStateIds = ArrayList<String>()
            for ((id, backStackStates) in backStackStates) {
                backStackStateIds += id
                val states = arrayOfNulls<Any>(backStackStates.size)
                backStackStates.forEachIndexed { stateIndex, backStackState ->
                    states[stateIndex] = backStackState
                }
                b.putAnyArray(KEY_BACK_STACK_STATES_PREFIX + id, states)
            }
            b.putStringArrayList(KEY_BACK_STACK_STATES_IDS, backStackStateIds)
        }
        if (deepLinkHandled) {
            if (b == null) {
                b = Bundle()
            }
            b.putBoolean(KEY_DEEP_LINK_HANDLED, deepLinkHandled)
        }
        return b
    }

    /**
     * Restores all navigation controller state from a bundle. This should be called before any
     * call to [setGraph].
     *
     * State may be saved to a bundle by calling [saveState].
     * Restoring controller state is the responsibility of a [NavHost].
     *
     * @param navState state bundle to restore
     */
    
    @Suppress("DEPRECATION")
    open fun restoreState(navState: Bundle?) {
        if (navState == null) {
            return
        }
        navigatorStateToRestore = navState.getBundle(KEY_NAVIGATOR_STATE)
        backStackToRestore = navState.getAnyArrayList(KEY_BACK_STACK)
        backStackStates.clear()
        val backStackDestIds = navState.getIntArray(KEY_BACK_STACK_DEST_IDS)
        val backStackIds = navState.getStringArrayList(KEY_BACK_STACK_IDS)
        if (backStackDestIds != null && backStackIds != null) {
            backStackDestIds.forEachIndexed { index, id ->
                backStackMap[id] = backStackIds[index]
            }
        }
        val backStackStateIds = navState.getStringArrayList(KEY_BACK_STACK_STATES_IDS)
        backStackStateIds?.forEach { id ->
            val backStackState = navState.getAnyArrayList(KEY_BACK_STACK_STATES_PREFIX + id)
            if (backStackState != null) {
                backStackStates[id] = ArrayDeque<NavBackStackEntryState>(
                    backStackState.size
                ).apply {
                    for (Any in backStackState) {
                        add(Any as NavBackStackEntryState)
                    }
                }
            }
        }
        deepLinkHandled = navState.getBoolean(KEY_DEEP_LINK_HANDLED)
    }

    /** @suppress */
    
    open fun setLifecycleOwner(owner: LifecycleOwner) {
        if (owner == lifecycleOwner) {
            return
        }
        lifecycleOwner?.platformLifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = owner
        owner.platformLifecycle.addObserver(lifecycleObserver)
    }

    /** @suppress */
    open fun setViewModelStore(viewModelStore: ViewModelStore) {
        if (viewModel == NavControllerViewModel.getInstance(viewModelStore)) {
            return
        }
        check(backQueue.isEmpty()) { "ViewModelStore should be set before setGraph call" }
        viewModel = NavControllerViewModel.getInstance(viewModelStore)
    }

    /**
     * Gets the [ViewModelStoreOwner] for a NavGraph. This can be passed to
     * [androidx.lifecycle.ViewModelProvider] to retrieve a ViewModel that is scoped
     * to the navigation graph - it will be cleared when the navigation graph is popped off
     * the back stack.
     *
     * @param navGraphId ID of a NavGraph that exists on the back stack
     * @throws IllegalStateException if called before the [NavHost] has called
     * [NavHostController.setViewModelStore].
     * @throws IllegalArgumentException if the NavGraph is not on the back stack
     */
    open fun getViewModelStoreOwner( navGraphId: Int): ViewModelStoreOwner {
        checkNotNull(viewModel) {
            "You must call setViewModelStore() before calling getViewModelStoreOwner()."
        }
        val lastFromBackStack = getBackStackEntry(navGraphId)
        require(lastFromBackStack.destination is NavGraph) {
            "No NavGraph with ID $navGraphId is on the NavController's back stack"
        }
        return lastFromBackStack
    }

    /**
     * Gets the topmost [NavBackStackEntry] for a destination id.
     *
     * This is always safe to use with [the current destination][currentDestination] or
     * [its parent][NavDestination.parent] or grandparent navigation graphs as these
     * destinations are guaranteed to be on the back stack.
     *
     * @param destinationId ID of a destination that exists on the back stack
     * @throws IllegalArgumentException if the destination is not on the back stack
     */
    open fun getBackStackEntry( destinationId: Int): NavBackStackEntry {
        val lastFromBackStack: NavBackStackEntry? = backQueue.lastOrNull { entry ->
            entry.destination.id == destinationId
        }
        requireNotNull(lastFromBackStack) {
            "No destination with ID $destinationId is on the NavController's back stack. The " +
                    "current destination is $currentDestination"
        }
        return lastFromBackStack
    }

    /**
     * Gets the topmost [NavBackStackEntry] for a route.
     *
     * This is always safe to use with [the current destination][currentDestination] or
     * [its parent][NavDestination.parent] or grandparent navigation graphs as these
     * destinations are guaranteed to be on the back stack.
     *
     * @param route route of a destination that exists on the back stack
     * @throws IllegalArgumentException if the destination is not on the back stack
     */
    fun getBackStackEntry(route: String): NavBackStackEntry {
        val lastFromBackStack: NavBackStackEntry? = backQueue.lastOrNull { entry ->
            entry.destination.route == route
        }
        requireNotNull(lastFromBackStack) {
            "No destination with route $route is on the NavController's back stack. The " +
                    "current destination is $currentDestination"
        }
        return lastFromBackStack
    }

    /**
     * The topmost [NavBackStackEntry].
     *
     * @return the topmost entry on the back stack or null if the back stack is empty
     */
    open val currentBackStackEntry: NavBackStackEntry?
        get() = backQueue.lastOrNull()

    private val _currentBackStackEntryFlow: MutableSharedFlow<NavBackStackEntry> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * A [Flow] that will emit the currently active [NavBackStackEntry] whenever it changes. If
     * there is no active [NavBackStackEntry], no item will be emitted.
     */
    val currentBackStackEntryFlow: Flow<NavBackStackEntry> =
        _currentBackStackEntryFlow.asSharedFlow()

    /**
     * The previous visible [NavBackStackEntry].
     *
     * This skips over any [NavBackStackEntry] that is associated with a [NavGraph].
     *
     * @return the previous visible entry on the back stack or null if the back stack has less
     * than two visible entries
     */
    open val previousBackStackEntry: NavBackStackEntry?
        get() {
            val iterator = backQueue.reversed().iterator()
            // throw the topmost destination away.
            if (iterator.hasNext()) {
                iterator.next()
            }
            return iterator.asSequence().firstOrNull { entry ->
                entry.destination !is NavGraph
            }
        }

    companion object {
        private const val TAG = "NavController"
        private const val KEY_NAVIGATOR_STATE = "android-support-nav:controller:navigatorState"
        private const val KEY_NAVIGATOR_STATE_NAMES =
            "android-support-nav:controller:navigatorState:names"
        private const val KEY_BACK_STACK = "android-support-nav:controller:backStack"
        private const val KEY_BACK_STACK_DEST_IDS =
            "android-support-nav:controller:backStackDestIds"
        private const val KEY_BACK_STACK_IDS =
            "android-support-nav:controller:backStackIds"
        private const val KEY_BACK_STACK_STATES_IDS =
            "android-support-nav:controller:backStackStates"
        private const val KEY_BACK_STACK_STATES_PREFIX =
            "android-support-nav:controller:backStackStates:"
        /** @suppress */
        
        const val KEY_DEEP_LINK_IDS: String = "android-support-nav:controller:deepLinkIds"
        /** @suppress */
        
        const val KEY_DEEP_LINK_ARGS: String = "android-support-nav:controller:deepLinkArgs"
        /** @suppress */
        
        @Suppress("IntentName")
        const val KEY_DEEP_LINK_EXTRAS: String =
            "android-support-nav:controller:deepLinkExtras"
        /** @suppress */
        
        const val KEY_DEEP_LINK_HANDLED: String =
            "android-support-nav:controller:deepLinkHandled"

        /**
         * The [Intent] that triggered a deep link to the current destination.
         */
        const val KEY_DEEP_LINK_INTENT: String =
            "android-support-nav:controller:deepLinkIntent"

        private var deepLinkSaveState = true

        /**
         * By default, [handleDeepLink] will automatically add calls to
         * [NavOptions.Builder.setPopUpTo] with a `saveState` of `true` when the deep
         * link takes you to another graph (e.g., a different navigation graph than the
         * one your start destination is in).
         *
         * You can disable this behavior by passing `false` for [saveState].
         */
        
        
        fun enableDeepLinkSaveState(saveState: Boolean) {
            deepLinkSaveState = saveState
        }
    }


    /**
     * Attempts to navigate up in the navigation hierarchy. Suitable for when the
     * user presses the "Up" button marked with a left (or start)-facing arrow in the upper left
     * (or starting) corner of the app UI.
     *
     * The intended behavior of Up differs from [Back][popBackStack] when the user
     * did not reach the current destination from the application's own task. e.g. if the user
     * is viewing a document or link in the current app in an activity hosted on another app's
     * task where the user clicked the link. In this case the current activity (determined by the
     * context used to create this NavController) will be [finished][Activity.finish] and
     * the user will be taken to an appropriate destination in this app on its own task.
     *
     * @return true if navigation was successful, false otherwise
     */
    public open fun navigateUp(): Boolean = popBackStack()

}

/**
 * Construct a new [NavGraph]
 *
 * @param id the graph's unique id
 * @param startDestination the route for the start destination
 * @param builder the builder used to construct the graph
 */
//@Suppress("Deprecation")
//@Deprecated(
//    "Use routes to create your NavGraph instead",
//    ReplaceWith(
//        "createGraph(startDestination = startDestination.toString(), route = id.toString()) " +
//                "{ builder.invoke() }"
//    )
//)
//public inline fun NavController.createGraph(
//     id: Int = 0,
//     startDestination: Int,
//    builder: NavGraphBuilder.() -> Unit
//): NavGraph = navigatorProvider.navigation(id, startDestination, builder)
//
///**
// * Construct a new [NavGraph]
// *
// * @param startDestination the route for the start destination
// * @param route the route for the graph
// * @param builder the builder used to construct the graph
// */
//public inline fun NavController.createGraph(
//    startDestination: String,
//    route: String? = null,
//    builder: NavGraphBuilder.() -> Unit
//): NavGraph = navigatorProvider.navigation(startDestination, route, builder)
