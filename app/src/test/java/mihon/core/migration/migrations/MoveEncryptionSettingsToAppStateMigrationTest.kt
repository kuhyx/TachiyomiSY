package mihon.core.migration.migrations

import androidx.core.content.edit
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class MoveEncryptionSettingsToAppStateMigrationTest {

    private val migration = MoveEncryptionSettingsToAppStateMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val store: PreferenceStore = AndroidPreferenceStore(app)
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        stopMigrationKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 66f
    }

    @Test
    fun failsWithoutCollaborators() = runTest(dispatcher) {
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin { single { store } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun movesKeysWithoutEncryption() = runTest(dispatcher) {
        prefs.edit { putString(Preference.privateKey("cbz_password"), "pw") }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getString(Preference.appStateKey("cbz_password"), null) shouldBe "pw"
        prefs.contains(Preference.privateKey("cbz_password")) shouldBe false
        ShadowToast.shownToastCount() shouldBe 0
    }

    @Test
    fun asksForARestartWhenEncrypted() = runTest(dispatcher) {
        prefs.edit {
            putBoolean(Preference.privateKey("encrypt_database"), true)
            putString(Preference.privateKey("sql_password"), "pw")
        }
        startBoth()
        migration(migrationContext()) shouldBe true
        prefs.getBoolean(Preference.appStateKey("encrypt_database"), false) shouldBe true
        prefs.getString(Preference.appStateKey("sql_password"), null) shouldBe "pw"
        ShadowToast.getTextOfLatestToast() shouldBe "Restart the app to load your encrypted library"
    }

    private fun startBoth() {
        startMigrationKoin {
            single { app }
            single { store }
        }
    }
}
