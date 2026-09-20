// The widgets render a `ListPreference<*>` / `MultiSelectListPreference<*>`, so the values they
// hand back are star-projected; these casts restore the item's own `T`, which is the only type
// its preference and callbacks were built for.
@file:Suppress("UNCHECKED_CAST")

package eu.kanade.presentation.more.settings

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.Preference.PreferenceItem.ListPreference
import eu.kanade.presentation.more.settings.Preference.PreferenceItem.MultiSelectListPreference

/** Stores a star-projected [value] into the item's preference. */
internal fun <T> ListPreference<T>.internalSet(value: Any) = preference.set(value as T)

/** Runs the item's callback with a star-projected [value]. */
internal suspend fun <T> ListPreference<T>.internalOnValueChanged(value: Any) = onValueChanged(value as T)

/** The item's subtitle for a star-projected [value]. */
@Composable
internal fun <T> ListPreference<T>.internalSubtitleProvider(value: Any?, entries: Map<out Any?, String>) =
    subtitleProvider(value as T, entries as Map<T, String>)

/** Stores a star-projected [value] into the item's preference. */
internal fun <T> MultiSelectListPreference<T>.internalSet(value: Set<Any?>) = preference.set(value as Set<T>)

/** Runs the item's callback with a star-projected [value]. */
internal suspend fun <T> MultiSelectListPreference<T>.internalOnValueChanged(value: Set<Any?>) =
    onValueChanged(value as Set<T>)

/** The item's subtitle for a star-projected [value]. */
@Composable
internal fun <T> MultiSelectListPreference<T>.internalSubtitleProvider(
    value: Set<Any?>,
    entries: Map<out Any?, String>,
) = subtitleProvider(value as Set<T>, entries as Map<T, String>)
