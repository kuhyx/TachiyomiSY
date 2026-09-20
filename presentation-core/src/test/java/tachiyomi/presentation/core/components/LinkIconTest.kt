package tachiyomi.presentation.core.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Composable
private fun LinkIconHost(url: String) {
    LinkIcon(label = "Website", icon = Icons.Default.Share, url = url)
}

@RunWith(RobolectricTestRunner::class)
internal class LinkIconTest {
    @get:Rule
    val compose = createComposeRule()

    private val opened = mutableListOf<String>()

    private val uriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            opened += uri
        }
    }

    @Test
    fun clickOpensTheUrl() {
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                    LinkIcon(label = "Website", icon = Icons.Default.Share, url = "https://example.org")
                }
            }
        }
        compose.onNodeWithContentDescription("Website").performClick()
        compose.runOnIdle { opened shouldBe listOf("https://example.org") }
    }

    @Test
    fun acceptsAModifier() {
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                    LinkIcon(
                        label = "Website",
                        icon = Icons.Default.Share,
                        url = "https://example.org",
                        modifier = Modifier.testTag("link"),
                    )
                }
            }
        }
        compose.onNodeWithTag("link").assertExists()
        compose.runOnIdle { opened shouldBe emptyList() }
    }

    @Test
    fun changedUrlRebuildsTheClick() {
        var url by mutableStateOf("https://a.example")
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalUriHandler provides uriHandler) { LinkIconHost(url = url) }
            }
        }
        compose.waitForIdle()
        url = "https://b.example"
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Website").performClick()
        compose.runOnIdle { opened shouldBe listOf("https://b.example") }
    }
}
