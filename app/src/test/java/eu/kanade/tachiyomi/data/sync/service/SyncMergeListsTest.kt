package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SyncMergeListsTest {

    private val service = FakeSyncService()
    private var logged = mutableListOf<String>()

    @BeforeEach
    fun setUp() {
        logged = captureLogcat()
    }

    @AfterEach
    fun tearDown() = releaseLogcat()

    @Test
    fun sourcesWithoutLists() {
        service.mergeSourcesLists(null, null).shouldBeEmpty()
        logged shouldContain "Starting source merge. Local sources: null, Remote sources: null"
    }

    @Test
    fun sourcesPreferLocal() {
        val localOnly = BackupSource(name = "Local", sourceId = 1)
        val sharedLocal = BackupSource(name = "SharedLocal", sourceId = 2)
        val sharedRemote = BackupSource(name = "SharedRemote", sourceId = 2)
        val remoteOnly = BackupSource(name = "Remote", sourceId = 3)
        service.mergeSourcesLists(
            listOf(localOnly, sharedLocal),
            listOf(sharedRemote, remoteOnly),
        ) shouldBe listOf(localOnly, sharedLocal, remoteOnly)
        logged shouldContain "Using local source: Local."
        logged shouldContain "Using remote source: Remote."
        logged shouldContain "Remote and local have the same source ID: 2. Keeping local."
        logged shouldContain "Processing source ID: 3. Local source: false, Remote source: true"
    }

    @Test
    fun savedSearchesWithoutLists() {
        service.mergeSavedSearchesLists(null, null).shouldBeEmpty()
    }

    @Test
    fun savedSearchesPreferLocal() {
        val localOnly = BackupSavedSearch(name = "L", source = 1)
        val sharedLocal = BackupSavedSearch(name = "S", query = "local", source = 1)
        val sharedRemote = BackupSavedSearch(name = "S", query = "remote", source = 1)
        val remoteOnly = BackupSavedSearch(name = "S", source = 2)
        service.mergeSavedSearchesLists(
            listOf(localOnly, sharedLocal),
            listOf(sharedRemote, remoteOnly),
        ) shouldBe listOf(localOnly, sharedLocal, remoteOnly)
        logged shouldContain "Using local saved search: L."
        logged shouldContain "Using remote saved search: S."
        logged shouldContain "Both remote and local have the same saved search key: S|1. Keeping local."
        logged shouldContain "Starting saved searches merge. Local saved searches: 2, Remote saved searches: 2"
    }

    @Test
    fun preferencesWithoutLists() {
        service.mergePreferencesLists(null, null).shouldBeEmpty()
    }

    @Test
    fun preferencesPreferLocal() {
        val localOnly = BackupPreference("a", IntPreferenceValue(1))
        val sharedLocal = BackupPreference("b", BooleanPreferenceValue(true))
        val sharedRemote = BackupPreference("b", BooleanPreferenceValue(false))
        val remoteOnly = BackupPreference("c", StringPreferenceValue("x"))
        service.mergePreferencesLists(
            listOf(localOnly, sharedLocal),
            listOf(sharedRemote, remoteOnly),
        ) shouldBe listOf(localOnly, sharedLocal, remoteOnly)
        logged shouldContain "Using local preference: a."
        logged shouldContain "Using remote preference: c."
        logged shouldContain "Both remote and local have the same preference key: b. Keeping local."
    }

    @Test
    fun sourcePreferencesWithoutLists() {
        service.mergeSourcePreferencesLists(null, null).shouldBeEmpty()
    }

    @Test
    fun sourcePreferencesMergePrefs() {
        val localOnly = BackupSourcePreferences("l", emptyList())
        val remoteOnly = BackupSourcePreferences("r", emptyList())
        val sharedLocal = BackupSourcePreferences(
            sourceKey = "s",
            prefs = listOf(BackupPreference("x", IntPreferenceValue(1)), BackupPreference("y", IntPreferenceValue(1))),
        )
        val sharedRemote = BackupSourcePreferences("s", listOf(BackupPreference("y", IntPreferenceValue(2))))
        service.mergeSourcePreferencesLists(
            listOf(localOnly, sharedLocal),
            listOf(sharedRemote, remoteOnly),
        ) shouldBe listOf(
            localOnly,
            BackupSourcePreferences(
                sourceKey = "s",
                prefs = listOf(
                    BackupPreference("x", IntPreferenceValue(1)),
                    BackupPreference("y", IntPreferenceValue(2)),
                ),
            ),
            remoteOnly,
        )
        logged shouldContain "Using local source preference: l."
        logged shouldContain "Using remote source preference: r."
    }

    @Test
    fun individualPrefsRemoteWins() {
        val local = listOf(BackupPreference("k", IntPreferenceValue(1)))
        val remote = listOf(BackupPreference("k", IntPreferenceValue(2)))
        service.mergeIndividualPreferences(local, remote) shouldBe remote
    }
}
