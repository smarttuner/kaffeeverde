package net.smarttuner.kaffeeverde.core.ref

actual class WeakReference<T : Any> {

    val _intValue: WeakReference<T>
    /**
     * Creates a weak reference object pointing to an object. Weak reference doesn't prevent
     * removing object, and is nullified once object is collected.
     */
    actual constructor(referred: T) {
        _intValue = WeakReference(referred)
    }

    /**
     * Clears reference to an object.
     */
    actual fun clear() {
        _intValue.clear()
    }

    /**
     * Returns either reference to an object or null, if it was collected.
     */
    @Suppress("UNCHECKED_CAST")
    actual fun get(): T? {
        return _intValue.get()
    }

    /**
     * Returns either reference to an object or null, if it was collected.
     */
    actual val value: T?
        get() = _intValue.value

}