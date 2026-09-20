package tachiyomi.presentation.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.core.common.i18n.stringResource

/** The string for [resource] in the current context. */
@Composable
@ReadOnlyComposable
public fun stringResource(resource: StringResource): String = LocalContext.current.stringResource(resource)

/** The string for [resource] formatted with [args]. */
@Composable
@ReadOnlyComposable
public fun stringResource(resource: StringResource, vararg args: Any): String =
    LocalContext.current.stringResource(resource, *args)

/** The plural form of [resource] for [count]. */
@Composable
@ReadOnlyComposable
public fun pluralStringResource(resource: PluralsResource, count: Int): String =
    LocalContext.current.pluralStringResource(resource, count)

/** The plural form of [resource] for [count], formatted with [args]. */
@Composable
@ReadOnlyComposable
public fun pluralStringResource(resource: PluralsResource, count: Int, vararg args: Any): String =
    LocalContext.current.pluralStringResource(resource, count, *args)
