package eu.kanade.tachiyomi.data.sync.service

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SyncMergeChaptersTest {

    private lateinit var logger: RecordingLogger

    @BeforeEach
    fun setUp() {
        logger = installRecordingLogger()
    }

    @AfterEach
    fun tearDown() {
        removeRecordingLogger(logger)
    }

    @Test
    fun notSyncingKeepsRemote() {
        val remote = listOf(syncChapter(url = "/r"))
        mergeChapters(
            localChapters = listOf(syncChapter(url = "/l")),
            remoteChapters = remote,
            lastSyncTime = 0L,
            syncingChapters = false,
        ) shouldBe remote
    }

    @Test
    fun localOnlyKeptOnFirstSync() {
        val local = listOf(syncChapter(url = "/l"))
        mergeChapters(
            localChapters = local,
            remoteChapters = emptyList(),
            lastSyncTime = 0L,
            syncingChapters = true,
        ) shouldBe local
        logger.messages.any { it == "Keeping local chapter: /l." } shouldBe true
    }

    @Test
    fun localOnlyKeptWhenNewer() {
        val local = listOf(syncChapter(url = "/l", lastModifiedAt = 20L))
        mergeChapters(
            localChapters = local,
            remoteChapters = emptyList(),
            lastSyncTime = 10L,
            syncingChapters = true,
        ) shouldBe local
    }

    @Test
    fun localOnlyDroppedWhenStale() {
        mergeChapters(
            localChapters = listOf(syncChapter(url = "/l", lastModifiedAt = 1L)),
            remoteChapters = emptyList(),
            lastSyncTime = 10L,
            syncingChapters = true,
        ) shouldBe emptyList()
        logger.messages.any { it == "Dropping local chapter deleted on remote: /l." } shouldBe true
    }

    @Test
    fun remoteOnlyTakenOnFirstSync() {
        val remote = listOf(syncChapter(url = "/r"))
        mergeChapters(
            localChapters = emptyList(),
            remoteChapters = remote,
            lastSyncTime = 0L,
            syncingChapters = true,
        ) shouldBe remote
        logger.messages.any { it == "Taking remote chapter: /r." } shouldBe true
    }

    @Test
    fun remoteOnlyTakenWhenNewer() {
        val remote = listOf(syncChapter(url = "/r", lastModifiedAt = 20L))
        mergeChapters(
            localChapters = emptyList(),
            remoteChapters = remote,
            lastSyncTime = 10L,
            syncingChapters = true,
        ) shouldBe remote
    }

    @Test
    fun remoteOnlyDroppedWhenStale() {
        mergeChapters(
            localChapters = emptyList(),
            remoteChapters = listOf(syncChapter(url = "/r", lastModifiedAt = 1L)),
            lastSyncTime = 10L,
            syncingChapters = true,
        ) shouldBe emptyList()
        logger.messages.any { it == "Dropping deleted remote chapter: /r." } shouldBe true
    }

    @Test
    fun localWinnerKeepsOwnOrder() {
        val local = syncChapter(url = "/c", version = 2L, sourceOrder = 1L)
        val remote = syncChapter(url = "/c", version = 1L, sourceOrder = 9L)
        val merged = mergeChapters(
            localChapters = listOf(local),
            remoteChapters = listOf(remote),
            lastSyncTime = 0L,
            syncingChapters = true,
        )
        merged.single().sourceOrder shouldBe 1L
        logger.messages.any { it.contains("Chosen version from: Local") } shouldBe true
    }

    @Test
    fun localWinnerTakesRemoteOrder() {
        val local = syncChapter(url = "/c", version = 2L, sourceOrder = 1L)
        val merged = mergeChapters(
            localChapters = listOf(local),
            remoteChapters = listOf(
                syncChapter(url = "/c", version = 1L, sourceOrder = 9L),
                syncChapter(url = "/d", version = 1L),
            ),
            lastSyncTime = 0L,
            syncingChapters = true,
        )
        merged.map { it.url } shouldContainExactly listOf("/c", "/d")
        merged.first().sourceOrder shouldBe 9L
    }

    @Test
    fun remoteWinnerWhenNewerVersion() {
        val remote = syncChapter(url = "/c", version = 5L, sourceOrder = 9L)
        val merged = mergeChapters(
            localChapters = listOf(syncChapter(url = "/c", version = 1L)),
            remoteChapters = listOf(remote),
            lastSyncTime = 0L,
            syncingChapters = true,
        )
        merged.single() shouldBe remote
        logger.messages.any { it.contains("Chosen version from: Remote") } shouldBe true
    }
}
