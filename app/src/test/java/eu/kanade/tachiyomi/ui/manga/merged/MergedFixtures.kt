package eu.kanade.tachiyomi.ui.manga.merged

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.EditMergedSettingsDialogBinding
import exh.source.MERGED_SOURCE_ID
import org.robolectric.Robolectric
import tachiyomi.domain.manga.model.MergedMangaReference

/** A reference [id] to a member of source [sourceId]; the merged source makes it the self reference. */
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

/** A started AppCompat activity themed like the app, whose inflater understands `app:srcCompat`. */
internal fun themedActivity(): AppCompatActivity {
    val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
    controller.get().setTheme(R.style.Theme_Tachiyomi)
    return controller.setup().get()
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
