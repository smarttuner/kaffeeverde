package net.smarttuner.kaffeeverde.core

actual class AtomicInteger actual constructor(value_: Int)  {

    private val _value = java.util.concurrent.atomic.AtomicInteger(value_)

    actual var value: Int
        get() = _value.get()
        set(value) {
            _value.set(value)
        }

    actual val andIncrement: Int
        get() = _value.getAndIncrement()

    actual val andDecrement: Int
        get() = _value.getAndDecrement()

    actual fun get(): Int {
        return value
    }

    actual fun set(newValue: Int) {
        value = newValue
    }

    actual fun getAndSet(newValue: Int): Int {
        return _value.getAndSet(newValue)
    }

    actual fun compareAndSet(expect: Int, update: Int): Boolean {
        return _value.compareAndSet(expect, update)
    }

    actual fun getAndAdd(delta: Int): Int {
        return _value.getAndAdd(delta)
    }

    actual fun incrementAndGet(): Int {
        return _value.incrementAndGet()
    }

    actual fun decrementAndGet(): Int {
        return _value.decrementAndGet()
    }

    actual fun addAndGet(delta: Int): Int {
        return _value.addAndGet(delta)
    }

    actual fun toByte(): Byte {
        return _value.toByte()
    }

    actual fun toChar(): Char {
        return _value.toChar()
    }

    actual fun toDouble(): Double {
        return _value.toDouble()
    }

    actual fun toFloat(): Float {
        return _value.toFloat()
    }

    actual fun toInt(): Int {
        return _value.toInt()
    }

    actual fun toLong(): Long {
        return _value.toLong()
    }

    actual fun toShort(): Short {
        return _value.toShort()
    }
}