package eu.kanade.presentation.category

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category

@RunWith(RobolectricTestRunner::class)
internal class CategoryExtensionsTest {
    @get:Rule
    val compose = createComposeRule()

    private val system = Category(id = Category.UNCATEGORIZED_ID, name = "", order = 0L, flags = 0L)
    private val named = Category(id = 5L, name = "Mine", order = 1L, flags = 0L)

    @Test
    fun contextNames() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        system.visualName(context) shouldBe "Default"
        named.visualName(context) shouldBe "Mine"
    }

    @Test
    fun composableNames() {
        compose.setContent { Text("${system.visualName}|${named.visualName}") }
        compose.onNodeWithText("Default|Mine").assertExists()
    }
}
