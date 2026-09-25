package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.sync.models.SyncSettings
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SyncMergeMangaTest {

    private val preferences = SyncPreferences(FlowPreferenceStore())
    private val service = FakeSyncService(preferences)
    private var logged = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        logged = captureLogcat()
    }

    @AfterEach
    fun tearDown() = releaseLogcat()

    private fun merge(local: List<BackupManga>?, remote: List<BackupManga>?): List<BackupManga> =
        service.mergeMangaLists(local, remote, emptyList(), emptyList(), emptyList())

    @Test
    fun missingListsMergeToNothing() {
        merge(null, null).shouldBeEmpty()
        logged shouldContain "Starting merge. Local list size: 0, Remote list size: 0"
    }

    @Test
    fun firstSyncKeepsBothSides() {
        val local = manga("a", favorite = false)
        val remote = manga("b")
        merge(listOf(local), listOf(remote)) shouldBe listOf(local, remote)
        logged shouldContain "Merge completed. Total merged manga: 2, Favorites: 1, Non-Favorites: 1"
    }

    @Test
    fun staleLocalOnlyIsDropped() {
        preferences.lastSyncTimestamp.set(10_000L)
        val stale = manga("stale", lastModifiedAt = 10)
        val fresh = manga("fresh", lastModifiedAt = 11)
        merge(listOf(stale, fresh), null) shouldBe listOf(fresh)
        logged shouldContain "Dropping local manga deleted on remote: stale."
    }

    @Test
    fun fewStaleRemotesAreDeletions() {
        preferences.lastSyncTimestamp.set(10_000L)
        val shared = (1..9).map { manga("m$it", lastModifiedAt = 1) }
        val stale = manga("gone", lastModifiedAt = 1)
        val fresh = manga("new", lastModifiedAt = 20)
        merge(shared, shared + stale).map { it.url } shouldBe shared.map { it.url }
        merge(emptyList(), listOf(fresh)) shouldBe listOf(fresh)
        logged shouldContain "Dropping deleted remote manga: gone."
    }

    @Test
    fun manyStaleRemotesAreACollapse() {
        preferences.lastSyncTimestamp.set(10_000L)
        val remote = (1..10).map { manga("m$it", lastModifiedAt = 1) }
        shouldThrow<SyncCollapseException> { merge(remote.drop(2), remote) }.message shouldBe
            "Refusing to sync: 2 of 10 server entries are missing from this device. That is a damaged local " +
            "library, not a deletion -- restore this device from a backup before syncing again."
    }

    @Test
    fun higherLocalVersionWins() {
        val local = manga("a", version = 2, chapters = listOf(chapter("c1")))
        val remote = manga("a", version = 1, chapters = listOf(chapter("c2")))
        val merged = merge(listOf(local), listOf(remote)).single()
        merged shouldBe local
        merged.chapters.map { it.url } shouldBe listOf("c1", "c2")
        logged shouldContain "Keeping local version of a with merged chapters."
    }

    @Test
    fun higherRemoteVersionWins() {
        val local = manga("a", version = 1)
        val remote = manga("a", version = 2, chapters = listOf(chapter("c2")))
        val merged = merge(listOf(local), listOf(remote)).single()
        merged shouldBe remote
        merged.chapters.map { it.url } shouldBe listOf("c2")
        logged shouldContain "Keeping remote version of a with merged chapters."
    }

    @Test
    fun chaptersUntouchedWhenOff() {
        preferences.setSyncSettings(SyncSettings(chapters = false))
        val remoteChapters = listOf(chapter("r"))
        val local = manga("a", version = 3, chapters = listOf(chapter("l")))
        merge(listOf(local), listOf(manga("a", chapters = remoteChapters))).single().chapters shouldBe remoteChapters
    }

    @Test
    fun categoryOrdersAreRemapped() {
        val localCategories = listOf(category("A", order = 1), category("B", order = 2))
        val remoteCategories = listOf(category("A", order = 5))
        val merged = listOf(category("A", order = 10))
        val localOnly = manga("l", categories = listOf(1L, 2L, 3L))
        val remoteOnly = manga("r", categories = listOf(5L, 1L))
        val shared = manga("s", version = 1, categories = listOf(2L))
        val remoteShared = manga("s", version = 2, categories = listOf(5L))
        service.mergeMangaLists(
            localMangaList = listOf(localOnly, shared),
            remoteMangaList = listOf(remoteOnly, remoteShared),
            localCategories = localCategories,
            remoteCategories = remoteCategories,
            mergedCategories = merged,
        ).map { it.categories } shouldBe listOf(listOf(10L), listOf(10L), listOf(10L))
    }
}
