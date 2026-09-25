package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference

internal class SyncServiceTest {

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

    private fun baseline(count: Int) {
        store.getInt(Preference.appStateKey("last_sync_entry_count"), 0).set(count)
    }

    @Test
    fun syncDataDefaults() {
        val data = SyncData()
        data.deviceId shouldBe ""
        data.backup shouldBe null
        data shouldBe SyncData()
        data shouldNotBe SyncData(deviceId = "a")
        data.hashCode() shouldBe SyncData().hashCode()
        data.toString() shouldNotBe ""
        data.copy(deviceId = "b").deviceId shouldBe "b"
    }

    @Test
    fun collapseExceptionCarriesMessage() {
        SyncCollapseException("boom").message shouldBe "boom"
    }

    @Test
    fun doSyncReturnsResult() = runTest {
        service.doSync(SyncData()) shouldBe null
        val backup = Backup(backupManga = emptyList())
        service.result = backup
        service.doSync(SyncData()) shouldBe backup
    }

    @Test
    fun noBaselineNeverThrows() {
        service.assertNoCollapse(entryCount = 0)
        baseline(count = -1)
        service.assertNoCollapse(entryCount = 0)
    }

    @Test
    fun withinDropRatioPasses() {
        baseline(count = 100)
        service.assertNoCollapse(entryCount = 90)
    }

    @Test
    fun belowDropRatioThrows() {
        baseline(count = 100)
        val error = shouldThrow<SyncCollapseException> { service.assertNoCollapse(entryCount = 89) }
        error.message shouldBe
            "Refusing to sync: this device now has 89 library entries, down from " +
            "100 at the last sync. Restore this device from a backup before syncing again."
    }

    @Test
    fun dropRatioConstant() {
        SyncService.MAX_REMOTE_DROP_RATIO shouldBe 0.10
    }

    @Test
    fun mergeSyncDataWithNullBackups() {
        val merged = service.merge(local = SyncData(), remote = SyncData())
        merged.deviceId shouldNotBe ""
        merged.backup shouldBe Backup(backupManga = emptyList())
    }

    @Test
    fun mergeSyncDataCombinesSections() {
        val local = SyncData(
            backup = Backup(
                backupManga = listOf(syncManga(url = "/a")),
                backupCategories = listOf(syncCategory(name = "L")),
                backupSources = listOf(BackupSource(name = "L", sourceId = 1L)),
                backupPreferences = listOf(BackupPreference(key = "k", value = IntPreferenceValue(1))),
                backupSourcePreferences = listOf(BackupSourcePreferences(sourceKey = "s", prefs = emptyList())),
            ),
        )
        val remote = SyncData(
            backup = Backup(
                backupManga = listOf(syncManga(url = "/b")),
                backupCategories = listOf(syncCategory(name = "R", order = 1L)),
                backupSources = listOf(BackupSource(name = "R", sourceId = 2L)),
            ),
        )
        val merged = service.merge(local = local, remote = remote)
        val backup = requireNotNull(merged.backup)
        backup.backupManga.map { it.url } shouldBe listOf("/a", "/b")
        backup.backupCategories.map { it.name } shouldBe listOf("L", "R")
        backup.backupSources.map { it.sourceId } shouldBe listOf(1L, 2L)
        backup.backupPreferences.map { it.key } shouldBe listOf("k")
        backup.backupSourcePreferences.map { it.sourceKey } shouldBe listOf("s")
    }

    @Test
    fun mergeSyncDataKeepsDeviceId() {
        val first = service.merge(local = SyncData(), remote = SyncData()).deviceId
        service.merge(local = SyncData(), remote = SyncData()).deviceId shouldBe first
    }
}
