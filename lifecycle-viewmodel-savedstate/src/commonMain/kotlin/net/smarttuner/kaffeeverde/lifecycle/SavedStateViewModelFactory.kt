/*
 * Copyright 2018 The Android Open Source Project
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
 * [androidx.lifecycle.ViewModelProvider.Factory] that can create ViewModels accessing and
 * contributing to a saved state via [SavedStateHandle] received in a constructor.
 * If `defaultArgs` bundle was passed into the constructor, it will provide default
 * values in `SavedStateHandle`.
 *
 * If ViewModel is instance of [androidx.lifecycle.AndroidViewModel], it looks for a
 * constructor that receives an [Application] and [SavedStateHandle] (in this order),
 * otherwise it looks for a constructor that receives [SavedStateHandle] only.
 * [androidx.lifecycle.AndroidViewModel] is only supported if you pass a non-null
 * [Application] instance.
 */
/*
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/savedstate/savedstate/src/main/java/androidx/savedstate/SavedStateRegistryOwner.kt * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.lifecycle

import net.smarttuner.kaffeeverde.core.Bundle
import net.smarttuner.kaffeeverde.lifecycle.Lifecycle
import net.smarttuner.kaffeeverde.lifecycle.viewmodel.CreationExtras
import net.smarttuner.kaffeeverde.lifecycle.SavedStateRegistry
import net.smarttuner.kaffeeverde.lifecycle.SavedStateRegistryOwner

class SavedStateViewModelFactory : ViewModelProvider.OnRequeryFactory, ViewModelProvider.Factory {
    private val factory: ViewModelProvider.Factory
    private var defaultArgs: Bundle? = null
    private var lifecycle: Lifecycle? = null
    private var savedStateRegistry: SavedStateRegistry? = null
    /**
     * Constructs this factory.
     *
     * When a factory is constructed this way, a component for which [SavedStateHandle] is
     * scoped must have called [enableSavedStateHandles].
     * @see [createSavedStateHandle] docs for more details.
     */
    constructor() {
        factory = ViewModelProvider.NonAndroidViewModelFactory()
    }
    /**
     * Creates [SavedStateViewModelFactory].
     *
     * [androidx.lifecycle.ViewModel] created with this factory can access to saved state
     * scoped to the given `activity`.
     *
     * @param owner       [SavedStateRegistryOwner] that will provide restored state for created
     * [ViewModels][androidx.lifecycle.ViewModel]
     */
    constructor(
        owner: SavedStateRegistryOwner
    ) : this(owner, null)
    /**
     * Creates [SavedStateViewModelFactory].
     *
     * [androidx.lifecycle.ViewModel] created with this factory can access to saved state
     * scoped to the given `activity`.
     *
     * When a factory is constructed this way, if you add any [CreationExtras] those arguments will
     * be used instead of the state passed in here. It is not possible to mix the arguments
     * received here with the [CreationExtras].
     *
     * @param owner       [SavedStateRegistryOwner] that will provide restored state for created
     * [ViewModels][androidx.lifecycle.ViewModel]
     * @param defaultArgs values from this `Bundle` will be used as defaults by [SavedStateHandle]
     * if there is no previously saved state or previously saved state misses a value by such key.
     */
    constructor(owner: SavedStateRegistryOwner, defaultArgs: Bundle?) {
        savedStateRegistry = owner.savedStateRegistry
        lifecycle = owner._lifecycle
        this.defaultArgs = defaultArgs
        factory = ViewModelProvider.NonAndroidViewModelFactory()
    }
    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the provided extras do not provide a
     * [ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY]
     */
//    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
//        val key = extras[ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY]
//            ?: throw IllegalStateException(
//                "VIEW_MODEL_KEY must always be provided by ViewModelProvider"
//            )
//
//            val viewModel = if (lifecycle != null) {
//                create(key, modelClass)
//            } else {
//                throw IllegalStateException("SAVED_STATE_REGISTRY_OWNER_KEY and" +
//                        "VIEW_MODEL_STORE_OWNER_KEY must be provided in the creation extras to" +
//                        "successfully create a ViewModel.")
//            }
//        return viewModel
//
//    }
    /**
     * Creates a new instance of the given `Class`.
     *
     * @param key a key associated with the requested ViewModel
     * @param modelClass a `Class` whose instance is requested
     * @return a newly created ViewModel
     *
     * @throws UnsupportedOperationException if the there is no lifecycle
     */
//    fun <T : ViewModel> create(key: String, modelClass: KClass<T>): T {
//        // empty constructor was called.
//        if (lifecycle == null) {
//            throw UnsupportedOperationException(
//                "SavedStateViewModelFactory constructed with empty constructor supports only " +
//                        "calls to create(modelClass: Class<T>, extras: CreationExtras)."
//            )
//        }
//        val constructor: Constructor<T>? = if (isAndroidViewModel && application != null) {
//            findMatchingConstructor(modelClass, ANDROID_VIEWMODEL_SIGNATURE)
//        } else {
//            findMatchingConstructor(modelClass, VIEWMODEL_SIGNATURE)
//        }
//        // doesn't need SavedStateHandle
//        if (constructor == null) {
//            // If you are using a stateful constructor and no application is available, we
//            // use an instance factory instead.
//            return if (application != null) factory.create(modelClass)
//            else instance.create(modelClass)
//        }
//        val controller = LegacySavedStateHandleController.create(
//            savedStateRegistry, lifecycle, key, defaultArgs
//        )
//        val viewModel: T = if (isAndroidViewModel && application != null) {
//            newInstance(modelClass, constructor, application!!, controller.handle)
//        } else {
//            newInstance(modelClass, constructor, controller.handle)
//        }
//        viewModel.setTagIfAbsent(
//            AbstractSavedStateViewModelFactory.TAG_SAVED_STATE_HANDLE_CONTROLLER, controller
//        )
//        return viewModel
//    }
    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the given modelClass does not have a classname
     */
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        // ViewModelProvider calls correct create that support same modelClass with different keys
//        // If a developer manually calls this method, there is no "key" in picture, so factory
//        // simply uses classname internally as as key.
//        val canonicalName = modelClass.canonicalName
//            ?: throw IllegalArgumentException("Local and anonymous classes can not be ViewModels")
//        return create(canonicalName, modelClass)
//    }
    /**
     * @hide
     */
    
//    override fun onRequery(viewModel: ViewModel) {
//        // needed only for legacy path
//        if (lifecycle != null) {
//            LegacySavedStateHandleController.attachHandleIfNeeded(
//                viewModel,
//                savedStateRegistry,
//                lifecycle
//            )
//        }
//    }
}
//internal fun <T : ViewModel?> newInstance(
//    modelClass: Class<T>,
//    constructor: Constructor<T>,
//    vararg params: Any
//): T {
//    return try {
//        constructor.newInstance(*params)
//    } catch (e: IllegalAccessException) {
//        throw RuntimeException("Failed to access $modelClass", e)
//    } catch (e: InstantiationException) {
//        throw RuntimeException("A $modelClass cannot be instantiated.", e)
//    } catch (e: InvocationTargetException) {
//        throw RuntimeException(
//            "An exception happened in constructor of $modelClass", e.cause
//        )
//    }
//}
//private val ANDROID_VIEWMODEL_SIGNATURE = listOf<Class<*>>(
//    Application::class.java,
//    SavedStateHandle::class.java
//)
//private val VIEWMODEL_SIGNATURE = listOf<Class<*>>(SavedStateHandle::class.java)
// it is done instead of getConstructor(), because getConstructor() throws an exception
// if there is no such constructor, which is expensive
//internal fun <T> findMatchingConstructor(
//    modelClass: KClass<T>,
//    signature: List<KClass<*>>
//): Constructor<T>? {
//    for (constructor in modelClass.constructors) {
//        val parameterTypes = constructor.parameterTypes.toList()
//        if (signature == parameterTypes) {
//            @Suppress("UNCHECKED_CAST")
//            return constructor as Constructor<T>
//        }
//        if (signature.size == parameterTypes.size && parameterTypes.containsAll(signature)) {
//            throw UnsupportedOperationException(
//                "Class ${modelClass.simpleName} must have parameters in the proper " +
//                        "order: $signature"
//            )
//        }
//    }
//    return null
//}