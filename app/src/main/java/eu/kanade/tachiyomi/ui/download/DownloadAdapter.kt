package eu.kanade.tachiyomi.ui.download

import android.view.MenuItem
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem

/**
 * Adapter storing a list of downloads.
 *
 * @property downloadItemListener Listener called when an item of the list is released.
 */
internal class DownloadAdapter(
    val downloadItemListener: DownloadItemListener,
) : FlexibleAdapter<AbstractFlexibleItem<*>>(
    null,
    downloadItemListener,
    true,
) {

    // Don't let sub-items changing group
    override fun shouldMove(fromPosition: Int, toPosition: Int): Boolean =
        getHeaderOf(getItem(fromPosition)) == getHeaderOf(getItem(toPosition))

    interface DownloadItemListener {
        fun onItemReleased(position: Int)
        fun onMenuItemClick(position: Int, menuItem: MenuItem)
    }
}
