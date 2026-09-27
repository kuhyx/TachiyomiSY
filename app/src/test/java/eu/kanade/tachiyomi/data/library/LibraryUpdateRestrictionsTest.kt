package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateRestrictionsTest {

    private val harness = LibraryJobHarness()

    @Before
    fun setUp() {
        harness.start()
        every { harness.fetchInterval.getWindow(any()) } returns (0L to WINDOW_END)
    }

    @After
    fun tearDown() = harness.stop()

    private fun restrict(vararg restrictions: String) =
        harness.libraryPreferences.autoUpdateMangaRestrictions.set(restrictions.toSet())

    private fun apply(vararg entries: LibraryManga): Pair<List<Long>, List<Pair<Manga, String?>>> {
        val skipped = mutableListOf<Pair<Manga, String?>>()
        val kept = harness.job().applyUpdateRestrictions(entries.toList(), skipped)
        return kept.map { it.id } to skipped
    }

    private fun entry(id: Long, total: Long = 0, read: Long = 0, edit: (Manga) -> Manga = { it }) =
        libraryEntry(edit(libManga(id = id, title = "t${10 - id}")), total = total, read = read)

    @Test
    fun keptEntriesSortAndDedupe() {
        restrict()
        val (kept, skipped) = apply(entry(1), entry(2), entry(1), entry(3, total = 4, read = 0))
        kept shouldBe listOf(3L, 2L, 1L)
        skipped shouldBe emptyList()
    }

    @Test
    fun fetchOnceWithChaptersSkips() {
        restrict()
        val once: (Manga) -> Manga = { it.copy(updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE) }
        val (kept, skipped) = apply(entry(1, total = 1, edit = once), entry(2, edit = once))
        kept shouldBe listOf(2L)
        skipped.single().second shouldBe "Skipped because series does not require updates"
    }

    @Test
    fun completedSkipsWhenRestricted() {
        restrict(MANGA_NON_COMPLETED)
        val (kept, skipped) = apply(entry(1) { it.copy(ogStatus = 2) }, entry(2) { it.copy(ogStatus = 1) })
        kept shouldBe listOf(2L)
        skipped.single().second shouldBe "Skipped because series is complete"
    }

    @Test
    fun unreadSkipsWhenRestricted() {
        restrict(MANGA_HAS_UNREAD)
        val (kept, skipped) = apply(entry(1, total = 3, read = 1), entry(2, total = 3, read = 3))
        kept shouldBe listOf(2L)
        skipped.single().second shouldBe "Skipped because there are unread chapters"
    }

    @Test
    fun unstartedSkipsWhenRestricted() {
        restrict(MANGA_NON_READ)
        val (kept, skipped) = apply(entry(1, total = 3), entry(2, total = 3, read = 1), entry(3))
        kept shouldBe listOf(3L, 2L)
        skipped.single().second shouldBe "Skipped because no chapters are read"
    }

    @Test
    fun outsideWindowSkips() {
        restrict(MANGA_OUTSIDE_RELEASE_PERIOD)
        val (kept, skipped) = apply(
            entry(1) { it.copy(nextUpdate = WINDOW_END + 1) },
            entry(2) { it.copy(nextUpdate = WINDOW_END) },
        )
        kept shouldBe listOf(2L)
        skipped.single().second shouldBe "Skipped because no release was expected today"
    }

    private companion object {
        const val WINDOW_END = 1_000L
    }
}
