package net.smarttuner.kaffeeverde.core

fun String.toUri(): Uri {
    return Uri.parse(this)!!
}