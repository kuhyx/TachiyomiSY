package mihon.core.migration.migrations

import androidx.core.content.edit
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.data.CategoriesQueries
import tachiyomi.data.Database
import tachiyomi.data.GetCategories
import tachiyomi.data.awaitList
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences

@RunWith(RobolectricTestRunner::class)
internal class MoveSortingModeSettingsMigrationTest {

    private val migration = MoveSortingModeSettingsMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val libraryPreferences = LibraryPreferences(InMemoryPreferenceStore())
    private val key = libraryPreferences.sortingMode.key()
    private val queries = mockk<CategoriesQueries>()
    private val database = mockk<Database> { every { categoriesQueries } returns queries }

    @After
    fun tearDown() {
        stopMigrationKoin()
        unmockkAll()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 38f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin {
            single { app }
            single { libraryPreferences }
        }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin {
            single { app }
            single { database }
        }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin {
            single { libraryPreferences }
            single { database }
        }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun renamesTheSortingModes() = runTest {
        stubCategories(emptyList())
        val renamed = mapOf(
            "LAST_CHECKED" to "LAST_MANGA_UPDATE",
            "UNREAD" to "UNREAD_COUNT",
            "DATE_FETCHED" to "CHAPTER_FETCH_DATE",
            "DRAG_AND_DROP" to "ALPHABETICAL",
            "LAST_READ" to "LAST_READ",
        )
        startAll()
        for ((old, new) in renamed) {
            prefs.edit { putString(key, old) }
            migration(migrationContext()) shouldBe true
            prefs.getString(key, null) shouldBe new
        }
    }

    @Test
    fun defaultsToAlphabetical() = runTest {
        stubCategories(emptyList())
        startAll()
        migration(migrationContext()) shouldBe true
        prefs.getString(key, null) shouldBe "ALPHABETICAL"
    }

    @Test
    fun clearsDateAddedSortFlag() = runTest {
        stubCategories(
            listOf(
                Category(id = 1, name = "by date", order = 0, flags = 0b1100001, version = 2, uid = 3),
                Category(id = 2, name = "by name", order = 1, flags = 0b1000),
            ),
        )
        startAll()
        migration(migrationContext()) shouldBe true
        coVerify(exactly = 1) {
            queries.update(
                name = null,
                order = null,
                flags = 0b1000001,
                version = 2,
                uid = 3,
                last_modified_at = null,
                isSyncing = null,
                categoryId = 1,
            )
        }
        coVerify(exactly = 0) { queries.update(any(), any(), any(), any(), any(), any(), any(), categoryId = 2) }
    }

    private fun stubCategories(categories: List<Category>) {
        mockkStatic("tachiyomi.data.QueryExtensionKt")
        val query = mockk<Query<GetCategories>>()
        every { queries.getCategories() } returns query
        coEvery { query.awaitList<GetCategories, Category>(any()) } returns categories
        coEvery { queries.update(any(), any(), any(), any(), any(), any(), any(), any()) } returns 1L
        coEvery { database.transaction(any(), any()) } coAnswers {
            secondArg<suspend SuspendingTransactionWithoutReturn.() -> Unit>().invoke(mockk())
        }
    }

    private fun startAll() {
        startMigrationKoin {
            single { app }
            single { libraryPreferences }
            single { database }
        }
    }
}
