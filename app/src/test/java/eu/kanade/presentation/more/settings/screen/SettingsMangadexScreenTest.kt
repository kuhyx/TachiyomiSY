package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.startNow
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDex
import exh.md.utils.getEnabledMangaDexs
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class SettingsMangadexScreenTest {
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
        mockkStatic("exh.md.utils.MdSourcesKt", "eu.kanade.tachiyomi.data.library.LibraryUpdateSchedulingKt")
        every { MdUtil.getEnabledMangaDex(any(), any()) } returns mdex
        every { MdUtil.getEnabledMangaDexs(any(), any()) } returns listOf(mdex)
        every { LibraryUpdateJob.startNow(any(), any(), any(), any(), any()) } returns true
        koin.start(module { single { mockk<SourceManager>() } })
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    @Test
    fun noMangaDexNoRows() {
        every { MdUtil.getEnabledMangaDex(any(), any()) } returns null
        harness.show(SettingsMangadexScreen)
        harness.prefs shouldBe emptyList()
    }

    @Test
    fun enabledFollowsSources() {
        SettingsMangadexScreen.isEnabled() shouldBe true
        every { MdUtil.getEnabledMangaDexs(any(), any()) } returns emptyList()
        SettingsMangadexScreen.isEnabled() shouldBe false
    }

    @Test
    fun preferredSourceEntries() {
        harness.show(SettingsMangadexScreen)
        harness.list("Preferred MangaDex source", "7") shouldBe true
    }

    @Test
    fun pushFavoritesStartsJob() {
        harness.show(SettingsMangadexScreen)
        harness.click("Sync library entries to MangaDex")
        verify { LibraryUpdateJob.startNow(any(), target = LibraryUpdateJob.Target.PUSH_FAVORITES) }
    }

    @Test
    fun syncFollowsDialog() {
        harness.show(SettingsMangadexScreen)
        harness.click("Sync MangaDex entries to your library")
        tap("Reading")
        tap("Completed")
        tap("OK")
        koin.source.mangadexSyncToLibraryIndexes.get().size shouldBe 2
        verify { LibraryUpdateJob.startNow(any(), target = LibraryUpdateJob.Target.SYNC_FOLLOWS) }
        harness.click("Sync MangaDex entries to your library")
        tap("Cancel")
        harness.count("Completed") shouldBe 0
    }
}
