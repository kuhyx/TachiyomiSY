package tachiyomi.core.common.util.system

import tachiyomi.core.common.util.system.FakePixelGrid.Companion.BLACK
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.GRAY
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.NEAR_BLACK
import tachiyomi.core.common.util.system.FakePixelGrid.Companion.WHITE

/** Named page shapes whose chosen background is pinned by the characterization tests. */
internal object BackgroundScenarios {
    private const val W = 100
    private const val H = 100

    val all: Map<String, PixelGrid> = linkedMapOf(
        "tiny" to FakePixelGrid(40, 40),
        "allWhite" to FakePixelGrid(W, H),
        "allBlack" to FakePixelGrid(W, H, BLACK),
        "allGray" to FakePixelGrid(W, H, GRAY),
        "blackTopHalf" to FakePixelGrid(W, H).paint(0..<W, 0..<50, BLACK),
        "blackBottomHalf" to FakePixelGrid(W, H).paint(0..<W, 50..<H, BLACK),
        "blackTopThird" to FakePixelGrid(W, H).paint(0..<W, 0..<33, BLACK),
        "blackBottomThird" to FakePixelGrid(W, H).paint(0..<W, 67..<H, BLACK),
        "blackLeftStripe" to FakePixelGrid(W, H).paint(0..<12, 0..<H, BLACK),
        "blackRightStripe" to FakePixelGrid(W, H).paint(88..<W, 0..<H, BLACK),
        "blackLeftStripeShort" to FakePixelGrid(W, H).paint(0..<12, 0..<70, BLACK),
        "blackRightStripeShort" to FakePixelGrid(W, H).paint(88..<W, 0..<70, BLACK),
        "blackWithWhiteBottomCorners" to FakePixelGrid(W, H, BLACK)
            .paint(0..<20, 80..<H, WHITE)
            .paint(80..<W, 80..<H, WHITE),
        "blackWithWhiteTopCorners" to FakePixelGrid(W, H, BLACK)
            .paint(0..<20, 0..<20, WHITE)
            .paint(80..<W, 0..<20, WHITE),
        "blackWithThreeWhiteCorners" to FakePixelGrid(W, H, BLACK)
            .paint(0..<20, 0..<20, WHITE)
            .paint(80..<W, 0..<20, WHITE)
            .paint(0..<20, 80..<H, WHITE),
        "grayWithBlackCorners" to FakePixelGrid(W, H, GRAY)
            .paint(0..<10, 0..<10, BLACK)
            .paint(90..<W, 0..<10, BLACK)
            .paint(0..<10, 90..<H, BLACK)
            .paint(90..<W, 90..<H, BLACK),
        "grayWithWhiteCorners" to FakePixelGrid(W, H, GRAY)
            .paint(0..<10, 0..<10, WHITE)
            .paint(90..<W, 0..<10, WHITE)
            .paint(0..<10, 90..<H, WHITE)
            .paint(90..<W, 90..<H, WHITE),
        "stripedRows" to FakePixelGrid(W, H).paintWhere(BLACK) { _, y -> y % 8 < 4 },
        "stripedRowsGray" to FakePixelGrid(W, H).paintWhere(GRAY) { _, y -> y % 8 < 4 },
        "darkEdgesOnlyNotOffset" to FakePixelGrid(W, H).paintWhere(NEAR_BLACK) { x, _ -> x == 2 || x == 98 },
        "darkTopBandThin" to FakePixelGrid(W, H).paint(0..<W, 0..<8, BLACK),
        "darkBottomBandThin" to FakePixelGrid(W, H).paint(0..<W, 92..<H, BLACK),
        "grayTopBlackBottom" to FakePixelGrid(W, H, GRAY).paint(0..<W, 50..<H, BLACK),
        "blackTopGrayBottom" to FakePixelGrid(W, H, BLACK).paint(0..<W, 50..<H, GRAY),
        "checker" to FakePixelGrid(W, H).paintWhere(BLACK) { x, y -> (x / 4 + y / 4) % 2 == 0 },
        "blackTopWhiteMidBlackBottom" to FakePixelGrid(W, H, BLACK).paint(0..<W, 30..<70, WHITE),
    )
}
