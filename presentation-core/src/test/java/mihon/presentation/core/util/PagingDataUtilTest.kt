package mihon.presentation.core.util

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PagingDataUtilTest {
    @get:Rule
    val compose = createComposeRule()

    private val pagers: MutableStateFlow<Flow<PagingData<Int>>> =
        MutableStateFlow(flowOf(PagingData.from(listOf(1, 2))))

    @Test
    fun collectsTheCurrentPager() {
        compose.setContent {
            val items = pagers.collectAsLazyPagingItems()
            Text("count=${items.itemCount}")
        }
        compose.onNodeWithText("count=2").assertIsDisplayed()
    }

    @Test
    fun followsAReplacedPager() {
        compose.setContent {
            val items = pagers.collectAsLazyPagingItems()
            Text("count=${items.itemCount}")
        }
        compose.onNodeWithText("count=2").assertIsDisplayed()
        pagers.value = flowOf(PagingData.from(listOf(1, 2, 3)))
        compose.waitForIdle()
        compose.onNodeWithText("count=3").assertIsDisplayed()
    }
}
