package mihon.core.migration.migrations

import androidx.core.content.edit
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ResetRotationSettingMigrationTest {

    private val migration = ResetRotationSettingMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 16f
    }

    @Test
    fun failsWithoutApplication() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun leavesAnUnsetRotation() = runTest {
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        prefs.contains("pref_rotation_type_key") shouldBe false
    }

    @Test
    fun resetsRotationToFree() = runTest {
        prefs.edit { putInt("pref_rotation_type_key", 4) }
        startMigrationKoin { single { app } }
        migration(migrationContext()) shouldBe true
        prefs.getInt("pref_rotation_type_key", 0) shouldBe 1
    }
}
