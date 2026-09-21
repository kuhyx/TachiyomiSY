package eu.kanade.tachiyomi.data.track.hikka.dto

import eu.kanade.tachiyomi.data.track.fixture
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

/** [HKMangaPagination] and its [HKPagination]. */
internal class HKMangaPaginationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun decodesTheSearchFixture() {
        val page = json.decodeFromString<HKMangaPagination>(
            fixture("eu/kanade/tachiyomi/data/track/hikka/manga_search.json"),
        )
        page.pagination shouldBe HKPagination(total = 2, pages = 1, page = 1)
        page.pagination.total shouldBe 2
        page.pagination.pages shouldBe 1
        page.pagination.page shouldBe 1
        page.list.map { it.slug } shouldBe listOf("first-slug", "second-slug")
        // A null `read` is omitted on encode and comes back as the empty-list default.
        val again = json.decodeFromString<HKMangaPagination>(json.encodeToString(page))
        again.pagination shouldBe page.pagination
        again.list[1].read shouldBe emptyList()
        HKMangaPagination(pagination = page.pagination, list = page.list) shouldBe page
    }
}
