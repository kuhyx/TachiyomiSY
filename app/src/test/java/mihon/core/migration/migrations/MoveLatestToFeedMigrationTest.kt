package mihon.core.migration.migrations

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.interactor.InsertFeedSavedSearch
import tachiyomi.domain.source.interactor.InsertSavedSearch
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch

@RunWith(RobolectricTestRunner::class)
internal class MoveLatestToFeedMigrationTest {

    private val migration = MoveLatestToFeedMigration()
    private val app = robolectricApp()
    private val prefs = defaultPrefs(app)
    private val insertSavedSearch = mockk<InsertSavedSearch>()
    private val insertFeedSavedSearch = mockk<InsertFeedSavedSearch>()

    @After
    fun tearDown() {
        stopMigrationKoin()
        unmockkAll()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 31f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin {
            single { app }
            single { insertSavedSearch }
        }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin {
            single { app }
            single { insertFeedSavedSearch }
        }
        migration(migrationContext()) shouldBe false
        stopMigrationKoin()
        startMigrationKoin {
            single { insertSavedSearch }
            single { insertFeedSavedSearch }
        }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun insertsNothingWhenEmpty() = runTest {
        startAll(app)
        migration(migrationContext()) shouldBe true
        coVerify(exactly = 0) { insertSavedSearch.awaitAll(any()) }
        coVerify(exactly = 0) { insertFeedSavedSearch.awaitAll(any()) }
    }

    @Test
    fun copesWithNullSets() = runTest {
        val nullPrefs = mockk<SharedPreferences>(relaxed = true) {
            every { getStringSet(any(), any()) } returns null
        }
        val mockApp = mockk<Application>()
        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(mockApp) } returns nullPrefs
        startAll(mockApp)
        migration(migrationContext()) shouldBe true
        coVerify(exactly = 0) { insertSavedSearch.awaitAll(any()) }
        coVerify(exactly = 0) { insertFeedSavedSearch.awaitAll(any()) }
    }

    @Test
    fun movesSearchesAndLatestSources() = runTest {
        prefs.edit {
            putStringSet(
                "eh_saved_searches",
                setOf(
                    """5:{"name":"tags","query":"cat","filters":[1]}""",
                    """6:{"name":"blank","query":"","filters":[]}""",
                    """7:{"name":"nullq","query":null,"filters":[]}""",
                    """8:{"query":"no name","filters":[]}""",
                    "9:not json",
                    "x:{}",
                ),
            )
            putStringSet("latest_tab_sources", setOf("11"))
        }
        val searches = slot<List<SavedSearch>>()
        coEvery { insertSavedSearch.awaitAll(capture(searches)) } returns Unit
        val feeds = slot<List<FeedSavedSearch>>()
        coEvery { insertFeedSavedSearch.awaitAll(capture(feeds)) } returns Unit
        startAll(app)
        migration(migrationContext()) shouldBe true
        searches.captured.sortedBy { it.source } shouldBe listOf(
            SavedSearch(id = -1, source = 5, name = "tags", query = "cat", filtersJson = "[1]"),
            SavedSearch(id = -1, source = 6, name = "blank", query = null, filtersJson = "[]"),
            SavedSearch(id = -1, source = 7, name = "nullq", query = null, filtersJson = "[]"),
        )
        feeds.captured shouldBe listOf(FeedSavedSearch(id = -1, source = 11, savedSearch = null, global = true))
        prefs.contains("eh_saved_searches") shouldBe false
        prefs.contains("latest_tab_sources") shouldBe false
    }

    private fun startAll(application: Application) {
        startMigrationKoin {
            single { application }
            single { insertSavedSearch }
            single { insertFeedSavedSearch }
        }
    }
}
