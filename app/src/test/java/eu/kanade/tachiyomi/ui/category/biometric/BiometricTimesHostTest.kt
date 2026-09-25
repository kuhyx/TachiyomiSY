package eu.kanade.tachiyomi.ui.category.biometric

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.ui.base.ActivityKoin
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.silentStore
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

/** The screen outside a fragment host (no picker) and before its first load. */
@RunWith(RobolectricTestRunner::class)
internal class BiometricTimesHostTest {
    @get:Rule
    val compose = createComposeRule()

    private val activityKoin = ActivityKoin()

    @After
    fun tearDown() = stopKoin()

    @Test
    fun noFragmentHostShowsNoPicker() {
        startKoin { modules(activityKoin.module()) }
        compose.setContent { ScreenHost(BiometricTimesScreen()) }
        compose.waitUntil(WAIT) {
            compose.onAllNodes(hasText("Add"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        activityKoin.security.authenticatorTimeRanges.get() shouldBe emptySet()
    }

    @Test
    fun loadingShowsNothingElse() {
        val silent = SecurityPreferences(silentStore())
        startKoin { modules(activityKoin.module(), module { single { silent } }) }
        compose.setContent { ScreenHost(BiometricTimesScreen()) }
        compose.onAllNodes(hasText("Add")).fetchSemanticsNodes().size shouldBe 0
    }
}

private const val WAIT = 5_000L
