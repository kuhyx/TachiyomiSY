package tachiyomi.core.common.util.system

/**
 * The handful of pixels the background heuristic reads around a page's border: the four
 * corners, the middles of each edge, and the corners one step further out.
 */
internal class EdgeSamples(private val image: PixelGrid) {
    /** Inset of the sampled columns from the left/right border. */
    val left: Int = (image.width * SIDE_INSET_FRACTION).toInt()
    val right: Int = image.width - left

    /** How far the offset columns sit outside [left] and [right]. */
    val offsetX: Int = (image.width * OFFSET_FRACTION).toInt()
    val leftOffsetX: Int = left - offsetX
    val rightOffsetX: Int = right + offsetX

    private val top = EDGE_INSET
    private val bot = image.height - EDGE_INSET
    private val midX = image.width / 2
    private val midY = image.height / 2

    val topLeft: Int = image[left, top]
    val topRight: Int = image[right, top]
    val botLeft: Int = image[left, bot]
    val botRight: Int = image[right, bot]
    private val midLeft = image[left, midY]
    private val midRight = image[right, midY]
    private val topCenter = image[midX, top]
    private val bottomCenter = image[midX, bot]

    val topLeftIsDark: Boolean = topLeft.isDark()
    val topRightIsDark: Boolean = topRight.isDark()
    val botLeftIsDark: Boolean = botLeft.isDark()
    val botRightIsDark: Boolean = botRight.isDark()
    val topMidIsDark: Boolean = topCenter.isDark()
    private val midLeftIsDark = midLeft.isDark()
    private val midRightIsDark = midRight.isDark()

    /** The columns the scan walks, in order: the two insets, then the two offsets. */
    val sampledColumns: List<Int> = listOf(left, right, leftOffsetX, rightOffsetX)

    /** Whether a dark top corner is backed by another dark sample on its side. */
    val cornersSuggestDark: Boolean =
        (topLeftIsDark && (botLeftIsDark || botRightIsDark || topRightIsDark || midLeftIsDark || topMidIsDark)) ||
            (topRightIsDark && (botRightIsDark || botLeftIsDark || midRightIsDark || topMidIsDark))

    /** More than two white corners rule out a dark page. */
    val mostCornersAreWhite: Boolean =
        listOf(topLeft, topRight, botLeft, botRight).count { it.isWhite() } > MAX_WHITE_CORNERS

    /** The first dark corner, clockwise from top-left, or white when none is. */
    val firstDarkCorner: Int = when {
        topLeftIsDark -> topLeft
        topRightIsDark -> topRight
        botLeftIsDark -> botLeft
        botRightIsDark -> botRight
        else -> WHITE
    }

    val topCornersAreWhite: Boolean get() = topLeft.isWhite() && topRight.isWhite()
    val botCornersAreWhite: Boolean get() = botLeft.isWhite() && botRight.isWhite()

    /** The one colour to use when every edge sample is non-white and close to its neighbour. */
    fun uniformEdgeColor(): Int? {
        val ring = listOf(topLeft, topCenter, topRight, botRight, bottomCenter, botLeft)
        val uniform = ring.indices.all { index ->
            val color = ring[index]
            !color.isWhite() && color.isCloseTo(ring[(index + 1) % ring.size])
        }
        return if (uniform) topLeft else null
    }

    /** Both top corners and both offset top corners are dark, and the top edge or the scan agrees. */
    fun topEdgeIsDark(overallBlackPixels: Int): Boolean =
        topLeftIsDark && topRightIsDark &&
            image[leftOffsetX, top].isDark() && image[rightOffsetX, top].isDark() &&
            (topMidIsDark || overallBlackPixels > MIN_BLACK_SUPPORT)

    /** The bottom-edge twin of [topEdgeIsDark]. */
    fun bottomEdgeIsDark(overallBlackPixels: Int): Boolean =
        botLeftIsDark && botRightIsDark &&
            image[leftOffsetX, bot].isDark() && image[rightOffsetX, bot].isDark() &&
            (bottomCenter.isDark() || overallBlackPixels > MIN_BLACK_SUPPORT)

    /** The dark colour a right-side column should adopt: a dark right corner, else [fallback]. */
    fun rightSideDark(fallback: Int): Int = when {
        topRightIsDark -> topRight
        botRightIsDark -> botRight
        else -> fallback
    }

    private companion object {
        const val SIDE_INSET_FRACTION = 0.0275
        const val OFFSET_FRACTION = 0.01
        const val EDGE_INSET = 5
        const val MAX_WHITE_CORNERS = 2
        const val MIN_BLACK_SUPPORT = 9
    }
}

/** Opaque white, the fallback background. */
internal const val WHITE: Int = -0x1
