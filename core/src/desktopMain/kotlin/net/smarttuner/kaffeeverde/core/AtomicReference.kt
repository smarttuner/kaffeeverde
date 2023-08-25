package net.smarttuner.kaffeeverde.core

import java.util.concurrent.atomic.AtomicReference

@SinceKotlin("1.9")
public actual class AtomicReference<T : Any> actual constructor(value: T?) {
    private val atomicRef = AtomicReference(value)

    actual var value: T?
        get() = atomicRef.get()
        set(value) {
            atomicRef.set(value)
        }

    actual fun getAndSet(newValue: T): T? {
        while (true) {
            val oldValue = value
            if (compareAndSet(oldValue, newValue)) return oldValue
        }
    }

    actual fun compareAndSet(expected: T?, newValue: T): Boolean {
        return atomicRef.compareAndSet(expected, newValue)
    }

    actual fun compareAndExchange(expected: T, newValue: T): Boolean {
        while (true) {
            val oldValue = value
            return compareAndSet(oldValue, newValue)
        }
    }

    actual override fun toString(): String {
        return "AtomicReference(value=${value})"
    }

    actual fun get(): Any? {
        return value
    }
}