package eu.kanade.tachiyomi.ui.updates

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.ui.base.TabHost
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Inside the main activity, loaded updates tell the splash screen the app is ready. */
@RunWith(RobolectricTestRunner::class)
internal class UpdatesTabReadyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val harness = UpdatesHarness()

    @Before
    fun setUp() {
        startKoin { modules(harness.koinModules() + module { single { UiPreferences(harness.store) } }) }
        harness.updates.value = listOf(update(1, dateFetch = System.currentTimeMillis()))
    }

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
        stopKoin()
        HomeScreen.showBottomNavEvent.tryReceive()
    }

    // Only constructed: the tab reads it as its context, nothing runs its lifecycle.
    private fun showInMain(): MainActivity {
        val main = Robolectric.buildActivity(MainActivity::class.java).get()
        compose.setContent {
            CompositionLocalProvider(LocalContext provides main) { TabHost(UpdatesTab) }
        }
        return main
    }

    @Test
    fun stillLoadingIsNotReady() {
        every { harness.getUpdates.subscribe(any(), any(), any(), any(), any()) } returns MutableSharedFlow()
        val main = showInMain()
        compose.waitUntil(WAIT) { compose.onAllNodes(hasText("tab:Updates")).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        main.ready shouldBe false
    }

    @Test
    fun loadedUpdatesMarkReady() {
        val main = showInMain()
        compose.waitUntil(WAIT) { compose.onAllNodes(hasText("C1")).fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(WAIT) { main.ready }
    }
}

private const val WAIT = 5_000L
