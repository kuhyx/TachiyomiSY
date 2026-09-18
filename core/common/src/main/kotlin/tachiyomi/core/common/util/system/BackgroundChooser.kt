package tachiyomi.core.common.util.system

/**
 * Algorithm for determining what background to accompany a comic/manga page, on the page's
 * pixels alone: [EdgeSamples] reads the border, [EdgeScan] walks four columns, and the
 * verdict becomes a solid colour or a top-to-bottom gradient.
 */
internal object BackgroundChooser {
    private const val MIN_SIZE = 50

    fun choose(image: PixelGrid, isLandscape: Boolean): PageBackground =
        if (image.width < MIN_SIZE || image.height < MIN_SIZE) {
            PageBackground.Solid(WHITE)
        } else {
            chooseFromEdges(image, EdgeSamples(image), isLandscape)
        }

    private fun chooseFromEdges(image: PixelGrid, edges: EdgeSamples, isLandscape: Boolean): PageBackground {
        val uniform = edges.uniformEdgeColor()
        if (uniform != null) {
            return PageBackground.Solid(uniform)
        }
        val scan = EdgeScan(image, edges).run()
        return if (isLandscape) {
            PageBackground.Solid(if (scan.darkBackground) scan.blackColor else WHITE)
        } else {
            portrait(edges, scan)
        }
    }

    private fun portrait(edges: EdgeSamples, scan: EdgeScan): PageBackground {
        val black = scan.blackColor
        val darkTop = PageBackground.Gradient(listOf(black, black, WHITE, WHITE))
        val darkBottom = PageBackground.Gradient(listOf(WHITE, WHITE, black, black))
        return when {
            scan.darkBackground && edges.botCornersAreWhite -> darkTop
            scan.darkBackground && edges.topCornersAreWhite -> darkBottom
            scan.darkBackground -> PageBackground.Solid(black)
            scan.topIsBlackStreak || edges.topEdgeIsDark(scan.overallBlackPixels) -> darkTop
            scan.bottomIsBlackStreak || edges.bottomEdgeIsDark(scan.overallBlackPixels) -> darkBottom
            else -> PageBackground.Solid(WHITE)
        }
    }
}
