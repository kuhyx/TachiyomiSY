package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference

internal class SyncMergeMangaTest {

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

    private fun merge(
        local: List<BackupManga>?,
        remote: List<BackupManga>?,
        localCategories: List<BackupCategory> = emptyList(),
        remoteCategories: List<BackupCategory> = emptyList(),
        mergedCategories: List<BackupCategory> = emptyList(),
    ): List<BackupManga> = service.mergeMangaLists(
        localMangaList = local,
        remoteMangaList = remote,
        localCategories = localCategories,
        remoteCategories = remoteCategories,
        mergedCategories = mergedCategories,
    )

    @Test
    fun bothListsNull() {
        merge(local = null, remote = null) shouldBe emptyList()
    }

    @Test
    fun localOnlyKeptOnFirstSync() {
        val local = listOf(syncManga(url = "/a"))
        merge(local = local, remote = null) shouldBe local
    }

    @Test
    fun localOnlyDroppedWhenStale() {
        lastSync(millis = 10_000L)
        merge(
            local = listOf(syncManga(url = "/a", lastModifiedAt = 1L)),
            remote = null,
        ) shouldBe emptyList()
        logger.messages.any { it == "Dropping local manga deleted on remote: /a." } shouldBe true
    }

    @Test
    fun remoteOnlyKeptWhenNewer() {
        lastSync(millis = 10_000L)
        val remote = listOf(syncManga(url = "/b", lastModifiedAt = 100L))
        merge(local = null, remote = remote) shouldBe remote
    }

    @Test
    fun remoteOnlyDroppedWhenStale() {
        lastSync(millis = 100_000L)
        // One stale entry out of twenty stays under the collapse ratio.
        val fresh = List(size = 19) { syncManga(url = "/f$it", lastModifiedAt = 1_000L) }
        val merged = merge(local = null, remote = fresh + syncManga(url = "/b", lastModifiedAt = 1L))
        merged.map { it.url } shouldBe fresh.map { it.url }
        logger.messages.any { it == "Dropping deleted remote manga: /b." } shouldBe true
    }

    @Test
    fun collapseWhenTooManyDropped() {
        lastSync(millis = 100_000L)
        val remote = List(size = 20) { syncManga(url = "/r$it", lastModifiedAt = 1L) }
        val error = shouldThrow<SyncCollapseException> { merge(local = emptyList(), remote = remote) }
        error.message shouldBe
            "Refusing to sync: 20 of 20 server entries are missing from this device. " +
            "That is a damaged local library, not a deletion -- " +
            "restore this device from a backup before syncing again."
    }

    @Test
    fun localVersionWinsAndMergesChapters() {
        val local = syncManga(url = "/a", version = 3L, chapters = listOf(syncChapter(url = "/c1")))
        val remote = syncManga(url = "/a", version = 1L, chapters = listOf(syncChapter(url = "/c2")))
        val merged = merge(local = listOf(local), remote = listOf(remote))
        merged.single() shouldBe local
        merged.single().chapters.map { it.url } shouldContainExactly listOf("/c1", "/c2")
        logger.messages.any { it == "Keeping local version of /a with merged chapters." } shouldBe true
    }

    @Test
    fun remoteVersionWinsAndMergesChapters() {
        val local = syncManga(url = "/a", version = 1L, chapters = listOf(syncChapter(url = "/c1")))
        val remote = syncManga(url = "/a", version = 9L, chapters = listOf(syncChapter(url = "/c2")))
        val merged = merge(local = listOf(local), remote = listOf(remote))
        merged.single() shouldBe remote
        logger.messages.any { it == "Keeping remote version of /a with merged chapters." } shouldBe true
    }

    @Test
    fun localCategoriesRemapped() {
        val merged = merge(
            local = listOf(syncManga(url = "/a", categories = listOf(0L, 5L))),
            remote = null,
            localCategories = listOf(syncCategory(name = "Reading", order = 0L)),
            mergedCategories = listOf(syncCategory(name = "Reading", order = 3L)),
        )
        merged.single().categories shouldContainExactly listOf(3L)
    }

    @Test
    fun remoteCategoriesRemapped() {
        val merged = merge(
            local = null,
            remote = listOf(syncManga(url = "/b", categories = listOf(1L, 7L))),
            remoteCategories = listOf(syncCategory(name = "Done", order = 1L), syncCategory(name = "Gone", order = 7L)),
            mergedCategories = listOf(syncCategory(name = "Done", order = 4L)),
        )
        merged.single().categories shouldContainExactly listOf(4L)
    }
}
