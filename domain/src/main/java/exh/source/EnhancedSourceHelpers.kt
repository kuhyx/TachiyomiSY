package exh.source

import eu.kanade.tachiyomi.source.Source
import tachiyomi.domain.util.InlinedOnly
import kotlin.reflect.KClass

/*
 * Views through an [EnhancedHttpSource] (SY): the app may wrap an extension's
 * source in an enhanced implementation; these pick the half a caller wants.
 */

/** The source in use behind an [EnhancedHttpSource] (enhanced or original per the setting), else this. */
public fun Source.getMainSource(): Source = if (this is EnhancedHttpSource) {
    this.source()
} else {
    this
}

/** [getMainSource] as a [type], or null when the source in use is not one. */
public fun <T : Source> Source.getMainSource(type: KClass<T>): T? {
    val main = getMainSource()
    return if (type.isInstance(main)) type.java.cast(main) else null
}

/** [getMainSource] cast to [T], or null when the source in use is not a [T]. */
@InlinedOnly
@JvmName("getMainSourceInline")
public inline fun <reified T : Source> Source.getMainSource(): T? = getMainSource(T::class)

/** The extension's own source behind an [EnhancedHttpSource], else this. */
public fun Source.getOriginalSource(): Source = if (this is EnhancedHttpSource) {
    this.originalSource
} else {
    this
}

/** The app's enhanced implementation behind an [EnhancedHttpSource], else this. */
public fun Source.getEnhancedSource(): Source = if (this is EnhancedHttpSource) {
    this.enhancedSource
} else {
    this
}

/** Whether this source, or either half of an [EnhancedHttpSource], is a [type]. */
public fun Source.anyIs(type: KClass<*>): Boolean = if (this is EnhancedHttpSource) {
    type.isInstance(originalSource) || type.isInstance(enhancedSource)
} else {
    type.isInstance(this)
}

/** Whether this source, or either half of an [EnhancedHttpSource], is a [T]. */
@InlinedOnly
public inline fun <reified T : Any> Source.anyIs(): Boolean = anyIs(T::class)
