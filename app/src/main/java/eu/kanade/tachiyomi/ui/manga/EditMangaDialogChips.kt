package eu.kanade.tachiyomi.ui.manga

import android.content.res.ColorStateList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.view.children
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.widget.materialdialogs.setTextInput
import exh.ui.metadata.adapters.MetadataUIUtil.getResourceColor
import exh.util.trimOrNull
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.api.get

internal fun ChipGroup.setChips(items: List<String>, scope: CoroutineScope) {
    removeAllViews()

    items.asSequence().map { item ->
        Chip(context).apply {
            text = item

            isCloseIconVisible = true
            // SY --> tinted through the chip, which holds the icon its style gives it
            closeIconTint = ColorStateList.valueOf(context.getResourceColor(R.attr.colorAccent))
            // SY <--
            setOnCloseIconClickListener {
                removeView(this)
            }
        }
    }.forEach {
        addView(it)
    }

    val addTagChip = Chip(context).apply {
        setText(SYMR.strings.add_tags.getString(context))

        // SY -->
        setChipIconResource(R.drawable.ic_add_24dp)
        isChipIconVisible = true
        chipIconTint = ColorStateList.valueOf(context.getResourceColor(R.attr.colorAccent))
        // SY <--

        setOnClickListener {
            var newTags: String? = null
            MaterialAlertDialogBuilder(context)
                .setTitle(SYMR.strings.add_tags.getString(context))
                .setMessage(SYMR.strings.multi_tags_comma_separated.getString(context))
                .setTextInput { newTags = it.trimOrNull() }
                .setPositiveButton(MR.strings.action_ok.getString(context)) { _, _ ->
                    newTags?.let {
                        setChips(items + it.split(",").map { it.trimOrNull() }.filterNotNull(), scope)
                    }
                }
                .setNegativeButton(MR.strings.action_cancel.getString(context), null)
                .show()
        }
    }
    addView(addTagChip)
}

// SY --> the group only ever holds chips (see setChips); the "add tags" one is not a tag
internal fun ChipGroup.getTextStrings(): List<String> = children.filterIsInstance<Chip>()
    .map { it.text.toString() }
    .filterNot { it.contains(context.stringResource(SYMR.strings.add_tags), ignoreCase = true) }
    .toList()
// SY <--
