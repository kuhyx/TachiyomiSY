package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.WebtoonLayoutManager
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.onPageSelected
import eu.kanade.tachiyomi.ui.reader.requestPreloadChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.isVolumeKey
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.math.max
import kotlin.math.min

// Implementation of a [Viewer] to display pages with a [RecyclerView].
// A side tap scrolls three quarters of the screen; the next chapter preloads five pages before the end.
private const val SCROLL_DISTANCE_NUMERATOR = 3
private const val SCROLL_DISTANCE_DENOMINATOR = 4
private const val PRELOAD_PAGES_BEFORE_END = 5
private const val REFRESH_RADIUS = 3

// Double the default cache size to reduce rebinds/recycles incurred by the extra layout space on
// scroll direction changes.

internal class WebtoonViewer(
    val activity: ReaderActivity,
    val isContinuous: Boolean = true,
    internal val tapByPage: Boolean = false,
) : Viewer {

    val downloadManager: DownloadManager by injectLazy()

    private val scope = MainScope()

    /**
     * Recycler view used by this viewer.
     */
    val recycler = WebtoonRecyclerView(activity)

    // Frame containing the recycler view.
    internal val frame = WebtoonFrame(activity)

    // Distance to scroll when the user taps on one side of the recycler view.
    internal val scrollDistance =
        activity.resources.displayMetrics.heightPixels * SCROLL_DISTANCE_NUMERATOR / SCROLL_DISTANCE_DENOMINATOR

    // Layout manager of the recycler view.
    internal val layoutManager = WebtoonLayoutManager(activity, scrollDistance)

    /**
     * Configuration used by this viewer, like allow taps, or crop image borders.
     */
    val config = WebtoonConfig(scope)

    // Adapter of the recycler view.
    internal val adapter = WebtoonAdapter(this)

    /**
     * Currently active item. It can be a chapter page or a chapter transition.
     */
    /* [EXH] private */
    var currentPage: Any? = null

    internal val threshold: Int =
        Injekt.get<ReaderPreferences>()
            .readerHideThreshold
            .get()
            .threshold

    init {
        setUpRecycler()
        setUpTapListeners()
        setUpConfigListeners()
        setUpFrame()
    }

    internal fun checkAllowPreload(page: ReaderPage?): Boolean {
        // Page is transition page - preload allowed
        page ?: return true

        // Initial opening - preload allowed
        currentPage ?: return true

        val nextItem = adapter.items.getOrNull(adapter.items.size - 1)
        val nextChapter = (nextItem as? ChapterTransition.Next)?.to ?: (nextItem as? ReaderPage)?.chapter

        // Allow preload for
        // 1. Going between pages of same chapter
        // 2. Next chapter page
        return when (page.chapter) {
            (currentPage as? ReaderPage)?.chapter -> true
            nextChapter -> true
            else -> false
        }
    }

    /**
     * Returns the view this viewer uses.
     */
    override fun getView(): View = frame

    /**
     * Destroys this viewer. Called when leaving the reader or swapping viewers.
     */
    override fun destroy() {
        super.destroy()
        scope.cancel()
    }

    // Called from the RecyclerView listener when a [page] is marked as active. It notifies the
    // activity of the change and requests the preload of the next chapter if this is the last page.
    internal fun onPageSelected(page: ReaderPage, allowPreload: Boolean) {
        val pages = page.chapter.pages ?: return
        logcat { "onPageSelected: ${page.number}/${pages.size}" }
        activity.onPageSelected(page)

        // Preload next chapter once we're within the last 5 pages of the current chapter
        val inPreloadRange = pages.size - page.number < PRELOAD_PAGES_BEFORE_END
        if (inPreloadRange && allowPreload && page.chapter == adapter.currentChapter) {
            logcat { "Request preload next chapter because we're at page ${page.number} of ${pages.size}" }
            val nextItem = adapter.items.getOrNull(adapter.items.size - 1)
            val transitionChapter = (nextItem as? ChapterTransition.Next)?.to ?: (nextItem as?ReaderPage)?.chapter
            if (transitionChapter != null) {
                logcat { "Requesting to preload chapter ${transitionChapter.chapter.chapter_number}" }
                activity.requestPreloadChapter(transitionChapter)
            }
        }
    }

    // Called from the RecyclerView listener when a [transition] is marked as active. It request the
    // preload of the destination chapter of the transition.
    internal fun onTransitionSelected(transition: ChapterTransition) {
        logcat { "onTransitionSelected: $transition" }
        val toChapter = transition.to
        if (toChapter != null) {
            logcat { "Request preload destination chapter because we're on the transition" }
            activity.requestPreloadChapter(toChapter)
        }
    }

    /**
     * Tells this viewer to set the given [chapters] as active.
     */
    override fun setChapters(chapters: ViewerChapters) {
        val forceTransition = config.alwaysShowChapterTransition || currentPage is ChapterTransition
        adapter.setChapters(chapters, forceTransition)

        if (recycler.isGone) {
            logcat { "Recycler first layout" }
            val pages = chapters.currChapter.pages ?: return
            moveToPage(pages[min(chapters.currChapter.requestedPage, pages.lastIndex)])
            recycler.isVisible = true
        }
    }

    /**
     * Tells this viewer to move to the given [page].
     */
    override fun moveToPage(page: ReaderPage) {
        val position = adapter.items.indexOf(page)
        if (position != -1) {
            layoutManager.scrollToPositionWithOffset(position, 0)
            if (layoutManager.findLastEndVisibleItemPosition() == -1) {
                onScrolled(pos = position)
            }
        } else {
            logcat { "Page $page not found in adapter" }
        }
    }

    /**
     * Called from the containing activity when a key [event] is received. It should return true
     * if the event was handled, false otherwise.
     */
    override fun handleKeyEvent(event: KeyEvent): Boolean {
        val action = keyAction(event.keyCode) ?: return false
        if (event.isVolumeKey() && (!config.volumeKeysEnabled || activity.viewModel.state.value.menuVisible)) {
            return false
        }
        // The key is claimed on the way down too; the scroll happens on release.
        if (event.action == KeyEvent.ACTION_UP) action()
        return true
    }

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    override fun handleGenericMotionEvent(event: MotionEvent): Boolean = false

    // Notifies adapter of changes around the current page to trigger a relayout in the recycler.
    // Used when an image configuration is changed.
    internal fun refreshAdapter() {
        val position = layoutManager.findLastEndVisibleItemPosition()
        adapter.refresh()
        adapter.notifyItemRangeChanged(
            max(0, position - REFRESH_RADIUS),
            min(position + REFRESH_RADIUS, adapter.itemCount - 1),
        )
    }
}
