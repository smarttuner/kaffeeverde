package net.smarttuner.kaffeeverde.core
/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.reflect.*
import kotlin.concurrent.*
import kotlin.native.concurrent.*

@SinceKotlin("1.9")
expect class AtomicReference<T : Any> {

    /**
     * Creates a new atomic reference pointing to the [given value][value].
     *
     * @throws InvalidMutabilityException with legacy MM if reference is not frozen.
     */
    constructor(value: T? = null)


    public var value: T?
    /**
     * Atomically sets the value to the given [new value][newValue] and returns the old value.
     */
    public fun getAndSet(newValue: T): T?

    /**
     * Atomically sets the value to the given [new value][newValue] if the current value equals the [expected value][expected],
     * returns true if the operation was successful and false only if the current value was not equal to the expected value.
     *
     * Provides sequential consistent ordering guarantees and cannot fail spuriously.
     *
     * Comparison of values is done by reference.
     */
    public fun compareAndSet(expected: T?, newValue: T): Boolean

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
    public fun compareAndExchange(expected: T, newValue: T): Boolean

    /**
     * Returns the string representation of the current [value].
     */
    public override fun toString(): String
    fun get(): Any?

}
