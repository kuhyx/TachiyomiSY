package mihon.core.migration.migrations

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences

internal class CategoryPreferencesCleanupMigrationTest {

    private val migration = CategoryPreferencesCleanupMigration()
    private val libraryPreferences = LibraryPreferences(InMemoryPreferenceStore())
    private val downloadPreferences = DownloadPreferences(InMemoryPreferenceStore())
    private val getCategories = mockk<GetCategories> {
        coEvery { await() } returns listOf(Category(id = 1, name = "a", order = 0, flags = 0))
    }

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 72f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single { libraryPreferences } }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin {
            single { libraryPreferences }
            single { downloadPreferences }
        }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin {
            single { downloadPreferences }
            single { getCategories }
        }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun keepsValidCategories() = runTest {
        startAll()
        libraryPreferences.defaultCategory.set(1)
        libraryPreferences.updateCategories.set(setOf("1"))
        downloadPreferences.removeExcludeCategories.set(setOf("1"))
        migration(migrationContext()) shouldBe true
        libraryPreferences.defaultCategory.get() shouldBe 1
        libraryPreferences.updateCategories.get() shouldBe setOf("1")
        downloadPreferences.removeExcludeCategories.get() shouldBe setOf("1")
    }

    @Test
    fun dropsDeletedCategories() = runTest {
        startAll()
        libraryPreferences.defaultCategory.set(7)
        libraryPreferences.updateCategoriesExclude.set(setOf("1", "7"))
        downloadPreferences.downloadNewChapterCategories.set(setOf("9"))
        downloadPreferences.downloadNewChapterCategoriesExclude.set(setOf("1"))
        migration(migrationContext()) shouldBe true
        libraryPreferences.defaultCategory.isSet() shouldBe false
        libraryPreferences.updateCategoriesExclude.get() shouldBe setOf("1")
        downloadPreferences.downloadNewChapterCategories.get() shouldBe emptySet()
        downloadPreferences.downloadNewChapterCategoriesExclude.get() shouldBe setOf("1")
    }

    private fun startAll() {
        startMigrationKoin {
            single { libraryPreferences }
            single { downloadPreferences }
            single { getCategories }
        }
    }
}
