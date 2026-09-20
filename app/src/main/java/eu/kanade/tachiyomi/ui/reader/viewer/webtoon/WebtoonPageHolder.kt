package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.res.Resources
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.databinding.ReaderErrorBinding
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.hideMenu
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.recycle
import eu.kanade.tachiyomi.ui.reader.viewer.setImage
import eu.kanade.tachiyomi.util.system.dpToPx
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

// Holder of the webtoon reader for a single page of a chapter.
// @param frame the root view for this holder.
// @param viewer the webtoon viewer.
// @constructor creates a new webtoon holder.
private const val PERCENT = 100f
internal const val QUARTER_TURN_DEGREES = 90f
internal const val ERROR_LAYOUT_HEIGHT = 0.8

internal class WebtoonPageHolder(
    internal val frame: ReaderPageImageView,
    viewer: WebtoonViewer,
) : WebtoonBaseHolder(frame, viewer) {

    // Progress bar container. Needed to keep a minimum height size of the holder, otherwise the
    // adapter would create more views to fill the screen, which is not wanted.
    internal val progressContainer: ViewGroup = FrameLayout(context).also {
        frame.addView(it, MATCH_PARENT, parentHeight)
    }

    // Loading progress bar to indicate the current progress.
    private val progressIndicator = createProgressIndicator()

    // Error layout to show when the image fails to load.
    internal var errorLayout: ReaderErrorBinding? = null

    // Getter to retrieve the height of the recycler view.
    internal val parentHeight
        get() = viewer.recycler.height

    // Page of a chapter.
    internal var page: ReaderPage? = null

    private val scope = MainScope()

    // Job for loading the page.
    private var loadJob: Job? = null

    init {
        refreshLayoutParams()

        frame.onImageLoaded = { onImageDecoded() }
        frame.onImageLoadError = { error -> setError(error) }
        frame.onScaleChanged = { viewer.activity.hideMenu() }
    }

    /**
     * Binds the given [page] with this view holder, subscribing to its state.
     */
    fun bind(page: ReaderPage) {
        this.page = page
        loadJob?.cancel()
        loadJob = scope.launch { loadPageAndProcessStatus() }
        refreshLayoutParams()
    }

    private fun refreshLayoutParams() {
        frame.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            if (!viewer.isContinuous) {
                bottomMargin = 15.dpToPx
            }

            val margin = Resources.getSystem().displayMetrics.widthPixels * (viewer.config.sidePadding / PERCENT)
            marginEnd = margin.toInt()
            marginStart = margin.toInt()
        }
    }

    /**
     * Called when the view is recycled and added to the view pool.
     */
    override fun recycle() {
        loadJob?.cancel()
        loadJob = null

        removeErrorLayout()
        frame.recycle()
        progressIndicator.setProgress(0)
        progressContainer.isVisible = true
    }

    // Loads the page and processes changes to the page's status.
    // Returns immediately if there is no page or the page has no PageLoader.
    // Otherwise, this function does not return. It will continue to process status changes until
    // the Job is cancelled.
    private suspend fun loadPageAndProcessStatus() {
        val page = page ?: return
        val loader = page.chapter.pageLoader ?: return
        supervisorScope {
            launchIO {
                loader.loadPage(page)
            }
            page.statusFlow.collectLatest { state ->
                when (state) {
                    Page.State.Queue -> {
                        setQueued()
                    }
                    Page.State.LoadPage -> {
                        setLoading()
                    }
                    Page.State.DownloadImage -> {
                        setDownloading()
                        page.progressFlow.collectLatest { value ->
                            progressIndicator.setProgress(value)
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

    // Called when the page is queued.
    private fun setQueued() {
        progressContainer.isVisible = true
        progressIndicator.show()
        removeErrorLayout()
    }

    // Called when the page is loading.
    private fun setLoading() {
        progressContainer.isVisible = true
        progressIndicator.show()
        removeErrorLayout()
    }

    // Called when the page is downloading.
    private fun setDownloading() {
        progressContainer.isVisible = true
        progressIndicator.show()
        removeErrorLayout()
    }

    // Called when the page is ready.
    private suspend fun setImage() {
        progressIndicator.setProgress(0)

        val streamFn = page?.stream ?: return

        try {
            val (source, isAnimated) = withIOContext {
                val source = streamFn().use { process(Buffer().readFrom(it)) }
                val isAnimated = ImageUtil.isAnimatedAndSupported(source)
                Pair(source, isAnimated)
            }
            withUIContext {
                frame.setImage(
                    source,
                    isAnimated,
                    ReaderPageImageView.Config(
                        zoomDuration = viewer.config.doubleTapAnimDuration,
                        minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_FIT_WIDTH,
                        cropBorders =
                        (viewer.config.imageCropBorders && viewer.isContinuous) ||
                            (viewer.config.continuousCropBorders && !viewer.isContinuous),
                    ),
                )
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

    // Called when the image is decoded and going to be displayed.
    private fun onImageDecoded() {
        progressContainer.isVisible = false
        removeErrorLayout()
    }
}
