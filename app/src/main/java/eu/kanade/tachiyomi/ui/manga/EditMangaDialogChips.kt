package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
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
            closeIcon?.setTint(context.getResourceColor(R.attr.colorAccent))
            setOnCloseIconClickListener {
                removeView(this)
            }
        }
    }.forEach {
        addView(it)
    }

    val addTagChip = Chip(context).apply {
        setText(SYMR.strings.add_tags.getString(context))

        chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_add_24dp)?.apply {
            isChipIconVisible = true
            setTint(context.getResourceColor(R.attr.colorAccent))
        }

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

internal fun ChipGroup.getTextStrings(): List<String> = children.mapNotNull {
    if (it is Chip && !it.text.toString().contains(context.stringResource(SYMR.strings.add_tags), ignoreCase = true)) {
        it.text.toString()
    } else {
        null
    }
}.toList()
