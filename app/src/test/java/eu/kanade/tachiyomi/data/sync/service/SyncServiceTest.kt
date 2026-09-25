package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SyncServiceTest {

    private val preferences = SyncPreferences(FlowPreferenceStore())
    private val service = FakeSyncService(preferences)

    @BeforeEach
    fun setUp() {
        captureLogcat()
    }

    @AfterEach
    fun tearDown() = releaseLogcat()

    @Test
    fun guardIsOffWithoutBaseline() {
        service.guard(entryCount = 0)
        preferences.lastSyncEntryCount.set(-1)
        service.guard(entryCount = 0)
    }

    @Test
    fun guardToleratesSmallDrops() {
        preferences.lastSyncEntryCount.set(100)
        service.guard(entryCount = 90)
        service.guard(entryCount = 150)
    }

    @Test
    fun guardRefusesACollapse() {
        preferences.lastSyncEntryCount.set(100)
        shouldThrow<SyncCollapseException> { service.guard(entryCount = 89) }.message shouldBe
            "Refusing to sync: this device now has 89 library entries, down from 100 at the last sync. " +
            "Restore this device from a backup before syncing again."
    }

    @Test
    fun mergeOfNothingIsEmpty() {
        val merged = service.merge(SyncData(), SyncData())
        merged.deviceId shouldBe preferences.uniqueDeviceID()
        merged.backup shouldBe Backup(backupManga = emptyList())
    }

    @Test
    fun mergeTakesTheOnlySide() {
        val full = Backup(
            backupManga = listOf(manga("a")),
            backupCategories = listOf(category("Cat", order = 1)),
            backupSources = listOf(BackupSource(name = "S", sourceId = 1L)),
            backupPreferences = listOf(BackupPreference("k", IntPreferenceValue(1))),
            backupSourcePreferences = listOf(BackupSourcePreferences("s", emptyList())),
            backupSavedSearches = listOf(BackupSavedSearch(name = "q")),
        )
        listOf(
            service.merge(SyncData(backup = full), SyncData()),
            service.merge(SyncData(), SyncData(backup = full)),
        ).forEach { merged ->
            val backup = checkNotNull(merged.backup)
            backup.backupManga.map { it.url } shouldBe listOf("a")
            backup.backupCategories.map { it.name } shouldBe listOf("Cat")
            backup.backupSources shouldBe full.backupSources
            backup.backupPreferences shouldBe full.backupPreferences
            backup.backupSourcePreferences shouldBe full.backupSourcePreferences
            backup.backupSavedSearches shouldBe full.backupSavedSearches
            backup.backupExtensionStores.shouldBeEmpty()
        }
    }

    @Test
    fun mergeCombinesBothSides() {
        val local = Backup(
            backupManga = listOf(manga("a", categories = listOf(1L))),
            backupCategories = listOf(category("Local", order = 1)),
        )
        val remote = Backup(
            backupManga = listOf(manga("b", categories = listOf(2L))),
            backupCategories = listOf(category("Remote", order = 2)),
        )
        val merged = service.merge(SyncData(backup = local), SyncData(deviceId = "x", backup = remote))
            .backup
            .let(::checkNotNull)
        merged.backupManga.map { it.url } shouldBe listOf("a", "b")
        merged.backupManga.map { it.categories } shouldBe listOf(listOf(1L), listOf(2L))
        merged.backupCategories.map { it.name } shouldBe listOf("Local", "Remote")
    }

    @Test
    fun fakeDoSyncAnswersRemote() = runTest {
        service.doSync(SyncData()) shouldBe null
        service.remote = Backup(backupManga = emptyList())
        service.doSync(SyncData()) shouldBe service.remote
        service.json.encodeToString(SyncData.serializer(), SyncData(deviceId = "d")) shouldBe """{"deviceId":"d"}"""
        service.syncPreferences shouldBe preferences
    }

    @Test
    fun syncDataMembers() {
        val data = SyncData()
        data.deviceId shouldBe ""
        data.backup shouldBe null
        data shouldBe SyncData()
        data shouldNotBe SyncData(deviceId = "a")
        data.hashCode() shouldBe SyncData().hashCode()
        data.toString() shouldBe "SyncData(deviceId=, backup=null)"
        data.copy(deviceId = "b").component1() shouldBe "b"
        data.component2() shouldBe null
        SyncCollapseException("boom").message shouldBe "boom"
    }

    @Test
    fun maxDropRatioIsATenth() {
        SyncService.MAX_REMOTE_DROP_RATIO shouldBe 0.10
    }
}
