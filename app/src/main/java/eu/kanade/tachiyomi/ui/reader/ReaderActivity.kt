package eu.kanade.tachiyomi.ui.reader

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.transition.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.transition.platform.MaterialContainerTransform
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.presentation.reader.DisplayRefreshHost
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderActivityBinding
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import eu.kanade.tachiyomi.ui.reader.setting.useAutoWebtoon
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.util.system.toast
import exh.util.defaultReaderType
import exh.util.mangaType
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// Reader theme preference values, the inverted-colours matrix offset and the brightness scale.

internal class ReaderActivity : BaseActivity() {

    internal val readerPreferences = Injekt.get<ReaderPreferences>()
    internal val preferences = Injekt.get<BasePreferences>()

    val binding: ReaderActivityBinding by lazy { ReaderActivityBinding.inflate(layoutInflater) }

    val viewModel by viewModels<ReaderViewModel>()
    internal var assistUrl: String? = null

    // SY -->
    internal val sourceManager = Injekt.get<SourceManager>()
    // SY <--

    // Configuration at reader level, like background color or forced orientation.
    private var config: ReaderConfig? = null

    internal var menuToggleToast: Toast? = null
    internal var readingModeToast: Toast? = null
    internal val displayRefreshHost = DisplayRefreshHost()

    internal val windowInsetsController by lazy { WindowInsetsControllerCompat(window, window.decorView) }

    internal var loadingIndicator: ReaderProgressIndicator? = null

    internal var isScrollingThroughPages = false

    /**
     * Called when the activity is created. Initializes the presenter and configuration.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        registerSecureActivity(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.shared_axis_x_push_enter,
                R.anim.shared_axis_x_push_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_push_enter, R.anim.shared_axis_x_push_exit)
        }

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        setComposeOverlay(binding)

        if (viewModel.needsInit() && !initViewModelFromIntent()) {
            finish()
            return
        }

        config = ReaderConfig(this)
        setMenuVisibility(viewModel.state.value.menuVisible)
        enableExhAutoScroll()

        observeViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.state.value.viewer?.destroy()
        config = null
        menuToggleToast?.cancel()
        readingModeToast?.cancel()
    }

    override fun onPause() {
        lifecycleScope.launchNonCancellable {
            viewModel.progress.updateHistory()
        }
        super.onPause()
    }

    /**
     * Set menu visibility again on activity resume to apply immersive mode again if needed.
     * Helps with rotations.
     */
    override fun onResume() {
        super.onResume()
        viewModel.progress.restartReadTimer()
        setMenuVisibility(viewModel.state.value.menuVisible)
    }

    /**
     * Called when the window focus changes. It sets the menu visibility to the last known state
     * to apply immersive mode again if needed.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setMenuVisibility(viewModel.state.value.menuVisible)
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        assistUrl?.let { outContent.webUri = it.toUri() }
    }

    /**
     * Called when the user clicks the back key or the button on the toolbar. The call is
     * delegated to the presenter.
     */
    override fun finish() {
        viewModel.onActivityFinish()
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.shared_axis_x_pop_enter,
                R.anim.shared_axis_x_pop_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.shared_axis_x_pop_enter, R.anim.shared_axis_x_pop_exit)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_N -> {
            loadNextChapter()
            true
        }
        KeyEvent.KEYCODE_P -> {
            loadPreviousChapter()
            true
        }
        else -> {
            super.onKeyUp(keyCode, event)
        }
    }

    /**
     * Dispatches a key event. If the viewer doesn't handle it, call the default implementation.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handled = viewModel.state.value.viewer?.handleKeyEvent(event) ?: false
        return handled || super.dispatchKeyEvent(event)
    }

    /**
     * Dispatches a generic motion event. If the viewer doesn't handle it, call the default
     * implementation.
     */
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val handled = viewModel.state.value.viewer?.handleGenericMotionEvent(event) ?: false
        return handled || super.dispatchGenericMotionEvent(event)
    }

// EXH <--

    // Called from the presenter when a manga is ready. Used to instantiate the appropriate viewer.
    internal fun updateViewer() {
        val prevViewer = viewModel.state.value.viewer
        val newViewer = ReadingMode.toViewer(viewModel.viewerSettings.getMangaReadingMode(), this)

        if (window.sharedElementEnterTransition is MaterialContainerTransform) {
            // Wait until transition is complete to avoid crash on API 26
            window.sharedElementEnterTransition.doOnEnd {
                setOrientation(viewModel.viewerSettings.getMangaOrientation())
            }
        } else {
            setOrientation(viewModel.viewerSettings.getMangaOrientation())
        }

        // Destroy previous viewer if there was one
        if (prevViewer != null) {
            prevViewer.destroy()
            binding.viewerContainer.removeAllViews()
        }
        viewModel.onViewerLoaded(newViewer)
        updateViewerInset(readerPreferences.fullscreen.get(), readerPreferences.drawUnderCutout.get())
        binding.viewerContainer.addView(newViewer.getView())

        // SY -->
        if (newViewer is PagerViewer) {
            if (readerPreferences.pageLayout.get() == PagerConfig.PageLayout.AUTOMATIC) {
                setDoublePageMode(newViewer)
            }
            viewModel.state.value.lastShiftDoubleState?.let { newViewer.config.shiftDoublePage = it }
        }

        val manga = viewModel.state.value.manga
        val defaultReaderType = manga?.defaultReaderType(
            manga.mangaType(sourceName = sourceManager.get(manga.source)?.name),
        )
        val usesDefaultReadingMode =
            (manga?.readingMode?.toInt() ?: ReadingMode.DEFAULT.flagValue) == ReadingMode.DEFAULT.flagValue
        val autoWebtoon = readerPreferences.useAutoWebtoon.get() && defaultReaderType == ReadingMode.WEBTOON.flagValue
        if (autoWebtoon && usesDefaultReadingMode) {
            readingModeToast?.cancel()
            readingModeToast = toast(SYMR.strings.eh_auto_webtoon_snack)
        } else if (readerPreferences.showReadingMode.get()) {
            // SY <--
            showReadingModeToast(viewModel.viewerSettings.getMangaReadingMode())
        }

        loadingIndicator = ReaderProgressIndicator(this)
        binding.readerContainer.addView(loadingIndicator)

        startPostponedEnterTransition()
    }

    // Updates viewer inset depending on fullscreen reader preferences.
    internal fun updateViewerInset(fullscreen: Boolean, drawUnderCutout: Boolean) {
        val view = binding.viewerContainer

        view.applyInsetsPadding(ViewCompat.getRootWindowInsets(view), fullscreen, drawUnderCutout)
        ViewCompat.setOnApplyWindowInsetsListener(view) { view, windowInsets ->
            view.applyInsetsPadding(windowInsets, fullscreen, drawUnderCutout)
            windowInsets
        }
    }

    private fun View.applyInsetsPadding(
        windowInsets: WindowInsetsCompat?,
        fullscreen: Boolean,
        drawUnderCutout: Boolean,
    ) {
        val insets = when {
            !fullscreen -> windowInsets?.getInsets(WindowInsetsCompat.Type.systemBars())
            !drawUnderCutout -> windowInsets?.getInsets(WindowInsetsCompat.Type.displayCutout())
            else -> null
        }
            ?: Insets.NONE

        setPadding(insets.left, insets.top, insets.right, insets.bottom)
    }

    /**
     * Class that handles the user preferences of the reader.
     */
    companion object {

        const val SHIFT_DOUBLE_PAGES = "shiftingDoublePages"
        const val SHIFTED_PAGE_INDEX = "shiftedPageIndex"
        const val SHIFTED_CHAP_INDEX = "shiftedChapterIndex"

        fun newIntent(
            context: Context,
            mangaId: Long?,
            chapterId: Long?,
            /* SY --> */
            page: Int? = null, /* SY <-- */
        ): Intent {
            return Intent(context, ReaderActivity::class.java).apply {
                putExtra("manga", mangaId)
                putExtra("chapter", chapterId)
                // SY -->
                putExtra("page", page)
                // SY <--
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }
}
