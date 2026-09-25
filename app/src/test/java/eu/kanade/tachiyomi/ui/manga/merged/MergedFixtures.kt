package eu.kanade.tachiyomi.ui.manga.merged

import android.app.Activity
import android.view.View
import androidx.activity.ComponentActivity
import androidx.appcompat.view.ContextThemeWrapper
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.EditMergedSettingsDialogBinding
import exh.source.MERGED_SOURCE_ID
import org.robolectric.Robolectric
import tachiyomi.domain.manga.model.MergedMangaReference

/** A reference [id] to a member of source [sourceId]; the merged entry's own one when [sourceId] is the merged source. */
internal fun reference(
    id: Long,
    sourceId: Long = 7L,
    sortMode: Int = MergedMangaReference.CHAPTER_SORT_NO_DEDUPE,
    isInfoManga: Boolean = false,
    priority: Int = id.toInt(),
): MergedMangaReference = MergedMangaReference(
    id = id,
    isInfoManga = isInfoManga,
    getChapterUpdates = true,
    chapterSortMode = sortMode,
    chapterPriority = priority,
    downloadChapters = true,
    mergeId = 1L,
    mergeUrl = "/merged",
    mangaId = id,
    mangaUrl = "/m/$id",
    mangaSourceId = sourceId,
)

/** A started activity themed like the app, for inflating the dialog's views. */
internal fun themedActivity(): ContextThemeWrapper {
    val activity: Activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    return ContextThemeWrapper(activity, R.style.Theme_Tachiyomi)
}

/** Lays [binding] out at a phone size so the recycler binds its rows. */
internal fun layOut(binding: EditMergedSettingsDialogBinding) {
    val width = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
    val height = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
    binding.root.measure(width, height)
    binding.root.layout(0, 0, 1080, 1920)
}

/** The merged entry's self reference with [sortMode]. */
internal fun selfReference(sortMode: Int = MergedMangaReference.CHAPTER_SORT_PRIORITY): MergedMangaReference =
    reference(id = 9L, sourceId = MERGED_SOURCE_ID, sortMode = sortMode)
