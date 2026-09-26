package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.databinding.ReaderErrorBinding
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import okio.BufferedSource
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.i18n.MR

internal fun WebtoonPageHolder.process(imageSource: BufferedSource): BufferedSource {
    if (viewer.config.dualPageRotateToFit) {
        return rotateDualPage(imageSource)
    }

    if (viewer.config.dualPageSplit) {
        val isDoublePage = ImageUtil.isWideImage(imageSource)
        if (isDoublePage) {
            val upperSide = if (viewer.config.dualPageInvert) ImageUtil.Side.LEFT else ImageUtil.Side.RIGHT
            return ImageUtil.splitAndMerge(imageSource, upperSide)
        }
    }

    return imageSource
}

internal fun WebtoonPageHolder.rotateDualPage(imageSource: BufferedSource): BufferedSource {
    val isDoublePage = ImageUtil.isWideImage(imageSource)
    return if (isDoublePage) {
        val rotation = if (viewer.config.dualPageRotateToFitInvert) -QUARTER_TURN_DEGREES else QUARTER_TURN_DEGREES
        ImageUtil.rotateImage(imageSource, rotation)
    } else {
        imageSource
    }
}

// Called when the page has an error.
internal fun WebtoonPageHolder.setError(error: Throwable?) {
    progressContainer.isVisible = false
    initErrorLayout(error)
}

// Creates a new progress bar.
internal fun WebtoonPageHolder.createProgressIndicator(): ReaderProgressIndicator {
    val progress = ReaderProgressIndicator(context).apply {
        updateLayoutParams<FrameLayout.LayoutParams> {
            updateMargins(top = parentHeight / 4)
        }
    }
    progressContainer.addView(progress)
    return progress
}

// Initializes a button to retry pages. Errors only come from a bound page, so [page] is set.
internal fun WebtoonPageHolder.initErrorLayout(error: Throwable?): ReaderErrorBinding {
    val boundPage = page!!
    val layout = errorLayout ?: ReaderErrorBinding.inflate(LayoutInflater.from(context), frame, true).also {
        errorLayout = it
        it.root.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, (parentHeight * ERROR_LAYOUT_HEIGHT).toInt())
        it.actionRetry.setOnClickListener {
            val current = page!!
            current.chapter.pageLoader?.retryPage(current)
        }
    }

    val imageUrl = boundPage.imageUrl
    layout.actionOpenInWebView.isVisible = imageUrl != null
    if (imageUrl != null && imageUrl.startsWith("http", true)) {
        layout.actionOpenInWebView.setOnClickListener {
            val sourceId = viewer.activity.viewModel.manga?.source

            val intent = WebViewActivity.newIntent(context, imageUrl, sourceId)
            context.startActivity(intent)
        }
    }

    layout.errorMessage.text = with(context) { error?.formattedMessage }
        ?: context.stringResource(MR.strings.decode_image_error)

    return layout
}

// Removes the decode error layout from the holder, if found.
internal fun WebtoonPageHolder.removeErrorLayout() {
    errorLayout?.let {
        frame.removeView(it.root)
        errorLayout = null
    }
}
