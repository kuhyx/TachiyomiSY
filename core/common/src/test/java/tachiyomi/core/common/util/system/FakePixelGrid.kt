package tachiyomi.core.common.util.system

/** An in-memory [PixelGrid] the background tests paint scenarios on. */
internal class FakePixelGrid(
    override val width: Int,
    override val height: Int,
    fill: Int = WHITE,
) : PixelGrid {
    private val pixels = IntArray(width * height) { fill }

    override fun get(x: Int, y: Int): Int = pixels[y * width + x]

    /** Paints every pixel with [x] in [xs] and [y] in [ys]. */
    fun paint(xs: IntRange, ys: IntRange, color: Int): FakePixelGrid = apply {
        for (y in ys) for (x in xs) pixels[y * width + x] = color
    }

    /** Paints every pixel whose coordinates satisfy [where]. */
    fun paintWhere(color: Int, where: (x: Int, y: Int) -> Boolean): FakePixelGrid = apply {
        for (y in 0..<height) for (x in 0..<width) if (where(x, y)) pixels[y * width + x] = color
    }

    companion object {
        const val WHITE: Int = 0xFFFFFFFF.toInt()
        const val BLACK: Int = 0xFF000000.toInt()
        const val GRAY: Int = 0xFF808080.toInt()
        const val NEAR_BLACK: Int = 0xFF101010.toInt()
        const val TRANSPARENT_BLACK: Int = 0x10000000
    }
}
