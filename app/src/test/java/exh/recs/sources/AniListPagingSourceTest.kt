package exh.recs.sources

import eu.kanade.tachiyomi.source.online.CannedServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.data.source.NoResultsException

private fun media(title: String, synonyms: String = "[]", country: String = "JP"): String =
    """{"countryOfOrigin":"$country","siteUrl":"https://anilist.co/manga/1","title":$title,""" +
        """"synonyms":$synonyms,"coverImage":{"large":"https://img/1.jpg"}}"""

private fun page(vararg entries: String): String =
    """{"data":{"Page":{"media":[${entries.joinToString(",")}]}}}"""

private fun withRecs(self: String, rec: String): String =
    self.dropLast(1) + ""","recommendations":{"edges":[{"node":{"mediaRecommendation":$rec}}]}}"""

internal class AniListPagingSourceTest {
    private val server = CannedServer()
    private val stub = RecsStub(server.client)
    private val source = AniListPagingSource(sourceManga(title = "Needle"))

    @AfterEach
    fun tearDown() = stub.uninstall()

    private fun body(): String = Buffer().also { server.request().body?.writeTo(it) }.readUtf8()

    @Test
    fun identity() {
        source.name shouldBe "AniList"
        source.category shouldBe tachiyomi.i18n.sy.SYMR.strings.community_recommendations
        source.associatedTrackerId shouldBe stub.trackerManager.aniList.id
        source.associatedSourceId shouldBe null
    }

    @Test
    fun recsByIdPreferEnglishTitle() {
        val rec = media("""{"english":"English","romaji":"Romaji","native":"Native"}""")
        server.body = page(withRecs(media("""{"english":"Self"}"""), rec))
        val recs = runBlocking { source.getRecsById("77") }
        recs.single().title shouldBe "English"
        recs.single().url shouldBe "https://anilist.co/manga/1"
        recs.single().thumbnail_url shouldBe "https://img/1.jpg"
        recs.single().initialized shouldBe true
        body().contains(""""variables":{"id":"77"}""") shouldBe true
        server.request().url.toString() shouldBe "https://graphql.anilist.co/"
    }

    @Test
    fun titleFallbacks() {
        val japanese = media("""{"english":null,"romaji":"Romaji","native":"Native"}""")
        server.body = page(withRecs(media("""{"english":"Self"}"""), japanese))
        runBlocking { source.getRecsById("1") }.single().title shouldBe "Romaji"
        val synonym = media("""{"english":"","romaji":null,"native":"Native"}""", synonyms = """["Synonym"]""")
        server.body = page(withRecs(media("""{"english":"Self"}"""), synonym))
        runBlocking { source.getRecsById("1") }.single().title shouldBe "Synonym"
        val foreign = media("""{"english":null,"romaji":"Romaji","native":"Native"}""", country = "KR")
        server.body = page(withRecs(media("""{"english":"Self"}"""), foreign))
        runBlocking { source.getRecsById("1") }.single().title shouldBe "Romaji"
        val nativeOnly = media("""{"english":null,"romaji":null,"native":"Native"}""", country = "KR")
        server.body = page(withRecs(media("""{"english":"Self"}"""), nativeOnly))
        runBlocking { source.getRecsById("1") }.single().title shouldBe "Native"
        val nameless = media("""{"english":null,"romaji":null,"native":null}""", country = "KR")
        server.body = page(withRecs(media("""{"english":"Self"}"""), nameless))
        runBlocking { source.getRecsById("1") }.single().title shouldBe "NO NAME FOUND"
    }

    @Test
    fun titleWithAbsentFields() {
        // A media object that omits the title keys and the country entirely, not just nulls them.
        val sparse = """{"siteUrl":"https://anilist.co/manga/2","title":{},"synonyms":["Only Synonym"],""" +
            """"coverImage":{"large":"https://img/2.jpg"}}"""
        server.body = page(withRecs(media("""{"english":"Self"}"""), sparse))
        runBlocking { source.getRecsById("1") }.single().title shouldBe "Only Synonym"
        val blankRomaji = media("""{"english":"","romaji":"  ","native":"Native"}""", country = "JP")
        server.body = page(withRecs(media("""{"english":"Self"}"""), blankRomaji))
        runBlocking { source.getRecsById("1") }.single().title shouldBe "Native"
        val noRomaji = """{"countryOfOrigin":"JP","siteUrl":"https://anilist.co/manga/3","title":{"native":"N"},""" +
            """"synonyms":["Syn"],"coverImage":{"large":"https://img/3.jpg"}}"""
        server.body = page(withRecs(media("""{"english":"Self"}"""), noRomaji))
        runBlocking { source.getRecsById("1") }.single().title shouldBe "Syn"
        val blankSynonym = media("""{"english":null,"romaji":null,"native":"Native"}""", synonyms = """[""]""")
        server.body = page(withRecs(media("""{"english":"Self"}"""), blankSynonym))
        runBlocking { source.getRecsById("1") }.single().title shouldBe "Native"
    }

    @Test
    fun emptyPageHasNoResults() {
        server.body = page()
        shouldThrow<NoResultsException> { runBlocking { source.getRecsById("1") } }
    }

    @Test
    fun searchFiltersOnTitles() {
        val rec = media("""{"english":"Rec"}""")
        server.body = page(
            withRecs(media("""{"english":"a Needle b","romaji":null,"native":null}"""), rec),
            withRecs(media("""{"english":null,"romaji":"needle romaji","native":null}"""), rec),
            withRecs(media("""{"english":null,"romaji":null,"native":"ニードルNeedle"}"""), rec),
            withRecs(media("""{"english":null,"romaji":null,"native":null}""", synonyms = """["needle alt"]"""), rec),
            withRecs(media("""{"english":"other","romaji":"other","native":"other"}"""), rec),
        )
        runBlocking { source.getRecsBySearch("Needle") }.map { it.title } shouldContainExactly
            List(4) { "Rec" }
        body().contains(""""variables":{"search":"Needle"}""") shouldBe true
    }

    @Test
    fun searchWithoutMatches() {
        val rec = media("""{"english":"Rec"}""")
        server.body = page(withRecs(media("""{"english":"other","romaji":null,"native":null}"""), rec))
        runBlocking { source.getRecsBySearch("Needle") }.isEmpty() shouldBe true
    }

    @Test
    fun requestNextPageUsesTracker() {
        val rec = media("""{"english":"Rec"}""")
        server.body = page(withRecs(media("""{"english":"Self"}"""), rec))
        stub.tracks = listOf(track(trackerId = 9_999L), track(trackerId = stub.trackerManager.aniList.id))
        runBlocking { source.requestNextPage(1) }.mangas.single().title shouldBe "Rec"
        body().contains(""""variables":{"id":"42"}""") shouldBe true
    }

    @Test
    fun requestNextPageSearches() {
        val rec = media("""{"english":"Rec"}""")
        server.body = page(withRecs(media("""{"english":"Needle"}"""), rec))
        stub.tracks = listOf(track(trackerId = 9_999L))
        runBlocking { source.requestNextPage(1) }.mangas.single().title shouldBe "Rec"
        body().contains(""""variables":{"search":"Needle"}""") shouldBe true
    }

    @Test
    fun requestNextPageErrors() {
        server.body = page()
        shouldThrow<NoResultsException> { runBlocking { source.requestNextPage(1) } }
        server.body = "not json"
        shouldThrow<Exception> { runBlocking { source.requestNextPage(1) } }
    }
}
