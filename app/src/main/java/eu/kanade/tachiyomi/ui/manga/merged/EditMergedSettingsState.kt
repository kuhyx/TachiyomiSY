package eu.kanade.tachiyomi.ui.manga.merged

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.databinding.EditMergedSettingsDialogBinding
import eu.kanade.tachiyomi.util.system.toast
import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

@Stable
internal class EditMergedSettingsState(
    private val context: Context,
    private val onDeleteClick: (MergedMangaReference) -> Unit,
    private val onDismissRequest: () -> Unit,
    private val onPositiveClick: (List<MergedMangaReference>) -> Unit,
) : EditMergedMangaAdapter.EditMergedMangaItemListener {
    var mergedMangas: List<Pair<Manga?, MergedMangaReference>> by mutableStateOf(emptyList())
    var mergeReference: MergedMangaReference? by mutableStateOf(null)
    var mergedMangaAdapter: EditMergedMangaAdapter? by mutableStateOf(null)
    var mergedMangaHeaderAdapter: EditMergedSettingsHeaderAdapter? by mutableStateOf(null)

    fun onViewCreated(
        context: Context,
        binding: EditMergedSettingsDialogBinding,
        mergedManga: List<Manga>,
        mergedReferences: List<MergedMangaReference>,
    ) {
        if (mergedReferences.isEmpty() || mergedReferences.size == 1) {
            context.toast(SYMR.strings.merged_references_invalid)
            onDismissRequest()
        }
        mergedMangas += mergedReferences.filter {
            it.mangaSourceId != MERGED_SOURCE_ID
        }.map { reference -> mergedManga.firstOrNull { it.id == reference.mangaId } to reference }
        mergeReference = mergedReferences.firstOrNull { it.mangaSourceId == MERGED_SOURCE_ID }

        val isPriorityOrder =
            mergeReference?.let { it.chapterSortMode == MergedMangaReference.CHAPTER_SORT_PRIORITY } ?: false

        mergedMangaAdapter = EditMergedMangaAdapter(this, isPriorityOrder)
        mergedMangaHeaderAdapter = EditMergedSettingsHeaderAdapter(this, mergedMangaAdapter!!)

        binding.recycler.adapter = ConcatAdapter(mergedMangaHeaderAdapter, mergedMangaAdapter)
        binding.recycler.layoutManager = LinearLayoutManager(context)

        mergedMangaAdapter?.isHandleDragEnabled = isPriorityOrder

        mergedMangaAdapter?.updateDataSet(
            mergedMangas.map {
                it.toModel()
            }.sortedBy { it.mergedMangaReference.chapterPriority },
        )
    }

    override fun onItemReleased(position: Int) {
        val mergedMangaAdapter = mergedMangaAdapter ?: return
        mergedMangas = mergedMangas.map { (manga, reference) ->
            manga to reference.copy(
                chapterPriority = mergedMangaAdapter.currentItems.indexOfFirst {
                    reference.id == it.mergedMangaReference.id
                },
            )
        }
    }

    override fun onDeleteClick(position: Int) {
        val mergedMangaAdapter = mergedMangaAdapter ?: return
        val mergeMangaReference = mergedMangaAdapter.currentItems.getOrNull(position)?.mergedMangaReference ?: return

        MaterialAlertDialogBuilder(context)
            .setTitle(SYMR.strings.delete_merged_entry.getString(context))
            .setMessage(SYMR.strings.delete_merged_entry_desc.getString(context))
            .setPositiveButton(MR.strings.action_ok.getString(context)) { _, _ ->
                onDeleteClick(mergeMangaReference)
                onDismissRequest()
            }
            .setNegativeButton(MR.strings.action_cancel.getString(context), null)
            .show()
    }

    override fun onToggleChapterUpdatesClicked(position: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle(SYMR.strings.chapter_updates_merged_entry.getString(context))
            .setMessage(SYMR.strings.chapter_updates_merged_entry_desc.getString(context))
            .setPositiveButton(MR.strings.action_ok.getString(context)) { _, _ ->
                toggleChapterUpdates(position)
            }
            .setNegativeButton(MR.strings.action_cancel.getString(context), null)
            .show()
    }

    private fun toggleChapterUpdates(position: Int) {
        val adapterReference = mergedMangaAdapter?.currentItems?.getOrNull(position)?.mergedMangaReference
            ?: return
        mergedMangas = mergedMangas.map { pair ->
            val (manga, reference) = pair
            if (reference.id != adapterReference.id) return@map pair

            mergedMangaAdapter?.allBoundViewHolders?.firstOrNull {
                it is EditMergedMangaHolder && it.reference?.id == reference.id
            }?.let {
                if (it is EditMergedMangaHolder) {
                    it.updateChapterUpdatesIcon(!reference.getChapterUpdates)
                }
            } ?: context.toast(SYMR.strings.merged_chapter_updates_error)

            manga to reference.copy(getChapterUpdates = !reference.getChapterUpdates)
        }
    }

    override fun onToggleDownloadsClicked(position: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle(SYMR.strings.download_merged_entry.getString(context))
            .setMessage(SYMR.strings.download_merged_entry_desc.getString(context))
            .setPositiveButton(MR.strings.action_ok.getString(context)) { _, _ ->
                toggleChapterDownloads(position)
            }
            .setNegativeButton(MR.strings.action_cancel.getString(context), null)
            .show()
    }

    private fun toggleChapterDownloads(position: Int) {
        val adapterReference = mergedMangaAdapter?.currentItems?.getOrNull(position)?.mergedMangaReference
            ?: return
        mergedMangas = mergedMangas.map { pair ->
            val (manga, reference) = pair
            if (reference.id != adapterReference.id) return@map pair

            mergedMangaAdapter?.allBoundViewHolders?.firstOrNull {
                it is EditMergedMangaHolder && it.reference?.id == reference.id
            }?.let {
                if (it is EditMergedMangaHolder) {
                    it.updateDownloadChaptersIcon(!reference.downloadChapters)
                }
            } ?: context.toast(SYMR.strings.merged_toggle_download_chapters_error)

            manga to reference.copy(downloadChapters = !reference.downloadChapters)
        }
    }

    fun onPositiveButtonClick() {
        onPositiveClick(listOfNotNull(mergeReference) + mergedMangas.map { it.second })
        onDismissRequest()
    }
}

internal fun Pair<Manga?, MergedMangaReference>.toModel(): EditMergedMangaItem = EditMergedMangaItem(first, second)
