package net.smarttuner.kaffeeverde.core.ref

expect class WeakReference<T : Any> {
    /**
     * Creates a weak reference object pointing to an object. Weak reference doesn't prevent
     * removing object, and is nullified once object is collected.
     */
    constructor(referred: T)

    /**
     * Clears reference to an object.
     */
    public fun clear()
    /**
     * Returns either reference to an object or null, if it was collected.
     */
    @Suppress("UNCHECKED_CAST")
    public fun get(): T?

    /**
     * Returns either reference to an object or null, if it was collected.
     */
    public val value: T?
        get
}