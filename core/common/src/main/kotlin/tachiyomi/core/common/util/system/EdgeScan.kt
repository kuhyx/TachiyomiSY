package tachiyomi.core.common.util.system

/**
 * Folds the four sampled columns into one verdict: whether the page reads as dark, which dark
 * colour to use, and how the top and bottom runs compare. Stops early once a column is
 * decisively black, exactly as the original loop did.
 */
internal class EdgeScan(private val image: PixelGrid, private val edges: EdgeSamples) {
    var darkBackground: Boolean = edges.cornersSuggestDark && !edges.mostCornersAreWhite
        private set
    var blackColor: Int = edges.firstDarkCorner
        private set
    var overallWhitePixels: Int = 0
        private set
    var overallBlackPixels: Int = 0
        private set
    private var topBlackStreak = 0
    private var topWhiteStreak = 0
    private var botBlackStreak = 0
    private var botWhiteStreak = 0

    val topIsBlackStreak: Boolean get() = topBlackStreak > topWhiteStreak
    val bottomIsBlackStreak: Boolean get() = botBlackStreak > botWhiteStreak

    fun run(): EdgeScan {
        for (x in edges.sampledColumns) {
            if (absorb(ColumnScan(image, x, edges.offsetX).scan(), x)) break
        }
        if (overallWhitePixels > MIN_WHITE_MAJORITY && overallWhitePixels > overallBlackPixels) {
            darkBackground = false
        }
        if (topIsBlackStreak && bottomIsBlackStreak) {
            darkBackground = true
        }
        return this
    }

    // Merges one column; true means the verdict is settled and later columns are skipped.
    private fun absorb(tally: ColumnTally, x: Int): Boolean {
        if (x == edges.left || x == edges.right) {
            overallWhitePixels += tally.whitePixels
            overallBlackPixels += tally.blackPixels
        }
        tally.topWhiteRun?.let { topWhiteStreak = it }
        tally.topBlackRun?.let { topBlackStreak = it }
        tally.bottomBlackRun?.let { botBlackStreak = it }
        tally.bottomWhiteRun?.let { botWhiteStreak = it }
        return when {
            tally.blackPixels > DOMINANT_PIXELS -> {
                settleDark(x, stop = true)
            }
            tally.hasBlackStreak -> {
                settleDark(x, stop = tally.blackPixels > STRONG_BLACK_PIXELS)
            }
            tally.hasWhiteStreak || tally.whitePixels > DOMINANT_PIXELS -> {
                darkBackground = false
                false
            }
            else -> {
                false
            }
        }
    }

    private fun settleDark(x: Int, stop: Boolean): Boolean {
        darkBackground = true
        if (x == edges.right || x == edges.rightOffsetX) {
            blackColor = edges.rightSideDark(blackColor)
        }
        if (stop) overallWhitePixels = 0
        return stop
    }

    private companion object {
        const val DOMINANT_PIXELS = 22
        const val STRONG_BLACK_PIXELS = 18
        const val MIN_WHITE_MAJORITY = 9
    }
}
