package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SyncMergeChaptersTest {

    private var logged = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        logged = captureLogcat()
    }

    @AfterEach
    fun tearDown() = releaseLogcat()

    @Test
    fun notSyncingKeepsRemote() {
        val remote = listOf(chapter("r"))
        mergeChapters(listOf(chapter("l")), remote, lastSyncTime = 0, syncingChapters = false) shouldBeSameInstanceAs
            remote
    }

    @Test
    fun firstSyncKeepsBothSides() {
        val local = chapter("l")
        val remote = chapter("r")
        mergeChapters(listOf(local), listOf(remote), lastSyncTime = 0, syncingChapters = true) shouldBe
            listOf(local, remote)
        logged shouldContain "Keeping local chapter: l."
        logged shouldContain "Taking remote chapter: r."
        logged shouldContain "Chapter merge completed. Total merged chapters: 2"
    }

    @Test
    fun staleOnesAreDropped() {
        val staleLocal = chapter("sl", lastModifiedAt = 10)
        val freshLocal = chapter("fl", lastModifiedAt = 11)
        val staleRemote = chapter("sr", lastModifiedAt = 9)
        val freshRemote = chapter("fr", lastModifiedAt = 12)
        mergeChapters(
            localChapters = listOf(staleLocal, freshLocal),
            remoteChapters = listOf(staleRemote, freshRemote),
            lastSyncTime = 10,
            syncingChapters = true,
        ) shouldBe listOf(freshLocal, freshRemote)
        logged shouldContain "Dropping local chapter deleted on remote: sl."
        logged shouldContain "Dropping deleted remote chapter: sr."
    }

    @Test
    fun localWinsTakingRemoteOrder() {
        val local = chapter("c", version = 2, sourceOrder = 0)
        val remote = chapter("c", version = 1, sourceOrder = 5)
        val extra = chapter("x")
        mergeChapters(listOf(local), listOf(remote, extra), lastSyncTime = 0, syncingChapters = true) shouldBe
            listOf(local, extra)
        local.sourceOrder shouldBe 5L
        logged shouldContain "Merging chapter: c. Chosen version from: Local, Local version: 2, Remote version: 1."
    }

    @Test
    fun localWinsKeepingItsOrder() {
        val local = chapter("c", version = 1, sourceOrder = 0)
        val remote = chapter("c", version = 1, sourceOrder = 5)
        mergeChapters(listOf(local, chapter("x")), listOf(remote), lastSyncTime = 0, syncingChapters = true)
            .first() shouldBe local
        local.sourceOrder shouldBe 0L
    }

    @Test
    fun newerRemoteWins() {
        val local = chapter("c", version = 1)
        val remote = chapter("c", version = 2)
        mergeChapters(listOf(local), listOf(remote), lastSyncTime = 0, syncingChapters = true) shouldBe listOf(remote)
        logged shouldContain "Merging chapter: c. Chosen version from: Remote, Local version: 1, Remote version: 2."
        logged shouldContain "Processing chapter key: c. Local chapter: true, Remote chapter: true"
    }
}
