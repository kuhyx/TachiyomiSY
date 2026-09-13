package exh.util

/** This collection, or null when it is empty. */
public fun <C : Collection<R>, R> C.nullIfEmpty(): C? = ifEmpty { null }
