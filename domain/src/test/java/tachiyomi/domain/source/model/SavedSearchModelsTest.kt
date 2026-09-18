package tachiyomi.domain.source.model

import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class SavedSearchModelsTest {

    @Test
    fun savedSearchSurface() {
        val search = SavedSearch(id = 1L, source = 2L, name = "name", query = "q", filtersJson = "[]")

        search.component1() shouldBe 1L
        search.component2() shouldBe 2L
        search.component3() shouldBe "name"
        search.component4() shouldBe "q"
        search.component5() shouldBe "[]"
        search shouldBe search.copy()
        search.copy(query = null, filtersJson = null) shouldNotBe search
        search.hashCode() shouldBe search.copy().hashCode()
        search.toString() shouldBe "SavedSearch(id=1, source=2, name=name, query=q, filtersJson=[])"
    }

    @Test
    fun feedSavedSearchSurface() {
        val feed = FeedSavedSearch(id = 1L, source = 2L, savedSearch = 3L, global = true)

        feed.component1() shouldBe 1L
        feed.component2() shouldBe 2L
        feed.component3() shouldBe 3L
        feed.component4() shouldBe true
        feed shouldBe feed.copy()
        feed.copy(savedSearch = null, global = false) shouldNotBe feed
        feed.hashCode() shouldBe feed.copy().hashCode()
        feed.toString() shouldBe "FeedSavedSearch(id=1, source=2, savedSearch=3, global=true)"
    }

    @Test
    fun exhSavedSearchSurface() {
        val filters = FilterList()
        val search = EXHSavedSearch(id = 1L, name = "name", query = null, filterList = filters)

        search.id shouldBe 1L
        search.name shouldBe "name"
        search.component1() shouldBe 1L
        search.component2() shouldBe "name"
        search.component3() shouldBe null
        search.component4() shouldBe filters
        search.copy(filterList = null).filterList shouldBe null
        search.copy(query = "q").query shouldBe "q"
        search.hashCode() shouldBe search.copy().hashCode()
        search.toString() shouldBe "EXHSavedSearch(id=1, name=name, query=null, filterList=$filters)"
    }
}
