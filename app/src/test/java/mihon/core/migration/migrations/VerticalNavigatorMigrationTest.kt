package mihon.core.migration.migrations

import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.InMemoryPreferenceStore.InMemoryPreference
import tachiyomi.core.common.preference.PreferenceStore

internal class VerticalNavigatorMigrationTest {

    private val migration = VerticalNavigatorMigration()
    private val readerPreferences = ReaderPreferences(InMemoryPreferenceStore())

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 79f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single<PreferenceStore> { InMemoryPreferenceStore() } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin { single { readerPreferences } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun ignoresOtherVersions() = runTest {
        startWith(InMemoryPreference("pref_webtoon_vertical_navigator", true, true))
        migration(migrationContext(previousVersion = 77)) shouldBe true
        readerPreferences.verticalNavigator.isSet() shouldBe false
        readerPreferences.verticalNavigatorOnLeft.isSet() shouldBe false
    }

    @Test
    fun enablesNavigatorFrom78() = runTest {
        startWith()
        migration(migrationContext(previousVersion = 78)) shouldBe true
        readerPreferences.verticalNavigator.get() shouldBe setOf(ReadingMode.WEBTOON, ReadingMode.CONTINUOUS_VERTICAL)
    }

    @Test
    fun keepsDisabledNavigatorFrom78() = runTest {
        startWith(InMemoryPreference("pref_webtoon_vertical_navigator", false, true))
        migration(migrationContext(previousVersion = 78)) shouldBe true
        readerPreferences.verticalNavigator.isSet() shouldBe false
    }

    @Test
    fun movesLeftHandedSetting() = runTest {
        startWith(InMemoryPreference("pref_webtoon_vertical_navigator_on_left", true, false))
        migration(migrationContext(previousVersion = 70)) shouldBe true
        readerPreferences.verticalNavigatorOnLeft.get() shouldBe true
    }

    private fun startWith(vararg legacy: InMemoryPreference<*>) {
        val store = InMemoryPreferenceStore(legacy.asSequence())
        startMigrationKoin {
            single<PreferenceStore> { store }
            single { readerPreferences }
        }
    }
}
