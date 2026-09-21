package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

internal class FrontPageCategoriesDialogState(
    preference: String,
) {
    // One flag per EhCategory, in enum order; the preference stores "disabled" so the flags are inverted.
    val enabled: SnapshotStateList<Boolean> = preference.split(",").map { !it.toBoolean() }.toMutableStateList()

    fun toPreference() = enabled.joinToString(separator = ",") { (!it).toString() }
}
