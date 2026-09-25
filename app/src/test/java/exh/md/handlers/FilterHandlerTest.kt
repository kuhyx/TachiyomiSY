package exh.md.handlers

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class FilterHandlerTest {
    private val handler = FilterHandler()

    private fun boxes(filters: FilterList, name: String): List<Filter.CheckBox> =
        (filters.first { it.name == name } as Filter.Group<*>).state.filterIsInstance<Filter.CheckBox>()

    private fun select(filters: FilterList, name: String): Filter.Select<*> =
        filters.first { it.name == name } as Filter.Select<*>

    @Test
    fun filterListLayout() {
        val filters = handler.getMDFilterList()
        filters.map { it.name } shouldContainExactly listOf(
            "Original language",
            "Publication Demographic",
            "Content Rating",
            "Status",
            "Sort",
            "Tags",
            "Included tags mode",
            "Excluded tags mode",
        )
        boxes(filters, "Publication Demographic").map { it.name } shouldContainExactly
            listOf("None", "Shounen", "Shoujo", "Seinen", "Josei")
        boxes(filters, "Status").map { it.name } shouldContainExactly
            listOf("Ongoing", "Completed", "Hiatus", "Abandoned")
        boxes(filters, "Content Rating").map { it.name } shouldContainExactly
            listOf("Safe", "Suggestive", "Erotica", "Pornographic")
        boxes(filters, "Original language").map { it.name } shouldContainExactly
            listOf("Japanese (Manga)", "Chinese (Manhua)", "Korean (Manhwa)")
        select(filters, "Included tags mode").state shouldBe 0
        select(filters, "Excluded tags mode").state shouldBe 1
        handler.sortableList.map { it.first } shouldContainExactly
            listOf("Number of follows", "Created at", "Updated at")
    }

    @Test
    fun tagsMatchTable() {
        val tags = handler.getTags()
        tags.size shouldBe 77
        tags.first().id shouldBe "391b0423-d847-456f-aff0-8b0cfc03066b"
        tags.first().name shouldBe "Action"
        tags.last().name shouldBe "Zombies"
    }

    @Test
    fun emptyQueryMapOnlyModes() {
        handler.getQueryMap(handler.getMDFilterList()) shouldContainExactly mapOf(
            "includedTagsMode" to "AND",
            "excludedTagsMode" to "OR",
        )
    }

    @Test
    fun checkedGroupsBecomeLists() {
        val filters = handler.getMDFilterList()
        boxes(filters, "Original language")[1].state = true
        boxes(filters, "Original language")[2].state = true
        boxes(filters, "Content Rating")[0].state = true
        boxes(filters, "Publication Demographic")[1].state = true
        boxes(filters, "Status")[3].state = true
        val map = handler.getQueryMap(filters)
        map["originalLanguage[]"] shouldBe listOf("zh-hk", "zh", "ko")
        map["contentRating[]"] shouldBe listOf("safe")
        map["publicationDemographic[]"] shouldBe listOf("shounen")
        map["status[]"] shouldBe listOf("abandoned")
    }

    @Test
    fun tagStatesSplitIncludeExclude() {
        val filters = handler.getMDFilterList()
        val tags = (filters.first { it.name == "Tags" } as Filter.Group<*>).state.filterIsInstance<FilterHandler.Tag>()
        tags[0].state = Filter.TriState.STATE_INCLUDE
        tags[1].state = Filter.TriState.STATE_EXCLUDE
        tags[2].state = Filter.TriState.STATE_INCLUDE
        val map = handler.getQueryMap(filters)
        map["includedTags[]"] shouldBe listOf(tags[0].id, tags[2].id)
        map["excludedTags[]"] shouldBe listOf(tags[1].id)
    }

    @Test
    fun modesUppercased() {
        val filters = handler.getMDFilterList()
        select(filters, "Included tags mode").state = 1
        select(filters, "Excluded tags mode").state = 0
        val map = handler.getQueryMap(filters)
        map["includedTagsMode"] shouldBe "OR"
        map["excludedTagsMode"] shouldBe "AND"
    }

    @Test
    fun sortDefaultAddsNothing() {
        val filters = handler.getMDFilterList()
        val sort = filters.first { it.name == "Sort" } as FilterHandler.SortFilter
        sort.state = Filter.Sort.Selection(0, true)
        handler.getQueryMap(filters) shouldNotContainKey "order[]"
        sort.state = null
        handler.getQueryMap(filters).keys.none { it.startsWith("order") } shouldBe true
    }

    @Test
    fun sortSelectionAddsOrder() {
        val filters = handler.getMDFilterList()
        val sort = filters.first { it.name == "Sort" } as FilterHandler.SortFilter
        sort.state = Filter.Sort.Selection(1, true)
        handler.getQueryMap(filters)["order[createdAt]"] shouldBe "asc"
        sort.state = Filter.Sort.Selection(2, false)
        handler.getQueryMap(filters)["order[updatedAt]"] shouldBe "desc"
    }

    @Test
    fun unknownFilterIgnored() {
        val filters = FilterList(Filter.Header("x"), FilterHandler.SortFilter(arrayOf("a")))
        handler.getQueryMap(filters) shouldContainExactly emptyMap()
    }

    @Test
    fun helpersOnTheirOwn() {
        val map = mutableMapOf<String, Any>()
        map.putList("k", emptyList())
        map shouldContainExactly emptyMap()
        map.putList("k", listOf("v"))
        map["k"] shouldBe listOf("v")
        val group = object : Filter.Group<Filter.CheckBox>(
            "g",
            listOf(object : Filter.CheckBox("Yes", true) {}, object : Filter.CheckBox("No") {}),
        ) {}
        group.checkedNames() shouldContainExactly listOf("yes")
    }
}
