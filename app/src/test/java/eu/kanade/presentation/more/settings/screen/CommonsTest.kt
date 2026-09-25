package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category

internal val defaultCategory = Category(id = 0, name = "", order = 0, flags = 0)
internal val comics = Category(id = 1, name = "Comics", order = 1, flags = 0)
internal val novels = Category(id = 2, name = "Novels", order = 2, flags = 0)

@RunWith(RobolectricTestRunner::class)
internal class CommonsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun label(all: List<Category>, included: Set<String>, excluded: Set<String>): String {
        var text = ""
        compose.setContent { text = getCategoriesLabel(all, included, excluded) }
        compose.waitForIdle()
        return text
    }

    private val all = listOf(novels, defaultCategory, comics)

    @Test
    fun someIncludedNoneExcluded() {
        label(all, included = setOf("0", "2", "9"), excluded = emptySet()) shouldBe
            "Include: Default, Novels\nExclude: None"
    }

    @Test
    fun allIncludedSomeExcluded() {
        label(all, included = setOf("0", "1", "2"), excluded = setOf("1")) shouldBe
            "Include: All\nExclude: Comics"
    }

    @Test
    fun allExcluded() {
        label(all, included = emptySet(), excluded = setOf("0", "1", "2")) shouldBe
            "Include: None\nExclude: All"
    }

    @Test
    fun nothingChosen() {
        label(all, included = emptySet(), excluded = setOf("1")) shouldBe "Include: All\nExclude: Comics"
    }
}
