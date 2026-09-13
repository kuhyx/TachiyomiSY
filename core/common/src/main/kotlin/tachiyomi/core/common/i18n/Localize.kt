package tachiyomi.core.common.i18n

import android.content.Context
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.Plural
import dev.icerock.moko.resources.desc.PluralFormatted
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc

/** The localized string for [resource]. */
public fun Context.stringResource(resource: StringResource): String =
    StringDesc.Resource(resource).toString(this).fixed()

/** The localized string for [resource] formatted with [args]. */
public fun Context.stringResource(resource: StringResource, vararg args: Any): String =
    StringDesc.ResourceFormatted(resource, *args).toString(this).fixed()

/** The localized plural for [count]. */
public fun Context.pluralStringResource(resource: PluralsResource, count: Int): String =
    StringDesc.Plural(resource, count).toString(this).fixed()

/** The localized plural for [count] formatted with [args]. */
public fun Context.pluralStringResource(resource: PluralsResource, count: Int, vararg args: Any): String =
    StringDesc.PluralFormatted(resource, count, *args).toString(this).fixed()

// Workaround for https://github.com/icerockdev/moko-resources/issues/337 until it is fixed upstream.
private fun String.fixed() =
    this.replace("""\""", """"""")
