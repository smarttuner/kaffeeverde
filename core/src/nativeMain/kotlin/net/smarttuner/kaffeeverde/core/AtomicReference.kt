package net.smarttuner.kaffeeverde.core

import kotlin.concurrent.AtomicReference

@SinceKotlin("1.9")
actual class AtomicReference<T : Any> {
    private val atomicRef: AtomicReference<T?>
    actual constructor(value: T?) {
        atomicRef = AtomicReference<T?>(value)
    }

    actual var value: T?
        get() = atomicRef.value
        set(value) {
            atomicRef.value = value
        }

    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    actual fun getAndSet(newValue: T): T? {
        return atomicRef.getAndSet(newValue)
    }

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by reference.
     */
    actual fun compareAndSet(expected: T?, newValue: T): Boolean {
        return atomicRef.compareAndSet(expected, newValue)
    }

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected]
     * and returns the old value in any case.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by reference.
     *
     * Legacy MM: if the [new value][newValue] value is not null, it must be frozen or permanent object.
     *
     * @throws InvalidMutabilityException with legacy MM if the value is not frozen or a permanent object
     */
    actual fun compareAndExchange(expected: T, newValue: T): Boolean {
        return atomicRef.compareAndSet(expected, newValue)
    }

    actual fun get(): Any? {
        return atomicRef.value
    }

}