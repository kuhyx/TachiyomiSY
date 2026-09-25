package exh.md.handlers

import eu.kanade.tachiyomi.source.online.CannedServer
import eu.kanade.tachiyomi.source.online.sManga
import exh.md.dto.SAMPLE_DATA_JSON
import exh.md.service.MangaDexService
import exh.md.service.SimilarService
import exh.md.utils.MangaDexRelation
import exh.metadata.metadata.MangaDexSearchMetadata
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import org.junit.jupiter.api.Test

internal class SimilarHandlerTest {
    private val server = CannedServer()
    private val handler =
        SimilarHandler("en", MangaDexService(server.client, headersOf()), SimilarService(server.client))

    @Test
    fun similarMangas() {
        server.answers = { request ->
            if (request.url.host == "api.similarmanga.com") {
                """{"id":"m0","title":{"en":"Root"},"contentRating":"safe","updatedAt":"now","matches":[""" +
                    """{"id":"m1","title":{"en":"One"},"contentRating":"safe","score":0.9}]}"""
            } else {
                """{"limit":1,"offset":0,"total":1,"data":[$SAMPLE_DATA_JSON]}"""
            }
        }
        val page = runBlocking { handler.getSimilar(sManga("/manga/m0")) }
        server.request(0).url.toString() shouldBe "https://api.similarmanga.com/similar/m0.json"
        server.request(1).url.queryParameterValues("ids[]") shouldBe listOf("m1")
        page.mangas.map { it.title } shouldContainExactly listOf("Title")
        page.hasNextPage shouldBe false
        (page.mangasMetadata.single() as MangaDexSearchMetadata).relation shouldBe MangaDexRelation.SIMILAR
    }

    @Test
    fun relatedMangas() {
        server.answers = { request ->
            if (request.url.encodedPath.endsWith("/relation")) {
                """{"response":"collection","data":[""" +
                    """{"attributes":{"relation":"sequel"},"relationships":[{"id":"m1"}]},""" +
                    """{"attributes":{"relation":"prequel"},"relationships":[]}]}"""
            } else {
                """{"limit":1,"offset":0,"total":1,"data":[$SAMPLE_DATA_JSON]}"""
            }
        }
        val page = runBlocking { handler.getRelated(sManga("/manga/m0")) }
        server.request(1).url.queryParameterValues("ids[]") shouldBe listOf("m1")
        page.mangas.map { it.url } shouldContainExactly listOf("/manga/m1")
        (page.mangasMetadata.single() as MangaDexSearchMetadata).relation shouldBe MangaDexRelation.SEQUEL
    }

    @Test
    fun relatedWithoutMatchingRelation() {
        server.answers = { request ->
            if (request.url.encodedPath.endsWith("/relation")) {
                """{"response":"collection","data":[""" +
                    """{"attributes":{"relation":"sequel"},"relationships":[{"id":"m9"}]}]}"""
            } else {
                """{"limit":1,"offset":0,"total":1,"data":[$SAMPLE_DATA_JSON]}"""
            }
        }
        val page = runBlocking { handler.getRelated(sManga("/manga/m0")) }
        (page.mangasMetadata.single() as MangaDexSearchMetadata).relation shouldBe null
    }
}
