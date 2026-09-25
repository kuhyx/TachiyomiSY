package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.sync.SyncPreferences
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SyncMergeCategoriesTest {

    private val preferences = SyncPreferences(FlowPreferenceStore())
    private val service = FakeSyncService(preferences)
    private var logged = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        logged = captureLogcat()
    }

    @AfterEach
    fun tearDown() = releaseLogcat()

    @Test
    fun missingListsFallBack() {
        val only = listOf(category("A"))
        service.mergeCategoriesLists(null, null).shouldBeEmpty()
        service.mergeCategoriesLists(null, only) shouldBeSameInstanceAs only
        service.mergeCategoriesLists(only, null) shouldBeSameInstanceAs only
    }

    @Test
    fun uidMatchKeepsHigherLocal() {
        val local = category("A", version = 2, uid = 7)
        val remote = category("Renamed", version = 1, uid = 7)
        service.mergeCategoriesLists(listOf(local), listOf(remote)) shouldBe listOf(local)
        logged shouldContain "Keeping local category: A (UID: 7)"
    }

    @Test
    fun uidMatchTakesNewerRemote() {
        val local = category("A", version = 1, uid = 7)
        val remote = category("Renamed", version = 2, uid = 7)
        service.mergeCategoriesLists(listOf(local), listOf(remote)) shouldBe listOf(remote)
        logged shouldContain "Keeping remote category: Renamed (UID: 7)"
    }

    @Test
    fun remoteWithoutUidInheritsIt() {
        val local = category("A", version = 1, uid = 7)
        val remote = category("A", version = 2)
        service.mergeCategoriesLists(listOf(local), listOf(remote)) shouldBe listOf(remote)
        remote.uid shouldBe 7L
    }

    @Test
    fun unknownUidFallsBackToName() {
        val local = category("A", version = 0, uid = 3)
        val remote = category("A", version = 1, uid = 9)
        service.mergeCategoriesLists(listOf(local), listOf(remote)) shouldBe listOf(remote)
        remote.uid shouldBe 9L
    }

    @Test
    fun localWithoutUidMatchesByName() {
        val local = category("B", version = 3)
        service.mergeCategoriesLists(listOf(local), listOf(category("B", version = 1))) shouldBe listOf(local)
    }

    @Test
    fun firstSyncKeepsUnmatchedOnes() {
        val local = category("Local", order = 2)
        val remote = category("Remote", order = 1)
        service.mergeCategoriesLists(listOf(local), listOf(remote)) shouldBe listOf(remote, local)
        logged shouldContain "Adding new remote category: Remote (UID: 0)"
        logged shouldContain "Keeping local only category: Local (UID: 0)"
    }

    @Test
    fun laterSyncKeepsOnlyFreshOnes() {
        preferences.lastSyncTimestamp.set(5_000L)
        val freshLocal = category("FreshLocal", order = 3, lastModifiedAt = 6)
        val staleLocal = category("StaleLocal", order = 4, lastModifiedAt = 4)
        val freshRemote = category("FreshRemote", order = 1, lastModifiedAt = 6)
        val staleRemote = category("StaleRemote", order = 2, lastModifiedAt = 5)
        service.mergeCategoriesLists(
            listOf(freshLocal, staleLocal),
            listOf(freshRemote, staleRemote),
        ) shouldBe listOf(freshRemote, freshLocal)
        logged shouldContain "Dropping deleted remote category: StaleRemote (UID: 0)"
        logged shouldContain "Dropping local category deleted on remote: StaleLocal (UID: 0)"
    }
}
