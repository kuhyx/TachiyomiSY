package tachiyomi.domain.library.model

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated

// The fallback test stubs the companion's direction set, which is JVM-global state.
@Isolated
internal class LibrarySortTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun flagPacksTypeAndDirection() {
        val sort = LibrarySort(LibrarySort.Type.LastUpdate, LibrarySort.Direction.Descending)
        sort.flag shouldBe 0b00001000L
        sort.mask shouldBe 0b01111100L
        sort.isAscending shouldBe false
        LibrarySort.default.isAscending shouldBe true
    }

    @Test
    fun typeHasMaskAndDistinctFlags() {
        LibrarySort.types.forEach { type -> type.mask shouldBe 0b00111100L }
        LibrarySort.types.map { it.flag }.toSet().size shouldBe LibrarySort.types.size
        LibrarySort.types.map { it.toString() }.toSet().size shouldBe LibrarySort.types.size
        LibrarySort.types.map { it.hashCode() }.toSet().size shouldBe LibrarySort.types.size
    }

    @Test
    fun typeValueOfFindsEveryType() {
        LibrarySort.types.forEach { type ->
            LibrarySort.Type.valueOf(type.flag) shouldBe type
            // The direction bit is outside the type mask and must not matter.
            LibrarySort.Type.valueOf(type.flag or 0b01000000L) shouldBe type
        }
        LibrarySort.Type.valueOf(0b00111100L) shouldBe LibrarySort.Type.Random
        LibrarySort.Type.valueOf(0b00100100L) shouldBe LibrarySort.Type.TagList
    }

    @Test
    fun typeValueOfUnknownIsDefault() {
        // 0b101000 sits inside the type mask but no type claims it.
        LibrarySort.Type.valueOf(0b00101000L) shouldBe LibrarySort.default.type
        LibrarySort.Type.valueOf(0b00110000L) shouldBe LibrarySort.Type.Alphabetical
    }

    @Test
    fun directionValueOfReadsBit() {
        LibrarySort.directions.forEach { direction -> direction.mask shouldBe 0b01000000L }
        LibrarySort.Direction.valueOf(0b01000000L) shouldBe LibrarySort.Direction.Ascending
        LibrarySort.Direction.valueOf(0b01111111L) shouldBe LibrarySort.Direction.Ascending
        LibrarySort.Direction.valueOf(0L) shouldBe LibrarySort.Direction.Descending
        LibrarySort.Direction.valueOf(0b00111100L) shouldBe LibrarySort.Direction.Descending
        LibrarySort.Direction.Ascending.toString() shouldNotBe LibrarySort.Direction.Descending.toString()
        LibrarySort.Direction.Ascending.hashCode() shouldNotBe LibrarySort.Direction.Descending.hashCode()
    }

    @Test
    fun directionValueOfFallsBack() {
        // Both bit values are declared, so the fallback only runs when the set is emptied;
        // a clear bit then yields the default (Ascending) instead of Descending.
        mockkObject(LibrarySort)
        every { LibrarySort.directions } returns emptySet()

        LibrarySort.Direction.valueOf(0L) shouldBe LibrarySort.default.direction
        LibrarySort.Direction.valueOf(0L) shouldNotBe LibrarySort.Direction.Descending
    }

    @Test
    fun valueOfNullIsDefault() {
        LibrarySort.valueOf(null) shouldBe LibrarySort.default
        LibrarySort.default shouldBe LibrarySort(LibrarySort.Type.Alphabetical, LibrarySort.Direction.Ascending)
    }

    @Test
    fun valueOfUnpacksFlags() {
        val dateAdded = LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Ascending)
        val lastRead = LibrarySort(LibrarySort.Type.LastRead, LibrarySort.Direction.Descending)
        LibrarySort.valueOf(0b01011100L) shouldBe dateAdded
        LibrarySort.valueOf(0b00000100L) shouldBe lastRead
    }

    @Test
    fun typesAndDirectionsOrdered() {
        LibrarySort.types.toList() shouldContainExactly listOf(
            LibrarySort.Type.Alphabetical,
            LibrarySort.Type.LastRead,
            LibrarySort.Type.LastUpdate,
            LibrarySort.Type.UnreadCount,
            LibrarySort.Type.TotalChapters,
            LibrarySort.Type.LatestChapter,
            LibrarySort.Type.ChapterFetchDate,
            LibrarySort.Type.DateAdded,
            LibrarySort.Type.TrackerMean,
            LibrarySort.Type.Random,
            LibrarySort.Type.TagList,
        )
        LibrarySort.directions.toList() shouldContainExactly listOf(
            LibrarySort.Direction.Ascending,
            LibrarySort.Direction.Descending,
        )
    }

    @Test
    fun isADataClass() {
        val sort = LibrarySort(LibrarySort.Type.Random, LibrarySort.Direction.Descending)
        val copy = sort.copy(direction = LibrarySort.Direction.Ascending)
        val (type, direction) = copy
        type shouldBe LibrarySort.Type.Random
        direction shouldBe LibrarySort.Direction.Ascending
        copy shouldNotBe sort
        copy.copy(direction = LibrarySort.Direction.Descending) shouldBe sort
        sort.hashCode() shouldBe LibrarySort(LibrarySort.Type.Random, LibrarySort.Direction.Descending).hashCode()
        sort.toString() shouldBe "LibrarySort(type=Random, direction=Descending)"
    }
}
