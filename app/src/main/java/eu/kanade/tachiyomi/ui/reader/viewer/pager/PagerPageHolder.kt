package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import eu.kanade.tachiyomi.databinding.ReaderErrorBinding
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.hideMenu
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.ui.reader.viewer.setImage
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import okio.Buffer
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import java.io.InputStream

// View of the ViewPager that contains a page of a chapter.
// Progress-bar milestones while a page is prepared, and the double-page layout metrics.
private const val PROGRESS_DECODING = 95
internal const val PROGRESS_SPLITTING = 96
internal const val PROGRESS_MERGING = 97
private const val PROGRESS_DONE = 100
private const val STREAM_BUFFER_SIZE = 16
internal const val QUARTER_TURN_DEGREES = 90f
internal const val CENTER_MARGIN_PX = 96
internal const val HALF_CENTER_MARGIN_PX = 48
internal const val PAGE_SPLIT_DELAY_MS = 100L

@SuppressLint("ViewConstructor")
internal class PagerPageHolder(
    readerThemedContext: Context,
    val viewer: PagerViewer,
    val page: ReaderPage,
    internal var extraPage: ReaderPage? = null,
) : ReaderPageImageView(readerThemedContext), ViewPagerAdapter.PositionableView {

    /**
     * Item that identifies this view. Needed by the adapter to not recreate views.
     */
    override val item
        get() = page to extraPage

    // Loading progress bar to indicate the current progress.
    internal var progressIndicator: ReaderProgressIndicator? = null // = ReaderProgressIndicator(readerThemedContext)

    // Error layout to show when the image fails to load.
    internal var errorLayout: ReaderErrorBinding? = null

    internal val scope = MainScope()

    // Job for loading the page and processing changes to the page's status.
    private var loadJob: Job? = null

    // Job for loading the page.
    private var extraLoadJob: Job? = null

    init {
        loadJob = scope.launch { loadPageAndProcessStatus(1) }
        extraLoadJob = scope.launch { loadPageAndProcessStatus(2) }
    }

    /**
     * Called when this view is detached from the window. Unsubscribes any active subscription.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loadJob?.cancel()
        loadJob = null
        extraLoadJob?.cancel()
        extraLoadJob = null
    }

    // Shows the progress indicator, creating it on first use, and clears any error.
    private fun showProgress(): ReaderProgressIndicator {
        val indicator = progressIndicator ?: ReaderProgressIndicator(context).also {
            progressIndicator = it
            addView(it)
        }
        indicator.show()
        removeErrorLayout()
        return indicator
    }

    // Loads the page and processes changes to the page's status.
    // Returns immediately if the page has no PageLoader.
    // Otherwise, this function does not return. It will continue to process status changes until
    // the Job is cancelled.
    private suspend fun loadPageAndProcessStatus(pageIndex: Int) {
        // SY -->
        val page = if (pageIndex == 1) page else extraPage
        page ?: return
        // SY <--
        val loader = page.chapter.pageLoader ?: return
        supervisorScope {
            launchIO {
                loader.loadPage(page)
            }
            page.statusFlow.collectLatest { state ->
                when (state) {
                    Page.State.Queue, Page.State.LoadPage -> {
                        showProgress()
                    }
                    Page.State.DownloadImage -> {
                        val indicator = showProgress()
                        page.progressFlow.collectLatest { value ->
                            indicator.setProgress(value)
                        }
                    }
                    Page.State.Ready -> {
                        setImage()
                    }
                    is Page.State.Error -> {
                        setError(state.error)
                    }
                }
            }
        }
    }

    // Called when the page is ready.
    private suspend fun setImage() {
        if (extraPage == null) {
            progressIndicator?.setProgress(0)
        } else {
            progressIndicator?.setProgress(PROGRESS_DECODING)
        }

        val streamFn = page.stream ?: return
        val streamFn2 = extraPage?.stream

        try {
            val (source, isAnimated, background) = decode(streamFn, streamFn2)
            withUIContext {
                setImage(
                    source,
                    isAnimated,
                    Config(
                        zoomDuration = viewer.config.doubleTapAnimDuration,
                        minimumScaleType = viewer.config.imageScaleType,
                        cropBorders = viewer.config.imageCropBorders,
                        zoomStartPosition = viewer.config.imageZoomType,
                        landscapeZoom = viewer.config.landscapeZoom,
                    ),
                )
                if (!isAnimated) {
                    pageBackground = background
                }
                removeErrorLayout()
            }
        } catch (expected: Throwable) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
            withUIContext {
                setError(expected)
            }
        }
    }

    // The page image (split or merged with the extra page, per the settings), whether it animates, and the
    // backdrop chosen for it.
    private suspend fun decode(streamFn: () -> InputStream, streamFn2: (() -> InputStream)?) = withIOContext {
        streamFn().buffered(STREAM_BUFFER_SIZE).use { source ->
            // SY -->
            // streamFn2 is the extra page's stream, so it is null whenever there is no extra page.
            streamFn2?.let { it().buffered(STREAM_BUFFER_SIZE) }.use { source2 ->
                val itemSource = if (viewer.config.dualPageSplit) {
                    process(item.first, Buffer().readFrom(source))
                } else {
                    mergePages(Buffer().readFrom(source), source2?.let { Buffer().readFrom(it) })
                }
                // SY <--
                val isAnimated = ImageUtil.isAnimatedAndSupported(itemSource)
                val background = if (!isAnimated && viewer.config.automaticBackground) {
                    ImageUtil.chooseBackground(context, itemSource.peek())
                } else {
                    null
                }
                Triple(itemSource, isAnimated, background)
            }
        }
    }

    internal fun updateProgress(progress: Int) {
        scope.launch {
            if (progress == PROGRESS_DONE) {
                progressIndicator?.hide()
            } else {
                progressIndicator?.setProgress(progress)
            }
        }
    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        progressIndicator?.hide()
    }

    /**
     * Called when an image fails to decode.
     */
    override fun onImageLoadError(error: Throwable?) {
        super.onImageLoadError(error)
        setError(error)
    }

    /**
     * Called when an image is zoomed in/out.
     */
    override fun onScaleChanged(newScale: Float) {
        super.onScaleChanged(newScale)
        viewer.activity.hideMenu()
    }
}
