package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SyncMergeSourcesTest {

    private lateinit var logger: RecordingLogger
    private lateinit var service: FakeSyncService

    @BeforeEach
    fun setUp() {
        logger = installRecordingLogger()
        service = fakeSyncService()
    }

    @AfterEach
    fun tearDown() {
        removeRecordingLogger(logger)
    }

    @Test
    fun sourcesWithBothNull() {
        service.mergeSourcesLists(localSources = null, remoteSources = null) shouldBe emptyList()
    }

    @Test
    fun sourcesLocalOnly() {
        val local = listOf(BackupSource(name = "L", sourceId = 1L))
        service.mergeSourcesLists(localSources = local, remoteSources = null) shouldBe local
        logger.messages.any { it == "Using local source: L." } shouldBe true
    }

    @Test
    fun sourcesRemoteOnly() {
        val remote = listOf(BackupSource(name = "R", sourceId = 2L))
        service.mergeSourcesLists(localSources = null, remoteSources = remote) shouldBe remote
        logger.messages.any { it == "Using remote source: R." } shouldBe true
    }

    @Test
    fun sourcesSharedIdKeepsLocal() {
        val local = listOf(BackupSource(name = "L", sourceId = 1L))
        val remote = listOf(BackupSource(name = "R", sourceId = 1L))
        service.mergeSourcesLists(localSources = local, remoteSources = remote) shouldBe local
        logger.messages.any { it.endsWith("Keeping local.") } shouldBe true
    }

    @Test
    fun sourcesUnionOfIds() {
        val local = listOf(BackupSource(name = "L", sourceId = 1L))
        val remote = listOf(BackupSource(name = "R", sourceId = 2L))
        val merged = service.mergeSourcesLists(localSources = local, remoteSources = remote)
        merged.map { it.sourceId } shouldContainExactly listOf(1L, 2L)
    }

    @Test
    fun savedSearchesWithBothNull() {
        service.mergeSavedSearchesLists(localSearches = null, remoteSearches = null) shouldBe emptyList()
    }

    @Test
    fun savedSearchesLocalOnly() {
        val local = listOf(BackupSavedSearch(name = "L", source = 1L))
        service.mergeSavedSearchesLists(localSearches = local, remoteSearches = null) shouldBe local
        logger.messages.any { it == "Using local saved search: L." } shouldBe true
    }

    @Test
    fun savedSearchesRemoteOnly() {
        val remote = listOf(BackupSavedSearch(name = "R", source = 2L))
        service.mergeSavedSearchesLists(localSearches = null, remoteSearches = remote) shouldBe remote
        logger.messages.any { it == "Using remote saved search: R." } shouldBe true
    }

    @Test
    fun savedSearchesSharedKeyKeepsLocal() {
        val local = listOf(BackupSavedSearch(name = "S", query = "local", source = 1L))
        val remote = listOf(BackupSavedSearch(name = "S", query = "remote", source = 1L))
        service.mergeSavedSearchesLists(localSearches = local, remoteSearches = remote) shouldBe local
        logger.messages.any { it.endsWith("Keeping local.") } shouldBe true
    }

    @Test
    fun savedSearchesUnionOfKeys() {
        val local = listOf(BackupSavedSearch(name = "S", source = 1L))
        val remote = listOf(BackupSavedSearch(name = "S", source = 2L))
        val merged = service.mergeSavedSearchesLists(localSearches = local, remoteSearches = remote)
        merged.map { it.source } shouldContainExactly listOf(1L, 2L)
    }
}
