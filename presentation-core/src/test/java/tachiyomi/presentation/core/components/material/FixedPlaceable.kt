package tachiyomi.presentation.core.components.material

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** A placeable of a fixed size that only remembers where it was placed. */
internal class FixedPlaceable(width: Int, height: Int) : Placeable() {
    val placements = mutableListOf<IntOffset>()

    init {
        measuredSize = IntSize(width = width, height = height)
    }

    override fun get(alignmentLine: AlignmentLine): Int = AlignmentLine.Unspecified

    override fun placeAt(position: IntOffset, zIndex: Float, layerBlock: (GraphicsLayerScope.() -> Unit)?) {
        placements += position
    }
}
