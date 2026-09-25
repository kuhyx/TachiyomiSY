package mihon.core.migration.migrations

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences

internal class RemoveShorterLibraryUpdatesMigrationTest {

    private val migration = RemoveShorterLibraryUpdatesMigration()
    private val preferences = LibraryPreferences(InMemoryPreferenceStore())

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 18f
    }

    @Test
    fun failsWithoutPreferences() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun keepsLongerIntervals() = runTest {
        startMigrationKoin { single { preferences } }
        preferences.autoUpdateInterval.set(6)
        migration(migrationContext()) shouldBe true
        preferences.autoUpdateInterval.get() shouldBe 6
    }

    @Test
    fun raisesOneHour() = runTest {
        startMigrationKoin { single { preferences } }
        preferences.autoUpdateInterval.set(1)
        migration(migrationContext()) shouldBe true
        preferences.autoUpdateInterval.get() shouldBe 3
    }

    @Test
    fun raisesTwoHours() = runTest {
        startMigrationKoin { single { preferences } }
        preferences.autoUpdateInterval.set(2)
        migration(migrationContext()) shouldBe true
        preferences.autoUpdateInterval.get() shouldBe 3
    }
}
