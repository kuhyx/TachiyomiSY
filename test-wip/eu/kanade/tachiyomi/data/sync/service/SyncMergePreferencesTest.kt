package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private fun preference(key: String, value: Int = 1): BackupPreference =
    BackupPreference(key = key, value = IntPreferenceValue(value))

internal class SyncMergePreferencesTest {

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
    fun preferencesWithBothNull() {
        service.mergePreferencesLists(localPreferences = null, remotePreferences = null) shouldBe emptyList()
    }

    @Test
    fun preferencesLocalOnly() {
        val local = listOf(preference(key = "a"))
        service.mergePreferencesLists(localPreferences = local, remotePreferences = null) shouldBe local
        logger.messages.any { it == "Using local preference: a." } shouldBe true
    }

    @Test
    fun preferencesRemoteOnly() {
        val remote = listOf(preference(key = "b"))
        service.mergePreferencesLists(localPreferences = null, remotePreferences = remote) shouldBe remote
        logger.messages.any { it == "Using remote preference: b." } shouldBe true
    }

    @Test
    fun preferencesSharedKeyKeepsLocal() {
        val local = listOf(preference(key = "a", value = 1))
        val remote = listOf(preference(key = "a", value = 2))
        service.mergePreferencesLists(localPreferences = local, remotePreferences = remote) shouldBe local
        logger.messages.any { it.endsWith("Keeping local.") } shouldBe true
    }

    @Test
    fun sourcePreferencesWithBothNull() {
        service.mergeSourcePreferencesLists(
            localPreferences = null,
            remotePreferences = null,
        ) shouldBe emptyList()
    }

    @Test
    fun sourcePreferencesLocalOnly() {
        val local = listOf(BackupSourcePreferences(sourceKey = "s", prefs = emptyList()))
        service.mergeSourcePreferencesLists(localPreferences = local, remotePreferences = null) shouldBe local
        logger.messages.any { it == "Using local source preference: s." } shouldBe true
    }

    @Test
    fun sourcePreferencesRemoteOnly() {
        val remote = listOf(BackupSourcePreferences(sourceKey = "t", prefs = emptyList()))
        service.mergeSourcePreferencesLists(localPreferences = null, remotePreferences = remote) shouldBe remote
        logger.messages.any { it == "Using remote source preference: t." } shouldBe true
    }

    @Test
    fun sourcePreferencesMergePrefs() {
        val local = listOf(BackupSourcePreferences(sourceKey = "s", prefs = listOf(preference(key = "a"))))
        val remote = listOf(BackupSourcePreferences(sourceKey = "s", prefs = listOf(preference(key = "b"))))
        val merged = service.mergeSourcePreferencesLists(localPreferences = local, remotePreferences = remote)
        merged.single().sourceKey shouldBe "s"
        merged.single().prefs.map { it.key } shouldContainExactly listOf("a", "b")
    }

    @Test
    fun individualPreferencesRemoteWins() {
        val merged = service.mergeIndividualPreferences(
            localPrefs = listOf(preference(key = "a")),
            remotePrefs = listOf(BackupPreference(key = "a", value = StringPreferenceValue("remote"))),
        )
        merged.single().value shouldBe StringPreferenceValue("remote")
    }

    @Test
    fun individualPreferencesUnion() {
        val merged = service.mergeIndividualPreferences(
            localPrefs = listOf(preference(key = "a")),
            remotePrefs = listOf(preference(key = "b")),
        )
        merged.map { it.key } shouldContainExactly listOf("a", "b")
    }
}
