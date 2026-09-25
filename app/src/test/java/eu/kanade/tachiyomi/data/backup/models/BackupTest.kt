package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test

internal class BackupTest {

    @Test
    fun everyListDefaultsToEmpty() {
        val backup = Backup(backupManga = emptyList())
        backup.backupManga shouldBe emptyList()
        backup.backupCategories shouldBe emptyList()
        backup.backupSources shouldBe emptyList()
        backup.backupPreferences shouldBe emptyList()
        backup.backupSourcePreferences shouldBe emptyList()
        backup.backupExtensionStores shouldBe emptyList()
        backup.backupSavedSearches shouldBe emptyList()
    }

    @Test
    fun dataClassMembers() {
        val backup = Backup(
            backupManga = listOf(BackupManga(source = 1L, url = "/u")),
            backupCategories = listOf(BackupCategory(name = "C")),
        )
        backup shouldNotBe Backup(backupManga = emptyList())
        backup.hashCode() shouldNotBe 0
        backup.toString() shouldNotBe ""
        backup.copy(backupManga = emptyList()).backupManga shouldBe emptyList()
        Backup(backupManga = emptyList()) shouldBe Backup(backupManga = emptyList())
    }

    @Test
    fun protoRoundTrip() {
        val backup = Backup(
            backupManga = listOf(BackupManga(source = 1L, url = "/u", title = "T")),
            backupCategories = listOf(BackupCategory(name = "C")),
            backupSources = listOf(BackupSource(name = "S", sourceId = 1L)),
            backupPreferences = listOf(BackupPreference(key = "k", value = IntPreferenceValue(1))),
            backupSourcePreferences = listOf(BackupSourcePreferences(sourceKey = "s", prefs = emptyList())),
            backupExtensionStores = listOf(backupExtensionStore()),
            backupSavedSearches = listOf(BackupSavedSearch(name = "S")),
        )
        val bytes = ProtoBuf.encodeToByteArray(Backup.serializer(), backup)
        val decoded = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
        decoded.backupManga.single().title shouldBe "T"
        decoded.backupCategories.single().name shouldBe "C"
        decoded.backupSources shouldBe backup.backupSources
        decoded.backupPreferences shouldBe backup.backupPreferences
        decoded.backupSourcePreferences shouldBe backup.backupSourcePreferences
        decoded.backupExtensionStores shouldBe backup.backupExtensionStores
        decoded.backupSavedSearches shouldBe backup.backupSavedSearches
    }

    @Test
    fun protoMinimalPayload() {
        val bytes = ProtoBuf.encodeToByteArray(Backup.serializer(), Backup(backupManga = emptyList()))
        val decoded = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
        decoded.backupManga shouldBe emptyList()
        decoded.backupCategories shouldBe emptyList()
    }
}
