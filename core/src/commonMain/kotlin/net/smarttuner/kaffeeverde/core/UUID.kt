package net.smarttuner.kaffeeverde.core

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

object UUID {
    fun randomUUID(): Uuid = uuid4()
}