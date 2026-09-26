package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.LayoutInflater
import androidx.core.view.isVisible
import eu.kanade.presentation.util.formattedMessage
import eu.kanade.tachiyomi.databinding.ReaderErrorBinding
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

// Called when the page has an error.
internal fun PagerPageHolder.setError(error: Throwable?) {
    progressIndicator?.hide()
    showErrorLayout(error)
}

internal fun PagerPageHolder.showErrorLayout(error: Throwable?): ReaderErrorBinding {
    val layout = errorLayout ?: ReaderErrorBinding.inflate(LayoutInflater.from(context), this, true).also {
        errorLayout = it
        it.actionRetry.viewer = viewer
        it.actionRetry.setOnClickListener {
            page.chapter.pageLoader?.retryPage(page)
        }
    }

    val imageUrl = page.imageUrl
    layout.actionOpenInWebView.isVisible = imageUrl != null
    if (imageUrl != null && imageUrl.startsWith("http", true)) {
        layout.actionOpenInWebView.viewer = viewer
        layout.actionOpenInWebView.setOnClickListener {
            val sourceId = viewer.activity.viewModel.manga?.source

            val intent = WebViewActivity.newIntent(context, imageUrl, sourceId)
            context.startActivity(intent)
        }
    }

    layout.errorMessage.text = with(context) { error?.formattedMessage }
        ?: context.stringResource(MR.strings.decode_image_error)

    layout.root.isVisible = true
    return layout
}

// Removes the decode error layout from the holder, if found.
internal fun PagerPageHolder.removeErrorLayout() {
    errorLayout?.let { it.root.isVisible = false }
    errorLayout = null
}
