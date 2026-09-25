package mihon.core.migration.migrations

import androidx.core.content.edit
import eu.kanade.domain.source.service.SourcePreferences
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.AndroidPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class MoveExtensionRepoSettingsMigrationTest {

    private val migration = MoveExtensionRepoSettingsMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val store: PreferenceStore = AndroidPreferenceStore(app)
    private val sourcePreferences = SourcePreferences(store)

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 60f
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
            single { sourcePreferences }
        }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin {
            single { store }
            single { sourcePreferences }
        }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun rewritesReposAndHidesTokens() = runTest {
        sourcePreferences.extensionRepos.set(setOf("owner/repo"))
        prefs.edit {
            putString("pref_mangasync_username_1", "user")
            putString("track_token_1", "token")
            putBoolean(Preference.appStateKey("trusted_signatures"), true)
            putString("other", "kept")
        }
        startMigrationKoin {
            single { app }
            single { store }
            single { sourcePreferences }
        }
        migration(migrationContext()) shouldBe true
        sourcePreferences.extensionRepos.get() shouldBe setOf("https://raw.githubusercontent.com/owner/repo/repo")
        prefs.getString(Preference.privateKey("pref_mangasync_username_1"), null) shouldBe "user"
        prefs.getString(Preference.privateKey("track_token_1"), null) shouldBe "token"
        prefs.contains("pref_mangasync_username_1") shouldBe false
        prefs.contains("track_token_1") shouldBe false
        prefs.contains(Preference.appStateKey("trusted_signatures")) shouldBe false
        prefs.getString("other", null) shouldBe "kept"
    }
}
