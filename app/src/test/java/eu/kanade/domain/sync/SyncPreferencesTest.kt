package eu.kanade.domain.sync

import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.sync.models.SyncSettings
import eu.kanade.tachiyomi.data.sync.models.SyncTriggerOptions
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveLength
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.Preference

@RunWith(RobolectricTestRunner::class)
internal class SyncPreferencesTest {

    private val preferences = SyncPreferences(AndroidPreferenceStore(ApplicationProvider.getApplicationContext()))

    @Test
    fun defaults() {
        preferences.clientHost.get() shouldBe "https://sync.tachiyomi.org"
        preferences.clientAPIKey.get() shouldBe ""
        preferences.lastSyncTimestamp.get() shouldBe 0L
        Preference.isAppState(preferences.lastSyncTimestamp.key()) shouldBe true
        preferences.lastSyncEtag.get() shouldBe ""
        preferences.lastSyncEntryCount.get() shouldBe 0
        preferences.syncInterval.get() shouldBe 0
        preferences.syncService.get() shouldBe 0
        preferences.googleDriveAccessToken.get() shouldBe ""
        preferences.googleDriveRefreshToken.get() shouldBe ""
        preferences.isSyncEnabled() shouldBe false
    }

    @Test
    fun syncIsEnabledByAService() {
        preferences.syncService.set(2)
        preferences.isSyncEnabled() shouldBe true
    }

    @Test
    fun deviceIdIsGeneratedOnce() {
        val id = preferences.uniqueDeviceID()
        id shouldHaveLength 36
        preferences.uniqueDeviceID() shouldBe id
    }

    @Test
    fun syncSettingsRoundTrip() {
        preferences.getSyncSettings() shouldBe SyncSettings(privateSettings = true)
        val custom = SyncSettings(
            libraryEntries = false,
            categories = false,
            chapters = true,
            tracking = false,
            history = true,
            appSettings = false,
            extensionStores = true,
            sourceSettings = false,
            privateSettings = false,
            customInfo = false,
            readEntries = true,
            savedSearches = false,
        )
        preferences.setSyncSettings(custom)
        preferences.getSyncSettings() shouldBe custom
    }

    @Test
    fun triggerOptionsRoundTrip() {
        preferences.getSyncTriggerOptions() shouldBe SyncTriggerOptions()
        val custom = SyncTriggerOptions(
            syncOnChapterRead = true,
            syncOnChapterOpen = false,
            syncOnAppStart = true,
            syncOnAppResume = true,
        )
        preferences.setSyncTriggerOptions(custom)
        preferences.getSyncTriggerOptions() shouldBe custom
    }
}
