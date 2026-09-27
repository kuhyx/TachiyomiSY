package eu.kanade.tachiyomi.ui.browse.migration.advanced.process

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class MigratingMangaTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun latestChapterIsFormatted() {
        MigratingManga.ChapterInfo(latestChapter = 3.5, chapterCount = 4)
            .getFormattedLatestChapter(context) shouldBe "Latest: 3.5"
        MigratingManga.ChapterInfo(latestChapter = 0.0, chapterCount = 0)
            .getFormattedLatestChapter(context) shouldBe "Latest: Unknown"
        MigratingManga.ChapterInfo(latestChapter = null, chapterCount = 0)
            .getFormattedLatestChapter(context) shouldBe "Latest: Unknown"
    }

    @Test
    fun startsSearching() {
        val migrating = MigratingManga(
            manga = Manga.create(),
            chapterInfo = MigratingManga.ChapterInfo(latestChapter = null, chapterCount = 0),
            sourcesString = "A, B",
            parentContext = Dispatchers.Unconfined,
        )
        migrating.searchResult.value shouldBe MigratingManga.SearchResult.Searching
        migrating.progress.value shouldBe (1 to 0)
        migrating.sourcesString shouldBe "A, B"
        migrating.migrationScope.cancel()
    }

    @Test
    fun resultsAreValues() {
        val found = MigratingManga.SearchResult.Result(4L)
        found shouldBe MigratingManga.SearchResult.Result(4L)
        found.id shouldBe 4L
        MigratingManga.SearchResult.NotFound.toString() shouldBe "NotFound"
        (MigratingManga.SearchResult.NotFound == MigratingManga.SearchResult.Searching) shouldBe false
    }
}
