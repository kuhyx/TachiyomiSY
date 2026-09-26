package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDex
import exh.md.utils.getEnabledMangaDexs
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class MangadexLoginTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)
    private val mdList = mockk<MdList> { every { id } returns 60L }
    private val mdex = mockk<MangaDex>().also {
        every { it.name } returns "MangaDex"
        every { it.id } returns 7L
        every { it.mdList } returns mdList
    }

    @Before
    fun setUp() {
        mockkStatic("exh.md.utils.MdSourcesKt")
        every { MdUtil.getEnabledMangaDex(any(), any()) } returns mdex
        every { MdUtil.getEnabledMangaDexs(any(), any()) } returns listOf(mdex)
        koin.start(module { single { mockk<SourceManager>() } })
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun logOut(): String {
        koin.track.trackToken(mdList).set("token")
        harness.show(SettingsMangadexScreen)
        compose.onNodeWithText("MangaDex Login").performClick()
        compose.waitForIdle()
        compose.onNode(hasText("Log out") and hasClickAction()).performClick()
        compose.awaitMain(timeoutMillis = 10_000) { ShadowToast.shownToastCount() == 1 }
        return ShadowToast.getTextOfLatestToast().toString()
    }

    @Test
    fun loggedOutOpensBrowser() {
        harness.show(SettingsMangadexScreen)
        compose.onNodeWithText("MangaDex Login").performClick()
        compose.waitForIdle()
        harness.count("Log out") shouldBe 0
    }

    @Test
    fun logoutSucceeds() {
        coEvery { mdex.logout() } returns true
        logOut() shouldBe "You are now logged out"
    }

    @Test
    fun logoutRejected() {
        coEvery { mdex.logout() } returns false
        logOut() shouldBe "Unknown error"
    }

    @Test
    fun logoutThrows() {
        coEvery { mdex.logout() } throws IllegalStateException("network")
        logOut() shouldBe "Unknown error"
    }

    @Test
    fun logoutCancelled() {
        koin.track.trackToken(mdList).set("token")
        harness.show(SettingsMangadexScreen)
        compose.onNodeWithText("MangaDex Login").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        harness.count("Log out") shouldBe 0
    }
}
