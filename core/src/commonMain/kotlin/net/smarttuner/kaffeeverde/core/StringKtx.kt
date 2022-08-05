package net.smarttuner.kaffeeverde.core

import net.smarttuner.kaffeeverde.core.Uri

fun String.toUri(): Uri? {
    return Uri.parse(this)
}