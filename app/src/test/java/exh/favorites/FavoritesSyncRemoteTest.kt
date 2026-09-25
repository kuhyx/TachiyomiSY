package exh.favorites

import eu.kanade.tachiyomi.network.GET
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.UpdateCategory
import tachiyomi.domain.category.model.CategoryUpdate

@RunWith(RobolectricTestRunner::class)
internal class FavoritesSyncRemoteTest {
    private val harness = FavoritesSyncHarness()
    private val errors = mutableListOf<FavoritesSyncStatus.SyncError.GallerySyncError>()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun categoriesFollowTheRemote() = runBlocking<Unit> {
        coEvery { harness.getCategories.await() } returns
            listOf(category(0, "Default", 0), category(1, "Same", 0), category(2, "Old", 1), category(3, "Third", 5))
        coEvery { harness.updateCategory.await(any()) } returns UpdateCategory.Result.Success
        coEvery { harness.createCategoryWithName.await("Fresh") } returns
            CreateCategoryWithName.Result.Success(category(4, "Fresh", 9))
        harness.helper().applyRemoteCategories(listOf("Same", "New", "Third", "Fresh"))
        coVerify(exactly = 1) { harness.updateCategory.await(CategoryUpdate(id = 2, order = null, name = "New")) }
        coVerify(exactly = 1) { harness.updateCategory.await(CategoryUpdate(id = 3, order = 2, name = null)) }
        coVerify(exactly = 1) { harness.updateCategory.await(CategoryUpdate(id = 4, order = 3, name = null)) }
    }

    @Test
    fun categoryFailuresPropagate() = runBlocking<Unit> {
        coEvery { harness.getCategories.await() } returns listOf(category(1, "Old", 0))
        coEvery { harness.updateCategory.await(any()) } returns UpdateCategory.Result.Error(IllegalStateException("u"))
        val error = shouldThrow<IllegalStateException> { harness.helper().applyRemoteCategories(listOf("New")) }
        error.message shouldBe "u"
        coEvery { harness.createCategoryWithName.await(any()) } returns
            CreateCategoryWithName.Result.InternalError(IllegalArgumentException("c"))
        shouldThrow<IllegalArgumentException> { harness.helper().applyRemoteCategories(listOf("Old", "X")) }
    }

    @Test
    fun retryStopsAtFirstSuccess() = runBlocking<Unit> {
        harness.helper().explicitlyRetryExhRequest(3, GET("https://exhentai.org/x")).shouldBeTrue()
        harness.requests.size shouldBe 1
        harness.remoteStatus = 500
        harness.helper().explicitlyRetryExhRequest(2, GET("https://exhentai.org/x")).shouldBeFalse()
        harness.requests.size shouldBe 3
        harness.remoteStatus = null
        harness.helper().explicitlyRetryExhRequest(2, GET("https://exhentai.org/x")).shouldBeFalse()
        harness.requests.size shouldBe 5
    }

    @Test
    fun addGalleryPostsTheForm() = runBlocking<Unit> {
        harness.helper().addGalleryRemote(errors, favEntry("42", category = 3))
        val request = harness.requests.single()
        request.url.toString() shouldBe "https://exhentai.org/gallerypopups.php?gid=42&t=tok42&act=addfav"
        request.method shouldBe "POST"
        errors.shouldBeEmpty()
    }

    @Test
    fun addFailureLenientOrStrict() = runBlocking<Unit> {
        harness.remoteStatus = 404
        harness.exhPreferences.exhLenientSync.set(true)
        val helper = harness.helper()
        helper.addGalleryRemote(errors, favEntry("1"))
        errors shouldContainExactly
            listOf(FavoritesSyncStatus.SyncError.GallerySyncError.UnableToAddGalleryToRemote("m1", "1"))
        harness.exhPreferences.exhLenientSync.set(false)
        shouldThrow<FavoritesSyncHelper.IgnoredException> { helper.addGalleryRemote(errors, favEntry("2")) }
        helper.status.value shouldBe
            FavoritesSyncStatus.SyncError.GallerySyncError.UnableToAddGalleryToRemote("m2", "2")
    }

    @Test
    fun changeSetRemovesThenAdds() = runBlocking<Unit> {
        val helper = harness.helper()
        helper.applyChangeSetToRemote(errors, ChangeSet(added = listOf(favEntry("5")), removed = listOf(favEntry("6"))))
        harness.requests.map { it.url.encodedPath } shouldContainExactly listOf("/favorites.php", "/gallerypopups.php")
        errors.shouldBeEmpty()
        helper.applyChangeSetToRemote(errors, ChangeSet(emptyList(), emptyList()))
        harness.requests.size shouldBe 2
    }

    @Test
    fun removeFailureLenientOrStrict() = runBlocking<Unit> {
        harness.remoteStatus = 500
        harness.exhPreferences.exhLenientSync.set(true)
        val helper = harness.helper()
        helper.applyChangeSetToRemote(errors, ChangeSet(emptyList(), listOf(favEntry("6"))))
        errors shouldContainExactly listOf(FavoritesSyncStatus.SyncError.GallerySyncError.UnableToDeleteFromRemote)
        harness.exhPreferences.exhLenientSync.set(false)
        shouldThrow<FavoritesSyncHelper.IgnoredException> {
            helper.applyChangeSetToRemote(errors, ChangeSet(emptyList(), listOf(favEntry("6"))))
        }
        helper.status.value shouldBe FavoritesSyncStatus.SyncError.GallerySyncError.UnableToDeleteFromRemote
    }
}
