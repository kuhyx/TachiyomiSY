package tachiyomi.data.manga

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import tachiyomi.data.manga.CustomMangaRepositoryImpl.MangaJson
import tachiyomi.data.manga.CustomMangaRepositoryImpl.MangaList

/** The two `edits.json` models: data-class contracts and the JSON they read and write. */
internal class CustomMangaModelTest {
    private val full = MangaJson(
        id = 1L, title = "t", author = "au", artist = "ar", thumbnailUrl = "th", description = "d",
        genre = listOf("g"), status = 2L,
    )
    private val fullJson = """{"id":1,"title":"t","author":"au","artist":"ar","thumbnailUrl":"th",""" +
        """"description":"d","genre":["g"],"status":2}"""

    @Test
    fun mangaJsonEqualsAndHashCode() {
        val same = full.copy()

        same shouldBe full
        same.hashCode() shouldBe full.hashCode()
        full shouldNotBe MangaJson()
        full shouldNotBe full.copy(title = "other")
        full shouldNotBe full.copy(genre = null)
        MangaJson() shouldBe MangaJson()
        MangaJson().hashCode() shouldBe MangaJson().hashCode()
        full shouldNotBe "text"
    }

    @Test
    fun mangaJsonToString() {
        full.toString() shouldBe "MangaJson(id=1, title=t, author=au, artist=ar, thumbnailUrl=th, " +
            "description=d, genre=[g], status=2)"
        MangaJson().toString().contains("id=null") shouldBe true
    }

    @Test
    fun mangaJsonCopyAndComponents() {
        val copy = full.copy(id = 9L, status = null)
        val (id, title, author) = copy

        id shouldBe 9L
        title shouldBe "t"
        author shouldBe "au"
        copy.status shouldBe null
        copy.genre shouldBe listOf("g")
    }

    @Test
    fun mangaJsonEncodesFull() {
        Json.encodeToString(MangaJson.serializer(), full) shouldBe fullJson
    }

    @Test
    fun mangaJsonDecodesJson() {
        Json.decodeFromString(MangaJson.serializer(), fullJson) shouldBe full
        Json.decodeFromString(MangaJson.serializer(), "{}") shouldBe MangaJson()
        Json.decodeFromString(MangaJson.serializer(), """{"id":3,"genre":null}""") shouldBe MangaJson(id = 3L)
    }

    @Test
    fun mangaListEqualsAndHashCode() {
        val list = MangaList(listOf(full))

        list shouldBe MangaList(listOf(full))
        list.hashCode() shouldBe MangaList(listOf(full)).hashCode()
        list shouldNotBe MangaList()
        MangaList() shouldBe MangaList(null)
        MangaList().hashCode() shouldBe MangaList(null).hashCode()
        list shouldNotBe "text"
    }

    @Test
    fun mangaListToStringAndCopy() {
        val list = MangaList(listOf(full))

        list.toString() shouldBe "MangaList(mangas=[$full])"
        list.copy(mangas = emptyList()).mangas shouldBe emptyList()
        list.copy().component1() shouldBe listOf(full)
    }

    @Test
    fun mangaListEncodesAndDecodes() {
        val encoded = Json.encodeToString(MangaList.serializer(), MangaList(listOf(full)))

        encoded shouldBe """{"mangas":[$fullJson]}"""
        Json.decodeFromString(MangaList.serializer(), encoded) shouldBe MangaList(listOf(full))
        Json.decodeFromString(MangaList.serializer(), "{}") shouldBe MangaList()
        Json.decodeFromString(MangaList.serializer(), """{"mangas":null}""") shouldBe MangaList()
        Json.decodeFromString(MangaList.serializer(), """{"mangas":[{}]}""") shouldBe MangaList(listOf(MangaJson()))
    }
}
