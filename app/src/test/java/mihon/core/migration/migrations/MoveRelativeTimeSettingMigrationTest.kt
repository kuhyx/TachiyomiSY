package mihon.core.migration.migrations

import eu.kanade.domain.ui.UiPreferences
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.PreferenceStore

internal class MoveRelativeTimeSettingMigrationTest {

    private val migration = MoveRelativeTimeSettingMigration()
    private val uiPreferences = UiPreferences(InMemoryPreferenceStore())

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 57f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single<PreferenceStore> { InMemoryPreferenceStore() } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin { single { uiPreferences } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun keepsRelativeTimeForDays() = runTest {
        startWithLegacyValue(3)
        migration(migrationContext()) shouldBe true
        uiPreferences.relativeTime.get() shouldBe true
    }

    @Test
    fun keepsRelativeTimeByDefault() = runTest {
        startMigrationKoin {
            single<PreferenceStore> { InMemoryPreferenceStore() }
            single { uiPreferences }
        }
        migration(migrationContext()) shouldBe true
        uiPreferences.relativeTime.get() shouldBe true
    }

    @Test
    fun disablesRelativeTimeWhenZero() = runTest {
        startWithLegacyValue(0)
        migration(migrationContext()) shouldBe true
        uiPreferences.relativeTime.get() shouldBe false
    }

    private fun startWithLegacyValue(days: Int) {
        val store = InMemoryPreferenceStore(sequenceOf(InMemoryPreference("relative_time", days, 7)))
        startMigrationKoin {
            single<PreferenceStore> { store }
            single { uiPreferences }
        }
    }
}
