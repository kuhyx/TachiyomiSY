package eu.kanade.tachiyomi.source.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/** JSON round trips of [Page] and the identity of its [Page.State] objects. */
internal class PageSerializationTest {
    private val json = Json

    private val encodeDefaults = Json { encodeDefaults = true }

    @Test
    fun encodesOnlyNonDefaults() {
        json.encodeToString(Page.serializer(), Page(2)) shouldBe """{"index":2}"""
    }

    @Test
    fun encodesEveryField() {
        val page = Page(index = 2, url = "u", imageUrl = "i", uri = null)
        json.encodeToString(Page.serializer(), page) shouldBe """{"index":2,"url":"u","imageUrl":"i"}"""
    }

    @Test
    fun encodesDefaultsOnDemand() {
        val encoded = encodeDefaults.encodeToString(Page.serializer(), Page(0))
        encoded shouldContain """"url":"""""
        encoded shouldContain """"imageUrl":null"""
        encoded shouldNotBe json.encodeToString(Page.serializer(), Page(0))
    }

    @Test
    fun decodesFullJson() {
        val page = json.decodeFromString(Page.serializer(), """{"index":1,"url":"u","imageUrl":"i"}""")
        page.index shouldBe 1
        page.url shouldBe "u"
        page.imageUrl shouldBe "i"
        page.uri shouldBe null
        page.status shouldBe Page.State.Queue
        page.progress shouldBe 0
    }

    @Test
    fun decodesMinimalJson() {
        val page = json.decodeFromString(Page.serializer(), """{"index":4}""")
        page.index shouldBe 4
        page.url shouldBe ""
        page.imageUrl shouldBe null
        page.number shouldBe 5
    }

    @Test
    fun decodesNullImage() {
        val page = json.decodeFromString(Page.serializer(), """{"index":0,"url":"u","imageUrl":null}""")
        page.imageUrl shouldBe null
        page.url shouldBe "u"
    }

    @Test
    fun roundTripKeepsFields() {
        val page = Page(7, "u", "i")
        val decoded = json.decodeFromString(Page.serializer(), json.encodeToString(Page.serializer(), page))
        decoded.index shouldBe page.index
        decoded.url shouldBe page.url
        decoded.imageUrl shouldBe page.imageUrl
    }

    @Test
    fun stateObjectsAreSingletons() {
        val states: List<Page.State> = listOf(
            Page.State.Queue,
            Page.State.LoadPage,
            Page.State.DownloadImage,
            Page.State.Ready,
        )
        states.map { it.toString() } shouldBe listOf("Queue", "LoadPage", "DownloadImage", "Ready")
        states.map { it.hashCode() }.toSet().size shouldBe states.size
        states.forEachIndexed { index, state ->
            states.forEachIndexed { other, candidate ->
                (state == candidate) shouldBe (index == other)
            }
        }
    }
}
