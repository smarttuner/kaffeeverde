package net.smarttuner.kaffeeverde.core

import kotlin.concurrent.AtomicInt

actual class AtomicInteger actual constructor(value_: Int)  {

    private val _value = AtomicInt(value_)

    
    actual val andIncrement: Int
        get() = _value.addAndGet(1)
    
    actual val andDecrement: Int
        get() = _value.addAndGet(-1)

    actual fun get(): Int {
        return _value.value
    }

    actual fun set(newValue: Int) {
        _value.value = newValue
    }

    actual fun getAndSet(newValue: Int): Int {
        val oldValue = _value.value
        _value.value = newValue
        return oldValue
    }

    actual fun compareAndSet(expect: Int, update: Int): Boolean {
        return _value.compareAndSet(expect, update)
    }

    actual fun getAndAdd(delta: Int): Int {
        val oldValue = _value.value
        _value.value += delta
        return oldValue
    }

    actual fun incrementAndGet(): Int {
        return addAndGet(1)
    }

    actual fun decrementAndGet(): Int {
        return addAndGet(-1)
    }

    actual fun toByte(): Byte {
        return _value.value.toByte()
    }

    actual fun toChar(): Char {
        return _value.value.toChar()
    }

    actual fun toDouble(): Double {
        return _value.value.toDouble()
    }

    actual fun toFloat(): Float {
        return _value.value.toFloat()
    }

    actual fun toInt(): Int {
        return _value.value.toInt()
    }

    actual fun toLong(): Long {
        return _value.value.toLong()
    }

    actual fun toShort(): Short {
        return _value.value.toShort()
    }

    actual var value: Int
        get() = _value.value
        set(value) {
            _value.value = value
        }

    actual fun addAndGet(delta: Int): Int {
        return _value.addAndGet(delta)
    }
}