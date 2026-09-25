package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class DownloadPendingDeleterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val one = Manga.create().copy(id = 1L, url = "/m1", ogTitle = "One", source = 4L)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val two = Manga.create().copy(id = 2L, url = "/m2", ogTitle = "Two", source = 4L)

    private fun chapter(id: Long) = Chapter.create().copy(id = id, url = "/c$id", name = "Ch $id", scanlator = "s")

    private fun deleter() = DownloadPendingDeleter(context = context, json = json)

    private fun pendingIds(deleter: DownloadPendingDeleter): Map<Long, List<Long>> =
        deleter.getPendingChapters().entries.associate { (manga, chapters) -> manga.id to chapters.map { it.id } }

    @Before
    fun setUp() {
        context.getSharedPreferences("chapters_to_delete", Context.MODE_PRIVATE).edit { clear() }
    }

    @Test
    fun chaptersAreGroupedByManga() {
        val deleter = deleter()
        deleter.addChapters(listOf(chapter(1L)), one)
        deleter.addChapters(listOf(chapter(2L), chapter(1L)), one)
        deleter.addChapters(listOf(chapter(3L)), two)
        pendingIds(deleter) shouldBe mapOf(1L to listOf(1L, 2L), 2L to listOf(3L))
        pendingIds(deleter) shouldBe emptyMap()
    }

    @Test
    fun modelsKeepTheBasics() {
        val deleter = deleter()
        deleter.addChapters(listOf(chapter(1L)), one)
        val (manga, chapters) = deleter.getPendingChapters().entries.single()
        listOf(manga.url, manga.ogTitle, manga.source) shouldBe listOf("/m1", "One", 4L)
        listOf(chapters.single().url, chapters.single().name, chapters.single().scanlator) shouldBe
            listOf("/c1", "Ch 1", "s")
    }

    @Test
    fun repeatedChaptersAreIgnored() {
        val deleter = deleter()
        deleter.addChapters(listOf(chapter(1L)), one)
        deleter.addChapters(listOf(chapter(1L)), one)
        pendingIds(deleter) shouldBe mapOf(1L to listOf(1L))
    }

    @Test
    fun savedEntriesAreExtended() {
        deleter().addChapters(listOf(chapter(1L)), one)
        val fresh = deleter()
        fresh.addChapters(listOf(chapter(1L)), one)
        fresh.addChapters(listOf(chapter(2L)), one)
        pendingIds(fresh) shouldBe mapOf(1L to listOf(1L, 2L))
    }

    @Test
    fun brokenEntriesAreSkipped() {
        val deleter = deleter()
        deleter.addChapters(listOf(chapter(1L)), one)
        context.getSharedPreferences("chapters_to_delete", Context.MODE_PRIVATE).edit {
            putString("99", "{broken")
            putInt("98", 1)
        }
        pendingIds(deleter) shouldBe mapOf(1L to listOf(1L))
    }

    /** Entries written before the scanlator was stored still decode, without one. */
    @Test
    fun oldEntriesHaveNoScanlator() {
        val legacy = """{"chapters":[{"id":5,"url":"/c5","name":"Ch 5"}],""" +
            """"manga":{"id":2,"url":"/m2","title":"Two","source":4}}"""
        context.getSharedPreferences("chapters_to_delete", Context.MODE_PRIVATE).edit { putString("2", legacy) }
        deleter().getPendingChapters().values.single().single().scanlator shouldBe null
    }

    @Test
    fun defaultJsonComesFromInjekt() {
        startKoin { modules(module { single { json } }) }
        try {
            DownloadPendingDeleter(context).getPendingChapters() shouldBe emptyMap()
        } finally {
            stopKoin()
        }
    }
}
