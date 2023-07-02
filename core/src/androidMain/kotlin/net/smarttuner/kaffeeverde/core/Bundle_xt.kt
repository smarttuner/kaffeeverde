package net.smarttuner.kaffeeverde.core

fun Bundle.toAndroidBundle() = android.os.Bundle().apply {
    forEach { (key, value) ->
        when (value) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Double -> putDouble(key, value)
            is Boolean -> putBoolean(key, value)
            is HashMap<*,*> -> putSerializable(key, value)
            else -> Unit
        }
    }
}

fun android.os.Bundle.toKVBundle() = Bundle().apply {
    keySet().forEach { key ->
        val any = when (val value = get(key)) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Double -> putDouble(key, value)
            is Boolean -> putBoolean(key, value)
            else -> Unit
        }
    }
}
