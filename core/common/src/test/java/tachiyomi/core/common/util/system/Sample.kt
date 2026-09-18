package tachiyomi.core.common.util.system

/** The single pixels [EdgeSamples] reads on a 100x100 page. */
internal enum class Sample(val x: Int, val y: Int) {
    TOP_LEFT(2, 5),
    TOP_RIGHT(98, 5),
    BOTTOM_LEFT(2, 95),
    BOTTOM_RIGHT(98, 95),
    MID_LEFT(2, 50),
    MID_RIGHT(98, 50),
    TOP_CENTER(50, 5),
    BOTTOM_CENTER(50, 95),
    TOP_LEFT_OFFSET(1, 5),
    TOP_RIGHT_OFFSET(99, 5),
    BOTTOM_LEFT_OFFSET(1, 95),
    BOTTOM_RIGHT_OFFSET(99, 95),
}

/** A white 100x100 page with [color] at each of [samples]. */
internal fun pageWith(color: Int, vararg samples: Sample): FakePixelGrid =
    FakePixelGrid(100, 100).apply { samples.forEach { paint(it.x..it.x, it.y..it.y, color) } }
