package net.smarttuner.kaffeeverde.core

fun <K, T> MutableMap<K, T>.putIfAbsent(key: K, value: T): T?{
    if(this.containsKey(key)) return this[key]
    this[key] = value
    return null
}
