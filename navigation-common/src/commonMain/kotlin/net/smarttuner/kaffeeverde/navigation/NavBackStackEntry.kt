/*
 * Copyright 2019 The Android Open Source Project
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
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/navigation/navigation-common/src/main/java/androidx/navigation/NavBackStackEntry.kt
 */
package net.smarttuner.kaffeeverde.navigation

import androidx.annotation.RestrictTo
import net.smarttuner.kaffeeverde.core.Bundle
import net.smarttuner.kaffeeverde.core.UUID
import net.smarttuner.kaffeeverde.core.keySet
import net.smarttuner.kaffeeverde.lifecycle.*
import net.smarttuner.kaffeeverde.lifecycle.viewmodel.CreationExtras
import net.smarttuner.kaffeeverde.lifecycle.viewmodel.MutableCreationExtras
import kotlin.reflect.KClass

/**
 * Representation of an entry in the back stack of a [androidx.navigation.NavController]. The
 * [Lifecycle], [ViewModelStore], and [SavedStateRegistry] provided via
 * this object are valid for the lifetime of this destination on the back stack: when this
 * destination is popped off the back stack, the lifecycle will be destroyed, state
 * will no longer be saved, and ViewModels will be cleared.
 */
class NavBackStackEntry private constructor(
    /**
     * The destination associated with this entry
     * @return The destination that is currently visible to users
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var destination: NavDestination,
    private val immutableArgs: Bundle? = null,
    private var hostLifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
    private val viewModelStoreProvider: NavViewModelStoreProvider? = null,
    /**
     * The unique ID that serves as the identity of this entry
     * @return the unique ID of this entry
     */
    public val id: String = UUID.randomUUID().toString(),
    private val savedState: Bundle? = null
) : LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(entry: NavBackStackEntry, arguments: Bundle? = entry.arguments) : this(
        entry.destination,
        arguments,
        entry.hostLifecycleState,
        entry.viewModelStoreProvider,
        entry.id,
        entry.savedState
    ) {
        hostLifecycleState = entry.hostLifecycleState
        maxLifecycle = entry.maxLifecycle
    }
    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun create(
            destination: NavDestination,
            arguments: Bundle? = null,
            hostLifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
            viewModelStoreProvider: NavViewModelStoreProvider? = null,
            id: String = UUID.randomUUID().toString(),
            savedState: Bundle? = null
        ): NavBackStackEntry = NavBackStackEntry(
            destination, arguments,
            hostLifecycleState, viewModelStoreProvider, id, savedState
        )
    }
    var _platformLifecycle = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private var savedStateRegistryAttached = false
    private val defaultFactory by lazy {
        SavedStateViewModelFactory(this, arguments)
    }
    /**
     * The arguments used for this entry. Note that the arguments of
     * a NavBackStackEntry are immutable and defined when you `navigate()`
     * to the destination - changes you make to this Bundle will not be
     * reflected in future calls to this property.
     *
     * @return The arguments used when this entry was created
     */
    public val arguments: Bundle?
        get() = if (immutableArgs == null) {
            null
        } else {
            Bundle(immutableArgs)
        }
    /**
     * The [SavedStateHandle] for this entry.
     */
    public val savedStateHandle: SavedStateHandle by lazy {
        check(savedStateRegistryAttached) {
            "You cannot access the NavBackStackEntry's SavedStateHandle until it is added to " +
                "the NavController's back stack (i.e., the Lifecycle of the NavBackStackEntry " +
                "reaches the CREATED state)."
        }
        check(platformLifecycle.currentState != Lifecycle.State.DESTROYED) {
            "You cannot access the NavBackStackEntry's SavedStateHandle after the " +
                "NavBackStackEntry is destroyed."
        }
        ViewModelProvider(
            this, NavResultSavedStateFactory(this)
        ).get(SavedStateViewModel::class).handle
    }
    /**
     * {@inheritDoc}
     *
     * If the [androidx.navigation.NavHost] has not called
     * [androidx.navigation.NavHostController.setLifecycleOwner], the
     * Lifecycle will be capped at [Lifecycle.State.CREATED].
     */
    override val platformLifecycle: Lifecycle
        get() = _platformLifecycle
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var maxLifecycle: Lifecycle.State = Lifecycle.State.INITIALIZED
        set(maxState) {
            field = maxState
            updateState()
        }
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun handleLifecycleEvent(event: Lifecycle.Event) {
        hostLifecycleState = event.targetState
        updateState()
    }
    /**
     * Update the state to be the lower of the two constraints:
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun updateState() {
        if (!savedStateRegistryAttached) {
            savedStateRegistryController.performAttach()
            savedStateRegistryAttached = true
            if (viewModelStoreProvider != null) {
                enableSavedStateHandles()
            }
            // Perform the restore just once, the first time updateState() is called
            // and specifically *before* we move up the Lifecycle
            savedStateRegistryController.performRestore(savedState)
        }
        if (hostLifecycleState.ordinal < maxLifecycle.ordinal) {
            _platformLifecycle.currentState = hostLifecycleState
        } else {
            _platformLifecycle.currentState = maxLifecycle
        }
    }
    public override val platformViewModelStore: ViewModelStore
        /**
         * {@inheritDoc}
         *
         * @throws IllegalStateException if called before the [lifecycle] has moved to
         * [Lifecycle.State.CREATED] or before the [androidx.navigation.NavHost] has called
         * [androidx.navigation.NavHostController.setViewModelStore].
         */
        get() {
            check(savedStateRegistryAttached) {
                "You cannot access the NavBackStackEntry's ViewModels until it is added to " +
                    "the NavController's back stack (i.e., the Lifecycle of the " +
                    "NavBackStackEntry reaches the CREATED state)."
            }
            check(platformLifecycle.currentState != Lifecycle.State.DESTROYED) {
                "You cannot access the NavBackStackEntry's ViewModels after the " +
                    "NavBackStackEntry is destroyed."
            }
            checkNotNull(viewModelStoreProvider) {
                "You must call setViewModelStore() on your NavHostController before " +
                    "accessing the ViewModelStore of a navigation graph."
            }
            return viewModelStoreProvider.getViewModelStore(id)
        }
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory = defaultFactory
    override val defaultViewModelCreationExtras: CreationExtras
    get(){
        val extras = MutableCreationExtras()
        extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
        extras[VIEW_MODEL_STORE_OWNER_KEY] = this
        arguments?.let { args ->
            extras[DEFAULT_ARGS_KEY] = args
        }
        return extras
    }
    override val platformSavedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun saveState(outBundle: Bundle) {
        savedStateRegistryController.performSave(outBundle)
    }
    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean {
        val arguments = arguments
        if (other == null || other !is NavBackStackEntry) return false
        return id == other.id && destination == other.destination &&
            platformLifecycle == other.platformLifecycle &&
            platformSavedStateRegistry == other.platformSavedStateRegistry &&
            (
                arguments == other.arguments ||
                        arguments?.keySet
                            ?.all { arguments[it] == other.arguments?.get(it) } == true
                    )
    }
    @Suppress("DEPRECATION")
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + destination.hashCode()
        immutableArgs?.keySet?.forEach {
            result = 31 * result + immutableArgs.get(it).hashCode()
        }
        result = 31 * result + platformLifecycle.hashCode()
        result = 31 * result + platformSavedStateRegistry.hashCode()
        return result
    }
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(this::class.simpleName)
        sb.append("($id)")
        sb.append(" destination=")
        sb.append(destination)
        return sb.toString()
    }
    /**
     * Used to create the {SavedStateViewModel}
     */
    private class NavResultSavedStateFactory(
        owner: SavedStateRegistryOwner
    ) : AbstractSavedStateViewModelFactory(owner, null) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: KClass<T>,
            handle: SavedStateHandle
        ): T {
            return SavedStateViewModel(handle) as T
        }
    }
    private class SavedStateViewModel(val handle: SavedStateHandle) : ViewModel()
}