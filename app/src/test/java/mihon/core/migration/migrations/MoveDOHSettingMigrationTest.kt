package mihon.core.migration.migrations

import androidx.core.content.edit
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.network.PREF_DOH_CLOUDFLARE
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class MoveDOHSettingMigrationTest {

    private val migration = MoveDOHSettingMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val networkPreferences = NetworkPreferences(InMemoryPreferenceStore())

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 14f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin { single { networkPreferences } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun leavesDohDisabled() = runTest {
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.contains(networkPreferences.dohProvider.key()) shouldBe false
    }

    @Test
    fun movesDohToCloudflare() = runTest {
        prefs.edit { putBoolean("enable_doh", true) }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getInt(networkPreferences.dohProvider.key(), -1) shouldBe PREF_DOH_CLOUDFLARE
        prefs.contains("enable_doh") shouldBe false
    }

    private fun startBoth() {
        startMigrationKoin {
            single { app }
            single { networkPreferences }
        }
    }
}
