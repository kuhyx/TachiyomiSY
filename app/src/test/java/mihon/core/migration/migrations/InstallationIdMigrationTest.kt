package mihon.core.migration.migrations

import eu.kanade.domain.base.BasePreferences
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.core.migration.Migration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

internal class InstallationIdMigrationTest {

    private val migration = InstallationIdMigration()
    private val preferences = BasePreferences(mockk(relaxed = true), InMemoryPreferenceStore())

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun runsAlways() {
        migration.version shouldBe Migration.ALWAYS
    }

    @Test
    fun failsWithoutPreferences() = runTest {
        startMigrationKoin { }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun keepsExistingId() = runTest {
        startMigrationKoin { single { preferences } }
        preferences.installationId.set("existing")
        migration(migrationContext()) shouldBe true
        preferences.installationId.get() shouldBe "existing"
    }

    @Test
    fun generatesMissingId() = runTest {
        startMigrationKoin { single { preferences } }
        migration(migrationContext()) shouldBe true
        preferences.installationId.isSet() shouldBe true
        preferences.installationId.get() shouldHaveLength 36
        preferences.installationId.get() shouldNotBe "existing"
    }
}
