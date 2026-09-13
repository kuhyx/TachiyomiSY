package tachiyomi.core.common.util.lang

public fun Boolean.toLong(): Long = if (this) 1L else 0L
