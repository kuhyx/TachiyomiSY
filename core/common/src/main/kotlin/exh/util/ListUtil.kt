package exh.util

public fun <C : Collection<R>, R> C.nullIfEmpty(): C? = ifEmpty { null }
