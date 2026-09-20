package tachiyomi.presentation.core.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaddingValuesTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var sum: PaddingValues

    @Test
    fun addsEverySideInLtr() {
        compose.setContent {
            sum = PaddingValues(start = 1.dp, top = 2.dp, end = 3.dp, bottom = 4.dp) + PaddingValues(10.dp)
        }
        sum.calculateStartPadding(LayoutDirection.Ltr) shouldBe 11.dp
        sum.calculateTopPadding() shouldBe 12.dp
        sum.calculateEndPadding(LayoutDirection.Ltr) shouldBe 13.dp
        sum.calculateBottomPadding() shouldBe 14.dp
    }

    @Test
    fun resolvesAbsoluteSidesInRtl() {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                sum = PaddingValues.Absolute(left = 1.dp, right = 3.dp) + PaddingValues(start = 10.dp, end = 30.dp)
            }
        }
        sum.calculateStartPadding(LayoutDirection.Rtl) shouldBe 13.dp
        sum.calculateEndPadding(LayoutDirection.Rtl) shouldBe 31.dp
        sum.calculateTopPadding() shouldBe 0.dp
        sum.calculateBottomPadding() shouldBe 0.dp
    }
}
