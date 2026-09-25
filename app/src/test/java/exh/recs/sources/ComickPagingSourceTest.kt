package exh.recs.sources

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.CannedServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.data.source.NoResultsException
import tachiyomi.i18n.sy.SYMR

private const val RECS = """{"comic":{"recommendations":[""" +
    """{"relates":{"title":"Rec One","hid":"h1","md_covers":[{"b2key":"c1.jpg"},{"b2key":"c2.jpg"}]}}]}}"""

internal class ComickPagingSourceTest {
    private val server = CannedServer()
    private val stub = RecsStub(server.client)
    private val comick = mockk<Source> {
        every { id } returns 77L
        every { name } returns "Comick"
    }
    private val source = ComickPagingSource(sourceManga(), comick)

    @AfterEach
    fun tearDown() = stub.uninstall()

    @Test
    fun identity() {
        source.name shouldBe "Comick"
        source.category shouldBe SYMR.strings.community_recommendations
        source.associatedSourceId shouldBe 77L
        comick.isComickSource() shouldBe true
        val other = mockk<Source> { every { name } returns "Other" }
        other.isComickSource() shouldBe false
    }

    @Test
    fun recommendationsFromComic() {
        server.body = RECS
        val page = runBlocking { source.requestNextPage(1) }
        server.request().url.toString() shouldBe "https://api.comick.fun/v1.0/comic/hid?tachiyomi=true#"
        server.request().header("Referer") shouldBe "api.comick.fun/"
        server.request().header("User-Agent")?.startsWith("Tachiyomi ") shouldBe true
        page.hasNextPage shouldBe false
        page.mangas.map { it.title } shouldContainExactly listOf("Rec One")
        page.mangas.single().url shouldBe "/comic/h1#"
        page.mangas.single().thumbnail_url shouldBe "https://meo.comick.pictures/c1.jpg"
        page.mangas.single().initialized shouldBe false
    }

    @Test
    fun noRecommendations() {
        server.body = """{"comic":{"recommendations":[]}}"""
        shouldThrow<NoResultsException> { runBlocking { source.requestNextPage(1) } }
    }
}
