package net.smarttuner.kaffeeverde.lifecycle

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras


@Suppress("SingletonConstructor")
open class NewInstanceFactory : ViewModelProvider.Factory {
    //        @Suppress("DocumentExceptions")
//        override fun <T : ViewModel> create(modelClass: KClass<T>): T {
//            return try {
//                modelClass.newInstance()
//            } catch (e: InstantiationException) {
//                throw RuntimeException("Cannot create an instance of $modelClass", e)
//            } catch (e: IllegalAccessException) {
//                throw RuntimeException("Cannot create an instance of $modelClass", e)
//            }
//        }
    companion object {
        private var sInstance: NewInstanceFactory? = null
        /**
         * @suppress
         * Retrieve a singleton instance of NewInstanceFactory.
         *
         * @return A valid [NewInstanceFactory]
         */

        val instance: NewInstanceFactory

            get() {
                if (sInstance == null) {
                    sInstance = NewInstanceFactory()
                }
                return sInstance!!
            }
        object ViewModelKeyImpl : CreationExtras.Key<String>
        /**
         * A [CreationExtras.Key] to get a key associated with a requested
         * `ViewModel` from [CreationExtras]
         *
         *  `ViewModelProvider` automatically puts a key that was passed to
         *  `ViewModelProvider.get(key, MyViewModel::class.java)`
         *  or generated in `ViewModelProvider.get(MyViewModel::class.java)` to the `CreationExtras` that
         *  are passed to [ViewModelProvider.Factory].
         */

        val VIEW_MODEL_KEY: CreationExtras.Key<String> = ViewModelKeyImpl
    }
}
/**
 * [Factory] which may create [AndroidViewModel] and
 * [ViewModel], which have an empty constructor.
 *
 * @param application an application to pass in [AndroidViewModel]
 */
open class NonAndroidViewModelFactory
private constructor(
    @Suppress("UNUSED_PARAMETER") unused: Int,
) : NewInstanceFactory() {
    /**
     * Constructs this factory.
     * When a factory is constructed this way, a component for which [ViewModel] is created
     * must provide an [Application] by [APPLICATION_KEY] in [CreationExtras], otherwise
     *  [IllegalArgumentException] will be thrown from [create] method.
     */
    @Suppress("SingletonConstructor")
    constructor() : this( 0)
    companion object {
        internal fun defaultFactory(owner: ViewModelStoreOwner): ViewModelProvider.Factory =
            if (owner is HasDefaultViewModelProviderFactory)
                owner.defaultViewModelProviderFactory else instance
        internal const val DEFAULT_KEY = "androidx.lifecycle.ViewModelProvider.DefaultKey"
        private var sInstance: NonAndroidViewModelFactory? = null
        /**
         * Retrieve a singleton instance of AndroidViewModelFactory.
         *
         * @param application an application to pass in [AndroidViewModel]
         * @return A valid [AndroidViewModelFactory]
         */
        fun getInstance(): NonAndroidViewModelFactory {
            if (sInstance == null) {
                sInstance = NonAndroidViewModelFactory()
            }
            return sInstance!!
        }
//            private object ApplicationKeyImpl : CreationExtras.Key<Application>
//            /**
//             * A [CreationExtras.Key] to query an application in which ViewModel is being created.
//             */
//
//            val APPLICATION_KEY: CreationExtras.Key<Application> = ApplicationKeyImpl
    }
}