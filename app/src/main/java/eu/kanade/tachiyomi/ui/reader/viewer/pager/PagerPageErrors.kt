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
    if (errorLayout == null) {
        errorLayout = ReaderErrorBinding.inflate(LayoutInflater.from(context), this, true)
        errorLayout?.actionRetry?.viewer = viewer
        errorLayout?.actionRetry?.setOnClickListener {
            page.chapter.pageLoader?.retryPage(page)
        }
    }

    val imageUrl = page.imageUrl
    errorLayout?.actionOpenInWebView?.isVisible = imageUrl != null
    if (imageUrl != null && imageUrl.startsWith("http", true)) {
        errorLayout?.actionOpenInWebView?.viewer = viewer
        errorLayout?.actionOpenInWebView?.setOnClickListener {
            val sourceId = viewer.activity.viewModel.manga?.source

            val intent = WebViewActivity.newIntent(context, imageUrl, sourceId)
            context.startActivity(intent)
        }
    }

    errorLayout?.errorMessage?.text = with(context) { error?.formattedMessage }
        ?: context.stringResource(MR.strings.decode_image_error)

    errorLayout?.root?.isVisible = true
    return errorLayout!!
}

// Removes the decode error layout from the holder, if found.
internal fun PagerPageHolder.removeErrorLayout() {
    errorLayout?.root?.isVisible = false
    errorLayout = null
}
