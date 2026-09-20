package exh.util

import kotlin.math.floor

internal fun Float.floor(): Int = floor(this).toInt()

internal fun Double.floor(): Int = floor(this).toInt()

internal fun Int.nullIfZero() = takeUnless { it == 0 }

internal fun Long.nullIfZero() = takeUnless { it == 0L }
