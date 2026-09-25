package mihon.core.migration.migrations

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences

internal class RemoveBatteryNotLowRestrictionMigrationTest {

    private val migration = RemoveBatteryNotLowRestrictionMigration()
    private val preferences = LibraryPreferences(InMemoryPreferenceStore())

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 56f
    }

    @Test
    fun failsWithoutPreferences() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun leavesUnsetRestrictions() = runTest {
        startMigrationKoin { single { preferences } }
        migration(migrationContext()) shouldBe true
        preferences.autoUpdateDeviceRestrictions.isSet() shouldBe false
    }

    @Test
    fun leavesOtherRestrictions() = runTest {
        startMigrationKoin { single { preferences } }
        preferences.autoUpdateDeviceRestrictions.set(setOf("wifi"))
        migration(migrationContext()) shouldBe true
        preferences.autoUpdateDeviceRestrictions.get() shouldBe setOf("wifi")
    }

    @Test
    fun dropsBatteryNotLow() = runTest {
        startMigrationKoin { single { preferences } }
        preferences.autoUpdateDeviceRestrictions.set(setOf("wifi", "battery_not_low"))
        migration(migrationContext()) shouldBe true
        preferences.autoUpdateDeviceRestrictions.get() shouldBe setOf("wifi")
    }
}
