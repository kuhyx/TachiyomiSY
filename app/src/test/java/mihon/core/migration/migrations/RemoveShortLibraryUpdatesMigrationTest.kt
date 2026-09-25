package mihon.core.migration.migrations

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences

internal class RemoveShortLibraryUpdatesMigrationTest {

    private val migration = RemoveShortLibraryUpdatesMigration()
    private val preferences = LibraryPreferences(InMemoryPreferenceStore())

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 22f
    }

    @Test
    fun failsWithoutPreferences() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun keepsTwelveHours() = runTest {
        startMigrationKoin { single { preferences } }
        preferences.autoUpdateInterval.set(24)
        migration(migrationContext()) shouldBe true
        preferences.autoUpdateInterval.get() shouldBe 24
    }

    @Test
    fun raisesRemovedIntervals() = runTest {
        startMigrationKoin { single { preferences } }
        for (removed in listOf(3, 4, 6, 8)) {
            preferences.autoUpdateInterval.set(removed)
            migration(migrationContext()) shouldBe true
            preferences.autoUpdateInterval.get() shouldBe 12
        }
    }
}
