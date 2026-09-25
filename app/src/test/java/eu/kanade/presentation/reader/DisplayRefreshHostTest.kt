package eu.kanade.presentation.reader

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.kotest.matchers.shouldBe
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
internal class DisplayRefreshHostTest {
    @get:Rule
    val compose = createComposeRule()

    private val preferences = ReaderPreferences(FlowPreferenceStore())

    @Before
    fun setUp() {
        startKoin { modules(module { single { preferences } }) }
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun flashesEveryIntervalPages() {
        val host = DisplayRefreshHost()
        host.setInterval(2)
        host.flash()
        host.currentDisplayRefresh shouldBe true
        host.currentDisplayRefresh = false
        host.flash()
        host.currentDisplayRefresh shouldBe false
        host.flash()
        host.currentDisplayRefresh shouldBe true
    }

    private fun flashWith(color: ReaderPreferences.FlashColor) {
        preferences.flashColor.set(color)
        preferences.flashDurationMillis.set(200)
        val host = DisplayRefreshHost()
        compose.mainClock.autoAdvance = false
        compose.setContent { DisplayRefreshHost(hostState = host, modifier = Modifier) }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { host.flash() }
        compose.mainClock.advanceTimeBy(150L)
        host.currentDisplayRefresh shouldBe true
        compose.mainClock.advanceTimeBy(500L)
        host.currentDisplayRefresh shouldBe false
    }

    @Test
    fun blackFlash() = flashWith(ReaderPreferences.FlashColor.BLACK)

    @Test
    fun whiteFlash() = flashWith(ReaderPreferences.FlashColor.WHITE)

    @Test
    fun whiteThenBlackFlash() = flashWith(ReaderPreferences.FlashColor.WHITE_BLACK)
}
