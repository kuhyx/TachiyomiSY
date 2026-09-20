package mihon.core.migration.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import exh.util.nullIfBlank
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.source.interactor.InsertFeedSavedSearch
import tachiyomi.domain.source.interactor.InsertSavedSearch
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch

private const val VERSION = 31f

internal class MoveLatestToFeedMigration : Migration {
    override val version: Float = VERSION

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>()
        val insertSavedSearch = migrationContext.get<InsertSavedSearch>()
        val insertFeedSavedSearch = migrationContext.get<InsertFeedSavedSearch>()
        if (context == null || insertSavedSearch == null || insertFeedSavedSearch == null) return false
        withIOContext { migrate(context, insertSavedSearch, insertFeedSavedSearch) }
        return true
    }

    private suspend fun migrate(
        context: Application,
        insertSavedSearch: InsertSavedSearch,
        insertFeedSavedSearch: InsertFeedSavedSearch,
    ) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val savedSearch = prefs.getStringSet("eh_saved_searches", emptySet())?.mapNotNull { parseSavedSearch(it) }
        if (!savedSearch.isNullOrEmpty()) {
            insertSavedSearch.awaitAll(savedSearch)
        }
        val feedSavedSearch = prefs.getStringSet("latest_tab_sources", emptySet())?.map {
            FeedSavedSearch(
                id = -1,
                source = it.toLong(),
                savedSearch = null,
                global = true,
            )
        }
        if (!feedSavedSearch.isNullOrEmpty()) {
            insertFeedSavedSearch.awaitAll(feedSavedSearch)
        }
        prefs.edit(commit = true) {
            remove("eh_saved_searches")
            remove("latest_tab_sources")
        }
    }

    // "<sourceId>:<json>" as the old preference stored it; null when either half is unreadable.
    private fun parseSavedSearch(entry: String): SavedSearch? {
        val source = entry.substringBefore(':').toLongOrNull() ?: return null
        return runCatching {
            val content = Json.decodeFromString<JsonObject>(entry.substringAfter(':'))
            SavedSearch(
                id = -1,
                source = source,
                name = content["name"]!!.jsonPrimitive.content,
                query = content["query"]!!.jsonPrimitive.contentOrNull?.nullIfBlank(),
                filtersJson = Json.encodeToString(content["filters"]!!.jsonArray),
            )
        }.getOrNull()
    }
}
