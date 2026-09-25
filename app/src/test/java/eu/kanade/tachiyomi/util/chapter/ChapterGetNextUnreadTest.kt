package eu.kanade.tachiyomi.util.chapter

import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.ChapterList
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

internal class ChapterGetNextUnreadTest {

    private val downloadManager = mockk<DownloadManager>()
    private val manga = Manga.create().copy(id = 1, source = 7, chapterFlags = Manga.CHAPTER_SORTING_NUMBER)
    private val chapters = listOf(
        Chapter.create().copy(id = 1, mangaId = 1, chapterNumber = 1.0, read = true),
        Chapter.create().copy(id = 2, mangaId = 1, chapterNumber = 2.0),
        Chapter.create().copy(id = 3, mangaId = 1, chapterNumber = 3.0),
    )
    private val items = chapters.map {
        ChapterList.Item(
            chapter = it,
            downloadState = Download.State.NOT_DOWNLOADED,
            downloadProgress = 0,
            sourceName = null,
            showScanlator = false,
        )
    }

    @BeforeEach
    fun setUp() {
        startKoin { modules(module { single { BasePreferences(mockk(relaxed = true), InMemoryPreferenceStore()) } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun plainMangaTakeTheLowestUnread() {
        chapters.getNextUnread(manga, downloadManager, emptyMap())!!.id shouldBe 2L
        val ascending = manga.copy(chapterFlags = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_SORT_ASC)
        chapters.getNextUnread(ascending, downloadManager, emptyMap())!!.id shouldBe 2L
        items.getNextUnread(manga)!!.id shouldBe 2L
        items.getNextUnread(ascending)!!.id shouldBe 2L
        val allRead = items.map { it.copy(chapter = it.chapter.copy(read = true)) }
        allRead.getNextUnread(manga).shouldBeNull()
        allRead.getNextUnread(ascending).shouldBeNull()
        chapters.map { it.copy(read = true) }.getNextUnread(ascending, downloadManager, emptyMap()).shouldBeNull()
        emptyList<Chapter>().getNextUnread(ascending, downloadManager, emptyMap()).shouldBeNull()
    }

    @Test
    fun ehMangaOnlyOfferTheEdgeChapter() {
        val eh = manga.copy(source = EH_SOURCE_ID)
        chapters.getNextUnread(eh, downloadManager, emptyMap())!!.id shouldBe 3L
        val ascending = eh.copy(chapterFlags = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_SORT_ASC)
        chapters.getNextUnread(ascending, downloadManager, emptyMap())!!.id shouldBe 3L
        items.getNextUnread(eh)!!.id shouldBe 3L
        items.getNextUnread(ascending)!!.id shouldBe 3L
        val allRead = chapters.map { it.copy(read = true) }
        allRead.getNextUnread(eh, downloadManager, emptyMap()).shouldBeNull()
        allRead.getNextUnread(ascending, downloadManager, emptyMap()).shouldBeNull()
        allRead.getNextUnread(manga, downloadManager, emptyMap()).shouldBeNull()
        emptyList<ChapterList.Item>().getNextUnread(eh).shouldBeNull()
        emptyList<ChapterList.Item>().getNextUnread(ascending).shouldBeNull()
        emptyList<Chapter>().getNextUnread(eh, downloadManager, emptyMap()).shouldBeNull()
        emptyList<Chapter>().getNextUnread(ascending, downloadManager, emptyMap()).shouldBeNull()
        val allReadItems = items.map { it.copy(chapter = it.chapter.copy(read = true)) }
        allReadItems.getNextUnread(eh).shouldBeNull()
        allReadItems.getNextUnread(ascending).shouldBeNull()
    }

    @Test
    fun exhCountsAsEh() {
        val exh = manga.copy(source = EXH_SOURCE_ID)
        chapters.getNextUnread(exh, downloadManager, emptyMap())!!.id shouldBe 3L
        items.getNextUnread(exh)!!.id shouldBe 3L
        emptyList<Chapter>().getNextUnread(manga, downloadManager, emptyMap()).shouldBeNull()
        emptyList<ChapterList.Item>().getNextUnread(manga).shouldBeNull()
    }
}
