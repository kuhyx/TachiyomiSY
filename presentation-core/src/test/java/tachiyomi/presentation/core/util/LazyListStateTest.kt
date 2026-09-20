package tachiyomi.presentation.core.util

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** [shouldExpandFAB] on a real list: expanded at either end or while scrolling back up, collapsed mid-way down. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xhdpi")
internal class LazyListStateTest {
    @get:Rule
    val compose = createComposeRule()

    private val state = LazyListState()

    private fun setList(itemCount: Int) {
        compose.setContent {
            MaterialTheme {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp), state = state) {
                    items(itemCount) { Text(text = "Row $it", modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }

    private fun scrollBy(pixels: Float) {
        compose.runOnIdle { runBlocking { state.scrollBy(pixels) } }
    }

    @Test
    fun expandedAtTheTop() {
        setList(itemCount = 100)
        compose.runOnIdle { state.shouldExpandFAB() shouldBe true }
    }

    @Test
    fun collapsedWhileScrollingDown() {
        setList(itemCount = 100)
        scrollBy(400f)
        compose.runOnIdle { state.shouldExpandFAB() shouldBe false }
    }

    @Test
    fun expandedWhileScrollingBackUp() {
        setList(itemCount = 100)
        scrollBy(400f)
        scrollBy(-50f)
        compose.runOnIdle { state.shouldExpandFAB() shouldBe true }
    }

    @Test
    fun expandedAtTheEnd() {
        setList(itemCount = 100)
        scrollBy(100_000f)
        compose.runOnIdle {
            state.canScrollForward shouldBe false
            state.shouldExpandFAB() shouldBe true
        }
    }

    @Test
    fun expandedWhenListCannotScroll() {
        setList(itemCount = 2)
        compose.runOnIdle { state.shouldExpandFAB() shouldBe true }
    }
}
