package eu.kanade.tachiyomi.ui.manga.merged

import eu.davidea.flexibleadapter.FlexibleAdapter

/**
 * Adapter storing a list of merged manga.
 *
 * @param listener receives the list edits.
 * @property isPriorityOrder if deduplication mode is based on priority
 */
internal class EditMergedMangaAdapter(listener: EditMergedSettingsState, var isPriorityOrder: Boolean) :
    FlexibleAdapter<EditMergedMangaItem>(null, listener, true),
    EditMergedSettingsHeaderAdapter.SortingListener {

    /**
     * Listener called when an item of the list is released.
     */
    val editMergedMangaItemListener: EditMergedMangaItemListener = listener

    interface EditMergedMangaItemListener {
        fun onItemReleased(position: Int)
        fun onDeleteClick(position: Int)
        fun onToggleChapterUpdatesClicked(position: Int)
        fun onToggleDownloadsClicked(position: Int)
    }

    override fun onSetPrioritySort(isPriorityOrder: Boolean) {
        isHandleDragEnabled = isPriorityOrder
        this.isPriorityOrder = isPriorityOrder
        allBoundViewHolders.filterIsInstance<EditMergedMangaHolder>().forEach { editMergedMangaHolder ->
            editMergedMangaHolder.setHandelAlpha(isPriorityOrder)
        }
    }
}
