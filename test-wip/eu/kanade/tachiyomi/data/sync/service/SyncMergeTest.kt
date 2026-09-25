package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference

internal class SyncMergeTest {

    private val store = MapPreferenceStore()
    private lateinit var logger: RecordingLogger
    private lateinit var service: FakeSyncService

    @BeforeEach
    fun setUp() {
        logger = installRecordingLogger()
        service = fakeSyncService(store)
    }

    @AfterEach
    fun tearDown() {
        removeRecordingLogger(logger)
    }

    private fun lastSync(millis: Long) {
        store.getLong(Preference.appStateKey("last_sync_timestamp"), 0L).set(millis)
    }

    @Test
    fun categoriesWithNullLocal() {
        val remote = listOf(syncCategory(name = "R"))
        service.mergeCategoriesLists(localCategoriesList = null, remoteCategoriesList = remote) shouldBe remote
        service.mergeCategoriesLists(
            localCategoriesList = null,
            remoteCategoriesList = null,
        ) shouldBe emptyList()
    }

    @Test
    fun categoriesWithNullRemote() {
        val local = listOf(syncCategory(name = "L"))
        service.mergeCategoriesLists(localCategoriesList = local, remoteCategoriesList = null) shouldBe local
    }

    @Test
    fun categoriesMatchByUid() {
        val local = listOf(syncCategory(name = "Local", uid = 7L, version = 2L))
        val remote = listOf(syncCategory(name = "Remote", uid = 7L, version = 1L))
        val merged = service.mergeCategoriesLists(localCategoriesList = local, remoteCategoriesList = remote)
        merged.map { it.name } shouldContainExactly listOf("Local")
    }

    @Test
    fun categoriesMatchByName() {
        val local = listOf(syncCategory(name = "Same", version = 1L))
        val remote = listOf(syncCategory(name = "Same", version = 5L, uid = 3L))
        val merged = service.mergeCategoriesLists(localCategoriesList = local, remoteCategoriesList = remote)
        merged.single().uid shouldBe 3L
        merged.single().version shouldBe 5L
    }

    @Test
    fun remoteWinnerInheritsLocalUid() {
        val local = listOf(syncCategory(name = "Same", uid = 4L, version = 1L))
        val remote = listOf(syncCategory(name = "Same", version = 9L))
        val merged = service.mergeCategoriesLists(localCategoriesList = local, remoteCategoriesList = remote)
        merged.single().uid shouldBe 4L
    }

    @Test
    fun newRemoteCategoryKept() {
        lastSync(millis = 1_000L)
        val remote = listOf(syncCategory(name = "New", lastModifiedAt = 5L))
        val merged = service.mergeCategoriesLists(localCategoriesList = emptyList(), remoteCategoriesList = remote)
        merged shouldBe remote
        logger.messages.any { it.startsWith("Adding new remote category") } shouldBe true
    }

    @Test
    fun deletedRemoteCategoryDropped() {
        lastSync(millis = 10_000L)
        val remote = listOf(syncCategory(name = "Old", lastModifiedAt = 1L))
        service.mergeCategoriesLists(
            localCategoriesList = emptyList(),
            remoteCategoriesList = remote,
        ) shouldBe emptyList()
        logger.messages.any { it.startsWith("Dropping deleted remote category") } shouldBe true
    }

    @Test
    fun localOnlyCategoryKeptWhenFresh() {
        val local = listOf(syncCategory(name = "Local", order = 2L), syncCategory(name = "Other", order = 1L))
        val merged = service.mergeCategoriesLists(localCategoriesList = local, remoteCategoriesList = emptyList())
        merged.map { it.name } shouldContainExactly listOf("Other", "Local")
    }

    @Test
    fun localOnlyCategoryDroppedWhenStale() {
        lastSync(millis = 10_000L)
        val local = listOf(syncCategory(name = "Local", lastModifiedAt = 1L))
        service.mergeCategoriesLists(
            localCategoriesList = local,
            remoteCategoriesList = emptyList(),
        ) shouldBe emptyList()
        logger.messages.any { it.startsWith("Dropping local category deleted on remote") } shouldBe true
    }

    @Test
    fun categoryModifiedAfterLastSync() {
        lastSync(millis = 1_000L)
        val local = listOf(syncCategory(name = "Local", lastModifiedAt = 5L))
        service.mergeCategoriesLists(localCategoriesList = local, remoteCategoriesList = emptyList()) shouldBe local
    }
}
