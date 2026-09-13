package tachiyomi.core.common.util.lang

/** 1 for true, 0 for false. */
public fun Boolean.toLong(): Long = if (this) 1L else 0L
