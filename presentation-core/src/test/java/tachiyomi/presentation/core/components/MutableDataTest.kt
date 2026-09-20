package tachiyomi.presentation.core.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MutableDataTest {
    @Test
    fun swapReturnsThePreviousValue() {
        val cell = MutableData(1)
        cell.swap(2) shouldBe 1
        cell.value shouldBe 2
        cell.swap(3) shouldBe 2
        cell.value shouldBe 3
    }

    @Test
    fun valueIsAssignable() {
        val cell = MutableData(false)
        cell.value = true
        cell.value shouldBe true
    }

    @Test
    fun geometryKeepsItsFields() {
        val geometry = ThumbGeometry(
            thumbTopPadding = 1f,
            thumbBottomPadding = 2f,
            trackHeightPx = 3f,
            heightPx = 4f,
        )
        geometry.thumbTopPadding shouldBe 1f
        geometry.thumbBottomPadding shouldBe 2f
        geometry.trackHeightPx shouldBe 3f
        geometry.heightPx shouldBe 4f
    }
}
