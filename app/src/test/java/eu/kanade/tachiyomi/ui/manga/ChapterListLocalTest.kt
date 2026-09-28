package eu.kanade.tachiyomi.ui.manga

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.base.BasePreferences
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalSource

/** The downloaded filter treats every chapter of a local entry as downloaded. */
internal class ChapterListLocalTest {
    @BeforeEach
    fun setUp() {
        startKoin { modules(module { single { BasePreferences(mockk(relaxed = true), FlowPreferenceStore()) } }) }
    }

    @AfterEach
    fun tearDown() = stopKoin()

    @Test
    fun localChaptersPassDownloaded() {
        val local = manga(flags = Manga.CHAPTER_SHOW_DOWNLOADED, source = LocalSource.ID)
        listOf(item(chapter(1L))).applyFilters(local).map { it.id }.toList() shouldContainExactly listOf(1L)
    }
}
