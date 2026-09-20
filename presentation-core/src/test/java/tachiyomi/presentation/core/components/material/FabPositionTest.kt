package tachiyomi.presentation.core.components.material

import androidx.compose.material3.ExperimentalMaterial3Api
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

@OptIn(ExperimentalMaterial3Api::class)
internal class FabPositionTest {
    @Test
    fun centerToString() {
        FabPosition.Center.toString() shouldBe "FabPosition.Center"
    }

    @Test
    fun endToString() {
        FabPosition.End.toString() shouldBe "FabPosition.End"
    }

    @Test
    fun positionsDiffer() {
        FabPosition.Center shouldNotBe FabPosition.End
        FabPosition.End shouldBe FabPosition.End
    }

    @Test
    fun placementIsAValueObject() {
        val placement = FabPlacement(left = 10, width = 56, height = 56)
        placement shouldBe FabPlacement(left = 10, width = 56, height = 56)
        placement.hashCode() shouldBe FabPlacement(left = 10, width = 56, height = 56).hashCode()
        placement shouldNotBe FabPlacement(left = 11, width = 56, height = 56)
        placement shouldNotBe FabPlacement(left = 10, width = 57, height = 56)
        placement shouldNotBe FabPlacement(left = 10, width = 56, height = 57)
        placement.toString() shouldBe "FabPlacement(left=10, width=56, height=56)"
    }

    @Test
    fun placementCopiesAndDestructures() {
        val placement = FabPlacement(left = 10, width = 56, height = 56)
        placement.copy(left = 20) shouldBe FabPlacement(left = 20, width = 56, height = 56)
        placement.copy(width = 1, height = 2) shouldBe FabPlacement(left = 10, width = 1, height = 2)
        val (left, width, height) = placement
        left shouldBe 10
        width shouldBe 56
        height shouldBe 56
        placement.left shouldBe 10
        placement.width shouldBe 56
        placement.height shouldBe 56
    }
}
