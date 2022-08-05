package net.smarttuner.kaffeeverde.core

expect class AtomicInteger(value_: Int) {
    var value: Int

    /**
     * Atomically increments by one the current value.
     *
     * @return the previous value
     */
    val andIncrement: Int

    /**
     * Atomically decrements by one the current value.
     *
     * @return the previous value
     */
    val andDecrement: Int

    /**
     * Gets the current value.
     *
     * @return the current value
     */
    fun get(): Int

    /**
     * Sets to the given value.
     *
     * @param newValue the new value
     */
    fun set(newValue: Int)

    /**
     * Atomically sets to the given value and returns the old value.
     *
     * @param newValue the new value
     * @return the previous value
     */
    fun getAndSet(newValue: Int): Int

    /**
     * Atomically sets the value to the given updated value
     * if the current value `==` the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     * @return true if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    fun compareAndSet(expect: Int, update: Int): Boolean

    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the previous value
     */
    fun getAndAdd(delta: Int): Int

    /**
     * Atomically increments by one the current value.
     *
     * @return the updated value
     */
    fun incrementAndGet(): Int

    /**
     * Atomically decrements by one the current value.
     *
     * @return the updated value
     */
    fun decrementAndGet(): Int

    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the updated value
     */
    fun addAndGet(delta: Int): Int
    fun toByte(): Byte
    fun toChar(): Char
    fun toDouble(): Double
    fun toFloat(): Float
    fun toInt(): Int
    fun toLong(): Long
    fun toShort(): Short

    /**
     * Returns the String representation of the current value.
     * @return the String representation of the current value.
     */
    override fun toString(): String
}
