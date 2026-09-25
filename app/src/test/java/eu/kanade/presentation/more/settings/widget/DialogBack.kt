package eu.kanade.presentation.more.settings.widget

import androidx.activity.ComponentDialog
import org.robolectric.shadows.ShadowDialog

/** Presses back inside the most recently shown Compose dialog, which is what dismisses it on a device. */
internal fun pressDialogBack() {
    val dialog = ShadowDialog.getLatestDialog() as ComponentDialog
    dialog.onBackPressedDispatcher.onBackPressed()
}
