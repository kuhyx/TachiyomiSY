package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.source.Source
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.source.local.LocalSource

@RunWith(RobolectricTestRunner::class)
internal class LibraryScreenFlowsTest {
    private val harness = LibraryHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun badges(downloads: Int, unread: Long, language: String) = LibraryItem.Badges(
        downloadCount = downloads,
        unreadCount = unread,
        isLocal = false,
        sourceLanguage = language,
    )

    @Test
    fun itemPrefsReadEveryPreference() {
        val model = harness.model()
        val restrictions = harness.libraryPreferences.autoUpdateMangaRestrictions
        restrictions.set(setOf(LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD))
        harness.basePreferences.downloadedOnly.set(true)
        harness.libraryPreferences.filterLewd.set(TriState.ENABLED_NOT)
        val prefs = runBlocking { model.getLibraryItemPreferencesFlow().first() }
        prefs.skipOutsideReleasePeriod shouldBe true
        prefs.globalFilterDownloaded shouldBe true
        prefs.filterLewd shouldBe TriState.ENABLED_NOT
        restrictions.set(emptySet())
        runBlocking { model.getLibraryItemPreferencesFlow().first() }.skipOutsideReleasePeriod shouldBe false
    }

    @Test
    fun badgesFollowThePreferences() {
        val model = harness.model()
        val source = mockk<Source> { every { lang } returns "en" }
        every { harness.sourceManager.getOrStub(1L) } returns source
        val manga = libManga(1L)
        every { harness.downloadManager.getDownloadCount(manga) } returns 3
        val entry = libEntry(manga, total = 4L, read = 1L)
        val shown = runBlocking { model.toLibraryItem(entry, itemPrefs(badges = true)) }
        shown.badges shouldBe badges(downloads = 3, unread = 3L, language = "en")
        val hidden = runBlocking { model.toLibraryItem(entry, itemPrefs()) }
        hidden.badges shouldBe badges(downloads = 0, unread = 0L, language = "")
        hidden.downloadCount shouldBe 3
        hidden.unreadCount shouldBe 3L
    }

    @Test
    fun localAndMergedEntries() {
        val model = harness.model()
        val local = libEntry(libManga(1L, source = LocalSource.ID))
        runBlocking { model.toLibraryItem(local, itemPrefs(badges = true)) }.badges.isLocal shouldBe true
        runBlocking { model.toLibraryItem(local, itemPrefs()) }.isLocal shouldBe true
        val parts = listOf(libManga(11L), libManga(12L))
        coEvery { harness.getMergedManga.await(10L) } returns parts
        every { harness.downloadManager.getDownloadCount(parts[0]) } returns 1
        every { harness.downloadManager.getDownloadCount(parts[1]) } returns 2
        val merged = libEntry(libManga(10L, source = MERGED_SOURCE_ID))
        runBlocking { model.toLibraryItem(merged, itemPrefs()) }.downloadCount shouldBe 3
    }

    @Test
    fun favoritesFlowBuildsItems() {
        val model = harness.model()
        harness.libraryManga.value = listOf(libEntry(libManga(1L)), libEntry(libManga(2L)))
        runBlocking { model.getFavoritesFlow().first() }.map { it.id } shouldBe listOf(1L, 2L)
    }

    @Test
    fun trackingFiltersPerTracker() {
        val model = harness.model()
        runBlocking { model.getTrackingFiltersFlow().first() } shouldBe emptyMap()
        harness.loggedIn.value = listOf(mockk<BaseTracker> { every { id } returns 5L })
        harness.libraryPreferences.filterTracking(5).set(TriState.ENABLED_IS)
        runBlocking { model.getTrackingFiltersFlow().first() } shouldBe mapOf(5L to TriState.ENABLED_IS)
    }
}
