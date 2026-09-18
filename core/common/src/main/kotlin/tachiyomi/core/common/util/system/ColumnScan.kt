package tachiyomi.core.common.util.system

/** Tallies of one sampled column, read top to bottom. */
internal class ColumnTally {
    var whitePixels: Int = 0
        private set
    var blackPixels: Int = 0
        private set

    /** A run of at least [WHITE_STREAK_MIN] white samples was seen. */
    var hasWhiteStreak: Boolean = false
        private set

    /** A run of at least [BLACK_STREAK_MIN] dark samples was seen. */
    var hasBlackStreak: Boolean = false
        private set

    /** The last qualifying run that started at the top, or null when none did. */
    var topWhiteRun: Int? = null
        private set
    var topBlackRun: Int? = null
        private set

    /** The run still open at the bottom, when it is long enough. */
    var bottomBlackRun: Int? = null
        private set
    var bottomWhiteRun: Int? = null
        private set

    private var whiteRun = 0
    private var blackRun = 0

    fun white(index: Int) {
        whiteRun++
        whitePixels++
        if (whiteRun >= WHITE_STREAK_MIN) hasWhiteStreak = true
        if (whiteRun >= SHORT_RUN_MIN && whiteRun >= index - 1) topWhiteRun = whiteRun
        endBlackRun(index)
    }

    fun black() {
        whiteRun = 0
        blackPixels++
        blackRun++
        if (blackRun >= BLACK_STREAK_MIN) hasBlackStreak = true
    }

    fun other(index: Int) {
        whiteRun = 0
        endBlackRun(index)
    }

    fun finish() {
        if (blackRun >= SHORT_RUN_MIN) {
            bottomBlackRun = blackRun
        } else if (whiteRun >= SHORT_RUN_MIN) {
            bottomWhiteRun = whiteRun
        }
    }

    private fun endBlackRun(index: Int) {
        if (blackRun >= SHORT_RUN_MIN && blackRun >= index - 1) topBlackRun = blackRun
        blackRun = 0
    }

    companion object {
        const val WHITE_STREAK_MIN: Int = 15
        const val BLACK_STREAK_MIN: Int = 14
        const val SHORT_RUN_MIN: Int = 7
    }
}

/** Walks [ROW_SAMPLES] rows of one column, pairing each sample with its offset neighbour. */
internal class ColumnScan(private val image: PixelGrid, private val x: Int, offsetX: Int) {
    private val offsetColumn = x + if (x < image.width / 2) -offsetX else offsetX

    fun scan(): ColumnTally {
        val tally = ColumnTally()
        (0..<image.height step image.height / ROW_SAMPLES).forEachIndexed { index, y ->
            val pixel = image[x, y]
            when {
                pixel.isWhite() -> tally.white(index)
                pixel.isDark() && image[offsetColumn, y].isDark() -> tally.black()
                else -> tally.other(index)
            }
        }
        tally.finish()
        return tally
    }

    private companion object {
        const val ROW_SAMPLES = 25
    }
}
