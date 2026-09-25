package mihon.core.migration.migrations

import androidx.core.content.edit
import androidx.work.WorkManager
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.util.system.workManager
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum

@RunWith(RobolectricTestRunner::class)
internal class RemoveUpdateCheckerJobsMigrationTest {

    private val migration = RemoveUpdateCheckerJobsMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val store: PreferenceStore = AndroidPreferenceStore(app)
    private val tracker = mockk<BaseTracker> { every { id } returns 9L }
    private val trackerManager = mockk<TrackerManager> { every { trackers } returns listOf(tracker) }
    private val workManager = mockk<WorkManager>(relaxed = true)

    @After
    fun tearDown() {
        stopMigrationKoin()
        unmockkAll()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 52f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin {
            single { app }
            single { store }
        }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin {
            single { app }
            single { trackerManager }
        }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin {
            single { store }
            single { trackerManager }
        }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun cancelsJobsAndConvertsFilters() = runTest {
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        every { app.workManager } returns workManager
        prefs.edit {
            putBoolean("automatic_ext_updates", true)
            putInt("pref_filter_library_downloaded", 1)
            putInt("pref_filter_library_unread", 2)
            putInt("pref_filter_library_tracked_9", 7)
        }
        startMigrationKoin {
            single { app }
            single { store }
            single { trackerManager }
        }
        migration(migrationContext()) shouldBe true
        verify(exactly = 1) { workManager.cancelAllWorkByTag("UpdateChecker") }
        verify(exactly = 1) { workManager.cancelAllWorkByTag("ExtensionUpdate") }
        prefs.contains("automatic_ext_updates") shouldBe false
        prefs.contains("pref_filter_library_downloaded") shouldBe false
        store.getEnum("pref_filter_library_downloaded_v2", TriState.DISABLED).get() shouldBe TriState.ENABLED_IS
        store.getEnum("pref_filter_library_unread_v2", TriState.DISABLED).get() shouldBe TriState.ENABLED_NOT
        store.getEnum("pref_filter_library_tracked_9_v2", TriState.DISABLED).get() shouldBe TriState.DISABLED
        store.getEnum("pref_filter_library_started_v2", TriState.DISABLED).isSet() shouldBe true
    }
}
