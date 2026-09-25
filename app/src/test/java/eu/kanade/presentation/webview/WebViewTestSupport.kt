package eu.kanade.presentation.webview

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule

/** Every [WebView] under [root], depth first. */
internal fun webViewsUnder(root: View): List<WebView> = when (root) {
    is WebView -> listOf(root)
    is ViewGroup -> (0 until root.childCount).flatMap { webViewsUnder(root.getChildAt(it)) }
    else -> emptyList()
}

/** The WebViews the composition currently hosts. */
internal fun ComposeContentTestRule.webViews(): List<WebView> {
    val root = onNode(isRoot()).fetchSemanticsNode().root as ViewRootForTest
    return webViewsUnder(root.view.rootView)
}
