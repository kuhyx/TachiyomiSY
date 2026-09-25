package mihon.core.migration.migrations

import android.content.SharedPreferences
import androidx.core.content.edit
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RemoveDuplicateReaderPreferenceMigrationTest {

    private val migration = RemoveDuplicateReaderPreferenceMigration()
    private val prefs = defaultPrefs(robolectricApp())

    @After
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 75f
    }

    @Test
    fun failsWithoutPreferences() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun leavesTheSettingWhenOff() = runTest {
        prefs.edit { putStringSet("mark_duplicate_read_chapter_read", setOf("new")) }
        startMigrationKoin { single { prefs } }
        migration(migrationContext()) shouldBe true
        prefs.getStringSet("mark_duplicate_read_chapter_read", null) shouldBe setOf("new")
    }

    @Test
    fun foldsTheOldFlagIntoTheSet() = runTest {
        prefs.edit {
            putBoolean("mark_read_dupe", true)
            putStringSet("mark_duplicate_read_chapter_read", setOf("new"))
        }
        startMigrationKoin { single { prefs } }
        migration(migrationContext()) shouldBe true
        prefs.getStringSet("mark_duplicate_read_chapter_read", null) shouldBe setOf("new", "existing")
        prefs.contains("mark_read_dupe") shouldBe false
    }

    @Test
    fun copesWithAMissingSet() = runTest {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val store = mockk<SharedPreferences> {
            every { getBoolean("mark_read_dupe", false) } returns true
            every { getStringSet("mark_duplicate_read_chapter_read", any()) } returns null
            every { edit() } returns editor
        }
        startMigrationKoin { single { store } }
        migration(migrationContext()) shouldBe true
        verify(exactly = 1) { editor.putStringSet("mark_duplicate_read_chapter_read", null) }
        verify(exactly = 1) { editor.remove("mark_read_dupe") }
    }
}
