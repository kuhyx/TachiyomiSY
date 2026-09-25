package mihon.feature.migration.list.models

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import kotlin.coroutines.EmptyCoroutineContext

internal class MigratingMangaTest {
    private val manga = Manga.create().copy(id = 5L, ogTitle = "Source entry")

    private fun migrating(latestChapter: Double? = 12.0): MigratingManga = MigratingManga(
        manga = manga,
        chapterCount = 4,
        latestChapter = latestChapter,
        source = "Source name",
        parentContext = EmptyCoroutineContext,
    )

    @Test
    fun startsSearchingAndCancels() {
        val migrating = migrating()
        migrating.manga.id shouldBe 5L
        migrating.chapterCount shouldBe 4
        migrating.latestChapter shouldBe 12.0
        migrating.source shouldBe "Source name"
        migrating.searchResult.value shouldBe MigratingManga.SearchResult.Searching
        val job = migrating.migrationScope.coroutineContext[Job]
        job?.isActive shouldBe true
        migrating.cancelMigration()
        job?.isActive shouldBe false
        job?.isCancelled shouldBe true
    }

    @Test
    fun scopeInheritsTheParentContext() = runTest {
        val migrating = MigratingManga(
            manga = manga,
            chapterCount = 0,
            latestChapter = null,
            source = "Other",
            parentContext = coroutineContext,
        )
        migrating.latestChapter shouldBe null
        migrating.migrationScope.coroutineContext[Job] shouldBe
            migrating.migrationScope.coroutineContext[Job]
        migrating.cancelMigration()
    }

    @Test
    fun searchResults() {
        val success = MigratingManga.SearchResult.Success(
            manga = manga,
            chapterCount = 3,
            latestChapter = 7.5,
            source = "Target",
        )
        success.manga.id shouldBe 5L
        success.chapterCount shouldBe 3
        success.latestChapter shouldBe 7.5
        success.source shouldBe "Target"
        success.copy(chapterCount = 4).chapterCount shouldBe 4
        (
            success == MigratingManga.SearchResult.Success(
                manga = manga,
                chapterCount = 3,
                latestChapter = 7.5,
                source = "Target",
            )
            ) shouldBe true
        success.toString().contains("Success") shouldBe true
        MigratingManga.SearchResult.NotFound.toString() shouldBe "NotFound"
        MigratingManga.SearchResult.Searching.toString() shouldBe "Searching"
    }
}
