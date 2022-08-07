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
/**
 *
 * === WARNING ===
 *
 * The original source file used for this file is available here:
 * https://android.googlesource.com/platform/frameworks/support/+/HEAD/lifecycle/lifecycle-runtime/src/main/java/androidx/lifecycle/LifecycleRegistry.java
 *
 * The content of this file is a port of the original work with some additions
 *
 */
package net.smarttuner.kaffeeverde.lifecycle

import net.smarttuner.kaffeeverde.core.FastSafeIterableMap
import net.smarttuner.kaffeeverde.core.annotation.MainThread
import net.smarttuner.kaffeeverde.core.annotation.SuppressLint
import net.smarttuner.kaffeeverde.core.ref.WeakReference
import net.smarttuner.kaffeeverde.lifecycle.*

/**
 * An implementation of [Lifecycle] that can handle multiple observers.
 *
 *
 * It is used by Fragments and Support Library Activities. You can also directly use it if you have
 * a custom LifecycleOwner.
 */
class LifecycleRegistry constructor(
    provider: LifecycleOwner,
    enforceMainThread: Boolean
) :
    Lifecycle() {
    /**
     * Custom list that keeps observers and can handle removals / additions during traversal.
     *
     * Invariant: at any moment of time for observer1 & observer2:
     * if addition_order(observer1) < addition_order(observer2), then
     * state(observer1) >= state(observer2),
     */
    private var mObserverMap: FastSafeIterableMap<LifecycleObserver, ObserverWithState> =
        FastSafeIterableMap<LifecycleObserver, ObserverWithState>()

    /**
     * Current state
     */
    private var mState: State

    /**
     * The provider that owns this Lifecycle.
     * Only WeakReference on LifecycleOwner is kept, so if somebody leaks Lifecycle, they won't leak
     * the whole Fragment / Activity. However, to leak Lifecycle object isn't great idea neither,
     * because it keeps strong references on all other listeners, so you'll leak all of them as
     * well.
     */
    private val mLifecycleOwner: WeakReference<LifecycleOwner>
    private var mAddingObserverCounter = 0
    private var mHandlingEvent = false
    private var mNewEventOccurred = false

    // we have to keep it for cases:
    // void onStart() {
    //     mRegistry.removeObserver(this);
    //     mRegistry.add(newObserver);
    // }
    // newObserver should be brought only to CREATED state during the execution of
    // this onStart method. our invariant with mObserverMap doesn't help, because parent observer
    // is no longer in the map.
    private val mParentStates: ArrayList<State> =
        ArrayList()
    private val mEnforceMainThread: Boolean

    /**
     * Creates a new LifecycleRegistry for the given provider.
     *
     *
     * You should usually create this inside your LifecycleOwner class's constructor and hold
     * onto the same instance.
     *
     * @param provider The owner LifecycleOwner
     */
    constructor(provider: LifecycleOwner) : this(provider, true)

    init {
        mLifecycleOwner = WeakReference(provider)
        mState = State.INITIALIZED
        mEnforceMainThread = enforceMainThread
    }

    /**
     * Moves the Lifecycle to the given state and dispatches necessary events to the observers.
     *
     * @param state new state
     */
    @MainThread
    @Deprecated("Use {@link #setCurrentState(State)}.")
    fun markState(state: State) {
        enforceMainThreadIfNeeded("markState")
        currentState = state
    }

    /**
     * Sets the current state and notifies the observers.
     *
     *
     * Note that if the `currentState` is the same state as the last call to this method,
     * calling this method has no effect.
     *
     * @param event The event that was received
     */
    fun handleLifecycleEvent(event: Event) {
        enforceMainThreadIfNeeded("handleLifecycleEvent")
        moveToState(event.targetState)
    }

    private fun moveToState(next: State) {
        if (mState == next) {
            return
        }
        if (mState == State.INITIALIZED && next == State.DESTROYED) {
            throw IllegalStateException("no event down from $mState")
        }
        mState = next
        if (mHandlingEvent || mAddingObserverCounter != 0) {
            mNewEventOccurred = true
            // we will figure out what to do on upper level.
            return
        }
        mHandlingEvent = true
        sync()
        mHandlingEvent = false
        if (mState == State.DESTROYED) {
            mObserverMap =
                FastSafeIterableMap<LifecycleObserver, ObserverWithState>()
        }
    }

    private val isSynced: Boolean
        get() {
            if (mObserverMap.size() == 0) {
                return true
            }
            val eldestObserverState: State? =
                mObserverMap.eldest()?.value?.mState
            val newestObserverState: State? =
                mObserverMap.newest()?.value?.mState
            return eldestObserverState == newestObserverState && mState == newestObserverState
        }

    private fun calculateTargetState(observer: LifecycleObserver): State {
        val previous: Map.Entry<LifecycleObserver, ObserverWithState>? =
            mObserverMap.ceil(observer)
        val siblingState: State? =
            previous?.value?.mState
        val parentState: State? =
            if (!mParentStates.isEmpty()) mParentStates.get(mParentStates.size - 1) else null
        return min(min(mState, siblingState), parentState)
    }

    override fun addObserver(observer: LifecycleObserver) {
        enforceMainThreadIfNeeded("addObserver")
        val initialState: State =
            if (mState == State.DESTROYED) State.DESTROYED else State.INITIALIZED
        val statefulObserver = ObserverWithState(observer, initialState)
        val previous: ObserverWithState? = mObserverMap.putIfAbsent(observer, statefulObserver)
        if (previous != null) {
            return
        }
        val lifecycleOwner: LifecycleOwner = mLifecycleOwner.get()
            ?: // it is null we should be destroyed. Fallback quickly
            return
        val isReentrance = mAddingObserverCounter != 0 || mHandlingEvent
        var targetState: State = calculateTargetState(observer)
        mAddingObserverCounter++
        while (statefulObserver.mState.compareTo(targetState) < 0
            && mObserverMap.contains(observer)
        ) {
            pushParentState(statefulObserver.mState)
            val event: Event =
                Event.upFrom(statefulObserver.mState)
                    ?: throw IllegalStateException("no event up from " + statefulObserver.mState)
            statefulObserver.dispatchEvent(lifecycleOwner, event)
            popParentState()
            // mState / subling may have been changed recalculate
            targetState = calculateTargetState(observer)
        }
        if (!isReentrance) {
            // we do sync only on the top level.
            sync()
        }
        mAddingObserverCounter--
    }

    private fun popParentState() {
        mParentStates.removeAt(mParentStates.size - 1)
    }

    private fun pushParentState(state: State) {
        mParentStates.add(state)
    }

    override fun removeObserver(observer: LifecycleObserver) {
        enforceMainThreadIfNeeded("removeObserver")
        // we consciously decided not to send destruction events here in opposition to addObserver.
        // Our reasons for that:
        // 1. These events haven't yet happened at all. In contrast to events in addObservers, that
        // actually occurred but earlier.
        // 2. There are cases when removeObserver happens as a consequence of some kind of fatal
        // event. If removeObserver method sends destruction events, then a clean up routine becomes
        // more cumbersome. More specific example of that is: your LifecycleObserver listens for
        // a web connection, in the usual routine in OnStop method you report to a server that a
        // session has just ended and you close the connection. Now let's assume now that you
        // lost an internet and as a result you removed this observer. If you get destruction
        // events in removeObserver, you should have a special case in your onStop method that
        // checks if your web connection died and you shouldn't try to report anything to a server.
        mObserverMap.remove(observer)
    }

    /**
     * The number of observers.
     *
     * @return The number of observers.
     */
    val observerCount: Int
        get() {
            enforceMainThreadIfNeeded("getObserverCount")
            return mObserverMap.size()
        }

    /**
     * Moves the Lifecycle to the given state and dispatches necessary events to the observers.
     *
     * @param state new state
     */
    @set:MainThread
    override var currentState: State
        get() = mState
        set(state) {
            enforceMainThreadIfNeeded("setCurrentState")
            moveToState(state)
        }

    private fun forwardPass(lifecycleOwner: LifecycleOwner) {
        val ascendingIterator: Iterator<Map.Entry<LifecycleObserver, ObserverWithState>?> =
            mObserverMap.iteratorWithAdditions()
        while (ascendingIterator.hasNext() && !mNewEventOccurred) {
            val entry = ascendingIterator.next() ?: return
            val key = entry.key
            val observer = entry.value
            while (observer.mState < mState && !mNewEventOccurred
                && mObserverMap.contains(key)
            ) {
                pushParentState(observer.mState)
                val event: Event =
                    Event.upFrom(observer.mState)
                        ?: throw IllegalStateException("no event up from " + observer.mState)
                observer.dispatchEvent(lifecycleOwner, event)
                popParentState()
            }
        }
    }

    private fun backwardPass(lifecycleOwner: LifecycleOwner) {
        val descendingIterator: Iterator<Map.Entry<LifecycleObserver, ObserverWithState>?> =
            mObserverMap.descendingIterator()
        while (descendingIterator.hasNext() && !mNewEventOccurred) {
            val entry = descendingIterator.next() ?: return
            val key = entry.key
            val observer = entry.value
            while (observer.mState.compareTo(mState) > 0 && !mNewEventOccurred
                && mObserverMap.contains(key)
            ) {
                val event: Event =
                    Event.downFrom(observer.mState)
                        ?: throw IllegalStateException("no event down from " + observer.mState)
                pushParentState(event.targetState)
                observer.dispatchEvent(lifecycleOwner, event)
                popParentState()
            }
        }
    }

    // happens only on the top of stack (never in reentrance),
    // so it doesn't have to take in account parents
    private fun sync() {
        val lifecycleOwner: LifecycleOwner = mLifecycleOwner.get()
            ?: throw IllegalStateException(
                "LifecycleOwner of this LifecycleRegistry is already"
                        + "garbage collected. It is too late to change lifecycle state."
            )
        while (!isSynced) {
            mNewEventOccurred = false
            // no need to check eldest for nullability, because isSynced does it for us.
            val eldestValue = mObserverMap.eldest()
            if (eldestValue != null && mState < eldestValue.value.mState) {
                backwardPass(lifecycleOwner)
            }
            val newest: Map.Entry<LifecycleObserver, ObserverWithState>? =
                mObserverMap.newest()
            if (!mNewEventOccurred && newest != null && mState > newest.value.mState) {
                forwardPass(lifecycleOwner)
            }
        }
        mNewEventOccurred = false
    }

    @SuppressLint("RestrictedApi")
    private fun enforceMainThreadIfNeeded(methodName: String) {
        //empty implementation: it is not possible to know the type of current thread with K/N
//        if (mEnforceMainThread) {
//            if (!ArchTaskExecutor.getInstance().isMainThread()) {
//                throw IllegalStateException(
//                    "Method " + methodName + " must be called on the "
//                            + "main thread"
//                )
//            }
//        }
    }

    internal class ObserverWithState(
        observer: LifecycleObserver,
        initialState: State
    ) {
        var mState: State
        var mLifecycleObserver: LifecycleObserver

        init {
            mLifecycleObserver = observer
            mState = initialState
        }

        fun dispatchEvent(
            owner: LifecycleOwner,
            event: Event
        ) {
            val newState: State = event.targetState
            mState = min(mState, newState)
            mLifecycleObserver.onStateChanged(owner, event)
            mState = newState
        }
    }

    companion object {
        /**
         * Creates a new LifecycleRegistry for the given provider, that doesn't check
         * that its methods are called on the threads other than main.
         *
         *
         * LifecycleRegistry is not synchronized: if multiple threads access this `LifecycleRegistry`, it must be synchronized externally.
         *
         *
         * Another possible use-case for this method is JVM testing, when main thread is not present.
         */
        fun createUnsafe(owner: LifecycleOwner): LifecycleRegistry {
            return LifecycleRegistry(owner, false)
        }

        fun min(
            state1: State,
            state2: State?
        ): State {
            return if (state2 != null && state2.compareTo(state1) < 0) state2 else state1
        }
    }
}