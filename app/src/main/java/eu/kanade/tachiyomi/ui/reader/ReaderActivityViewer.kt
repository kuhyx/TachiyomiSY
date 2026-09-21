package eu.kanade.tachiyomi.ui.reader

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.graphics.Insets
import androidx.core.transition.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.transition.platform.MaterialContainerTransform
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import eu.kanade.tachiyomi.ui.reader.setting.useAutoWebtoon
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerViewer
import eu.kanade.tachiyomi.util.system.toast
import exh.util.defaultReaderType
import exh.util.mangaType
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.api.get

// Called from the presenter when a manga is ready. Used to instantiate the appropriate viewer.
internal fun ReaderActivity.updateViewer() {
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
internal fun ReaderActivity.updateViewerInset(fullscreen: Boolean, drawUnderCutout: Boolean) {
    val view = binding.viewerContainer

    applyInsetsPadding(view, ViewCompat.getRootWindowInsets(view), fullscreen, drawUnderCutout)
    ViewCompat.setOnApplyWindowInsetsListener(view) { view, windowInsets ->
        applyInsetsPadding(view, windowInsets, fullscreen, drawUnderCutout)
        windowInsets
    }
}

internal fun ReaderActivity.applyInsetsPadding(
    view: View,
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

    view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
}
