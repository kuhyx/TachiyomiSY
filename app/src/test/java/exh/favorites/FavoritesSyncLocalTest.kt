package exh.favorites

import android.net.Uri
import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.getUrl

@RunWith(RobolectricTestRunner::class)
internal class FavoritesSyncLocalTest {
    private val harness = FavoritesSyncHarness()
    private val errors = mutableListOf<FavoritesSyncStatus.SyncError.GallerySyncError>()

    @Before
    fun setUp() {
        harness.start()
        coEvery { harness.getCategories.await() } returns
            listOf(category(0, "Default", 0), category(1, "A", 0), category(2, "B", 1))
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun removedAreUnfavourited() = runBlocking<Unit> {
        val onExh = favManga(1, "1")
        val onEh = favManga(2, "2", source = EH_SOURCE_ID)
        coEvery { harness.getManga.await(favEntry("1").getUrl(), EXH_SOURCE_ID) } returns onExh
        coEvery { harness.getManga.await(favEntry("2").getUrl(), EH_SOURCE_ID) } returns onEh
        coEvery { harness.getManga.await(favEntry("3").getUrl(), EXH_SOURCE_ID) } returns
            favManga(3, "3", favorite = false)
        harness.helper().applyChangeSetToLocal(
            errors,
            ChangeSet(added = emptyList(), removed = listOf(favEntry("1"), favEntry("2"), favEntry("3"))),
        )
        coVerify(exactly = 1) { harness.updateManga.awaitUpdateFavorite(1, false) }
        coVerify(exactly = 1) { harness.updateManga.awaitUpdateFavorite(2, false) }
        coVerify(exactly = 0) { harness.updateManga.awaitUpdateFavorite(3, any()) }
        coVerify(exactly = 1) { harness.setMangaCategories.await(1, emptyList()) }
        coVerify(exactly = 1) { harness.setMangaCategories.await(2, emptyList()) }
    }

    @Test
    fun addedLandInTheirCategory() = runBlocking<Unit> {
        val manga = favManga(7, "7")
        coEvery { harness.getManga.await(any<String>(), EXH_SOURCE_ID) } returns manga
        val changes = ChangeSet(added = listOf(favEntry("7", category = 1)), removed = emptyList())
        harness.helper().applyChangeSetToLocal(errors, changes)
        coVerify(exactly = 1) { harness.setMangaCategories.await(7, listOf(2L)) }
        coVerify(exactly = 1) { harness.updateManga.awaitUpdateFavorite(7, true) }
        errors.shouldBeEmpty()
    }

    @Test
    fun vanishedGalleriesAreOnlyLogged() = runBlocking<Unit> {
        coEvery {
            harness.updateMangaFromRemote(
                manga = any(),
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } returns Result.failure(EHentai.GalleryNotFoundException(IllegalStateException()))
        coEvery { harness.getManga.await(any<String>(), any()) } returns favManga(8, "8")
        harness.helper().applyChangeSetToLocal(errors, ChangeSet(listOf(favEntry("8")), emptyList()))
        errors.shouldBeEmpty()
        coVerify(exactly = 0) { harness.setMangaCategories.await(any(), any()) }
    }

    @Test
    fun failedImportsLenientOrStrict() = runBlocking<Unit> {
        every { harness.exh.matchesUri(any()) } returns false
        harness.exhPreferences.exhLenientSync.set(true)
        val helper = harness.helper()
        helper.applyChangeSetToLocal(errors, ChangeSet(listOf(favEntry("9")), emptyList()))
        val url = "https://exhentai.org${favEntry("9").getUrl()}"
        errors.single() shouldBe FavoritesSyncStatus.SyncError.GallerySyncError.InvalidGalleryFail("m9", url)
        harness.exhPreferences.exhLenientSync.set(false)
        shouldThrow<FavoritesSyncHelper.IgnoredException> {
            helper.applyChangeSetToLocal(errors, ChangeSet(listOf(favEntry("9")), emptyList()))
        }
        helper.status.value.shouldBeInstanceOf<FavoritesSyncStatus.SyncError.GallerySyncError.InvalidGalleryFail>()
    }

    @Test
    fun otherFailuresBecomeAddFails() = runBlocking<Unit> {
        harness.exhPreferences.exhLenientSync.set(true)
        every { harness.exh.matchesUri(any()) } throws IllegalStateException("bad")
        harness.helper().applyChangeSetToLocal(errors, ChangeSet(listOf(favEntry("10")), emptyList()))
        errors.single().shouldBeInstanceOf<FavoritesSyncStatus.SyncError.GallerySyncError.InvalidGalleryFail>()
        errors.clear()
        every { harness.exh.matchesUri(any()) } returns true
        coEvery { harness.exh.mapUrlToMangaUrl(any()) } throws IllegalStateException("boom")
        harness.helper().applyChangeSetToLocal(errors, ChangeSet(listOf(favEntry("11")), emptyList()))
        errors.single().shouldBeInstanceOf<FavoritesSyncStatus.SyncError.GallerySyncError.InvalidGalleryFail>()
        errors.clear()
        coEvery { harness.exh.mapUrlToMangaUrl(any()) } answers { firstArg<Uri>().path }
        coEvery { harness.getManga.await(any<String>(), any()) } throws IllegalStateException("db down")
        harness.helper().applyChangeSetToLocal(errors, ChangeSet(listOf(favEntry("12")), emptyList()))
        errors shouldContainExactly listOf(
            FavoritesSyncStatus.SyncError.GallerySyncError.GalleryAddFail(
                "m12",
                "db down (Gallery: https://exhentai.org${favEntry("12").getUrl()})",
            ),
        )
    }
}
