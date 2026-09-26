package eu.kanade.tachiyomi.ui.browse.feed

import eu.kanade.presentation.browse.FeedItemUI
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FeedScreenModelTest {
    private val harness = FeedHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun FeedScreenModel.loaded(): List<FeedItemUI> {
        eventually { !state.value.isLoading && !state.value.isLoadingItems }
        return state.value.items.orEmpty()
    }

    @Test
    fun emptyFeedIsEmpty() {
        val model = FeedScreenModel()
        model.loaded() shouldBe emptyList()
        model.state.value.isEmpty shouldBe true
        FeedScreenState().isLoadingItems shouldBe true
    }

    @Test
    fun latestAndSavedSearchFeeds() {
        coEvery { harness.getSavedSearches.await() } returns listOf(savedSearch(5L))
        harness.feeds.value = listOf(feed(1L), feed(2L, savedSearch = 5L), feed(3L, source = 9L))
        val items = FeedScreenModel().loaded()
        items.map { it.title } shouldBe listOf("One", "Search 5", "9")
        items[1].subtitle shouldBe "One"
        items.map { it.results?.map { manga -> manga.title } } shouldBe
            listOf(listOf("Latest"), listOf("Found"), emptyList())
    }

    @Test
    fun missingSourceSearchUsesId() {
        coEvery { harness.getSavedSearches.await() } returns listOf(savedSearch(5L))
        harness.feeds.value = listOf(feed(3L, source = 9L, savedSearch = 5L))
        FeedScreenModel().loaded().single().subtitle shouldBe "9"
    }

    @Test
    fun filtersAreRestored() {
        coEvery { harness.getSavedSearches.await() } returns listOf(
            savedSearch(5L, filtersJson = "[]"),
            savedSearch(6L, filtersJson = "not json", query = null),
        )
        harness.feeds.value = listOf(feed(1L, savedSearch = 5L), feed(2L, savedSearch = 6L))
        FeedScreenModel().loaded().size shouldBe 2
        coVerify { harness.source.getSearchManga(1, "", any()) }
    }

    @Test
    fun failingSourcesYieldNothing() {
        coEvery { harness.source.getLatestUpdates(1) } throws IllegalStateException("down")
        harness.feeds.value = listOf(feed(1L))
        FeedScreenModel().loaded().single().results shouldBe emptyList()
    }

    @Test
    fun reloadClearsResults() {
        harness.feeds.value = listOf(feed(1L))
        val model = FeedScreenModel()
        model.loaded()
        model.pushed = true
        model.init()
        model.pushed shouldBe false
        eventually { !model.state.value.isLoadingItems }
        coVerify(timeout = 5_000, exactly = 2) { harness.source.getLatestUpdates(1) }
        model.onDispose()
    }

    @Test
    fun feedErrorsAreReported() {
        every { harness.getFeeds.subscribe() } returns flow { error("db") }
        val model = FeedScreenModel()
        runBlocking { withTimeout(10_000L) { model.events.first() } } shouldBe
            FeedScreenModel.Event.FailedFetchingSources
        model.init()
        model.state.value.isLoading shouldBe true
    }

    @Test
    fun addDialogListsEnabledSources() {
        harness.koin.sourcePreferences.enabledLanguages.set(setOf("en"))
        harness.koin.sourcePreferences.pinnedSources.set(setOf("3"))
        harness.koin.sourcePreferences.disabledSources.set(setOf("2", "x"))
        harness.catalogue += harness.source(2L, "Two")
        harness.catalogue += harness.source(3L, "Three")
        harness.catalogue += harness.source(4L, "Four", sourceLang = "fr")
        val model = FeedScreenModel()
        model.openAddDialog()
        eventually { model.state.value.dialog != null }
        model.state.value.dialog.shouldBeInstanceOf<FeedScreenModel.Dialog.AddFeed>().options.map { it.id } shouldBe
            listOf(3L, 1L)
        coEvery { harness.count.await() } returns 11L
        model.openAddDialog()
        runBlocking { withTimeout(10_000L) { model.events.first() } } shouldBe FeedScreenModel.Event.TooManyFeeds
    }

    @Test
    fun searchAndDeleteDialogs() {
        coEvery { harness.bySource.await(1L) } returns listOf(savedSearch(5L))
        val model = FeedScreenModel()
        model.openAddSearchDialog(harness.source)
        eventually { model.state.value.dialog is FeedScreenModel.Dialog.AddFeedSearch }
        (model.state.value.dialog as FeedScreenModel.Dialog.AddFeedSearch).options shouldBe
            listOf(null, savedSearch(5L))
        model.openAddSearchDialog(harness.source(2L, "Two"))
        eventually { (model.state.value.dialog as? FeedScreenModel.Dialog.AddFeedSearch)?.options?.size == 0 }
        model.openDeleteDialog(feed(1L))
        eventually { model.state.value.dialog == FeedScreenModel.Dialog.DeleteFeed(feed(1L)) }
        model.dismissDialog()
        model.state.value.dialog shouldBe null
    }

    @Test
    fun feedsAreCreatedAndDeleted() {
        val model = FeedScreenModel()
        model.createFeed(harness.source, null)
        model.createFeed(harness.source, savedSearch(5L))
        model.deleteFeed(feed(4L))
        coVerify(timeout = 5_000) { harness.insert.await(feed(-1L)) }
        coVerify(timeout = 5_000) { harness.insert.await(feed(-1L, savedSearch = 5L)) }
        coVerify(timeout = 5_000) { harness.delete.await(4L) }
    }
}
