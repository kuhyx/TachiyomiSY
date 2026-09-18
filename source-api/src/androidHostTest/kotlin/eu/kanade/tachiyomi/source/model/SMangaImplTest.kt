package eu.kanade.tachiyomi.source.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

internal class SMangaImplTest {
    @Test
    fun urlIsLateinit() {
        val manga = SMangaImpl()
        shouldThrow<UninitializedPropertyAccessException> { manga.url }
        manga.url = "/u"
        manga.url shouldBe "/u"
    }

    @Test
    fun defaults() {
        val manga = SMangaImpl()
        manga.title shouldBe ""
        manga.thumbnail_url shouldBe null
        manga.artist shouldBe null
        manga.author shouldBe null
        manga.status shouldBe 0
        manga.description shouldBe null
        manga.genre shouldBe null
        manga.update_strategy shouldBe UpdateStrategy.ALWAYS_UPDATE
        manga.initialized shouldBe false
        manga.memo shouldBe JsonObject(emptyMap())
    }

    @Test
    fun settersStoreValues() {
        val manga = SMangaImpl()
        manga.title = "T"
        manga.thumbnail_url = "cover"
        manga.artist = "Ar"
        manga.author = "Au"
        manga.status = SManga.LICENSED
        manga.description = "D"
        manga.genre = "G"
        manga.update_strategy = UpdateStrategy.ONLY_FETCH_ONCE
        manga.initialized = true
        manga.memo = buildJsonObject { put("k", "v") }

        manga.title shouldBe "T"
        manga.thumbnail_url shouldBe "cover"
        manga.artist shouldBe "Ar"
        manga.author shouldBe "Au"
        manga.status shouldBe SManga.LICENSED
        manga.description shouldBe "D"
        manga.genre shouldBe "G"
        manga.update_strategy shouldBe UpdateStrategy.ONLY_FETCH_ONCE
        manga.initialized shouldBe true
        manga.memo shouldBe buildJsonObject { put("k", "v") }
    }

    @Test
    fun originalsTrackCurrentValues() {
        val manga = SMangaImpl()
        manga.originalTitle shouldBe ""
        manga.originalAuthor shouldBe null
        manga.originalArtist shouldBe null
        manga.originalThumbnailUrl shouldBe null
        manga.originalDescription shouldBe null
        manga.originalGenre shouldBe null
        manga.originalStatus shouldBe 0

        manga.title = "T"
        manga.author = "Au"
        manga.artist = "Ar"
        manga.thumbnail_url = "cover"
        manga.description = "D"
        manga.genre = "G"
        manga.status = SManga.ONGOING

        manga.originalTitle shouldBe "T"
        manga.originalAuthor shouldBe "Au"
        manga.originalArtist shouldBe "Ar"
        manga.originalThumbnailUrl shouldBe "cover"
        manga.originalDescription shouldBe "D"
        manga.originalGenre shouldBe "G"
        manga.originalStatus shouldBe SManga.ONGOING
    }

    @Test
    fun createReturnsImpl() {
        SManga.create().shouldBeInstanceOf<SMangaImpl>()
    }

    @Test
    fun updateHoldsMangaAndChapters() {
        val manga = SManga("/u", "t")
        val chapter = SChapter(name = "c1", url = "/c1")
        val update = SMangaUpdate(manga, listOf(chapter))
        update.manga shouldBe manga
        update.chapters shouldBe listOf(chapter)
        update.component1() shouldBe manga
        update.component2() shouldBe listOf(chapter)
    }

    @Test
    fun updateIsDataClass() {
        val manga = SManga("/u", "t")
        val update = SMangaUpdate(manga, emptyList())
        update shouldBe SMangaUpdate(manga, emptyList())
        update.hashCode() shouldBe SMangaUpdate(manga, emptyList()).hashCode()
        update shouldNotBe SMangaUpdate(SManga("/v", "t"), emptyList())
        update.copy(chapters = listOf(SChapter(name = "c", url = "/c"))).chapters.size shouldBe 1
        update.copy() shouldBe update
        update.toString() shouldBe "SMangaUpdate(manga=$manga, chapters=[])"
    }
}
