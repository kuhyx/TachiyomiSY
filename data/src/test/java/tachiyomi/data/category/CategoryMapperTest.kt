package tachiyomi.data.category

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.data.GetCategories
import tachiyomi.data.GetCategoriesByMangaId
import tachiyomi.data.GetCategory
import tachiyomi.domain.category.model.Category

internal class CategoryMapperTest {
    private val expected = Category(
        id = 1L,
        name = "Reading",
        order = 2L,
        flags = 3L,
        version = 4L,
        uid = 5L,
        lastModifiedAt = 6L,
    )

    @Test
    fun mapsGetCategoryRow() {
        val row = GetCategory(
            _id = 1L,
            name = "Reading",
            sort = 2L,
            flags = 3L,
            version = 4L,
            uid = 5L,
            last_modified_at = 6L,
        )
        CategoryMapper.mapCategory(row) shouldBe expected
    }

    @Test
    fun mapsGetCategoriesRow() {
        val row = GetCategories(
            id = 1L,
            name = "Reading",
            order = 2L,
            flags = 3L,
            version = 4L,
            uid = 5L,
            last_modified_at = 6L,
        )
        CategoryMapper.mapCategory(row) shouldBe expected
    }

    @Test
    fun mapsCategoriesByMangaIdRow() {
        val row = GetCategoriesByMangaId(
            id = 1L,
            name = "Reading",
            order = 2L,
            flags = 3L,
            version = 4L,
            uid = 5L,
            last_modified_at = 6L,
        )
        CategoryMapper.mapCategory(row) shouldBe expected
    }
}
