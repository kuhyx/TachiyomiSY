package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class WhatsNewDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private var dismissed = 0

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(module { single { XML.v1 { policy { ignoreUnknownChildren() } } } })
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun show() {
        compose.setContent { MaterialTheme { WhatsNewDialog { dismissed++ } } }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Version", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun releaseChangelogShown() {
        show()
        compose.onAllNodesWithText("Version", substring = true).fetchSemanticsNodes().size shouldBeGreaterThan 0
        compose.onNodeWithText("Cancel").performClick()
        dismissed shouldBe 1
    }

    @Test
    fun previewChangelogShown() {
        mockkStatic("eu.kanade.tachiyomi.util.system.BuildConfigKt")
        every { isPreviewBuildType } returns true
        show()
        compose.onAllNodesWithText("Version", substring = true).fetchSemanticsNodes().size shouldBeGreaterThan 0
    }
}
