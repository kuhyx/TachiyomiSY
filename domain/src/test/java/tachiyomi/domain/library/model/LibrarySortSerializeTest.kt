package tachiyomi.domain.library.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category

internal class LibrarySortSerializeTest {

    @Test
    fun serializeJoinsNames() {
        val lastRead = LibrarySort(LibrarySort.Type.LastRead, LibrarySort.Direction.Ascending)
        val lastUpdate = LibrarySort(LibrarySort.Type.LastUpdate, LibrarySort.Direction.Descending)
        val tagList = LibrarySort(LibrarySort.Type.TagList, LibrarySort.Direction.Descending)
        lastRead.serialize() shouldBe "LAST_READ,ASCENDING"
        lastUpdate.serialize() shouldBe "LAST_MANGA_UPDATE,DESCENDING"
        tagList.serialize() shouldBe "TAG_LIST,DESCENDING"
    }

    @Test
    fun deserializeRoundTripsAll() {
        LibrarySort.types.forEach { type ->
            LibrarySort.directions.forEach { direction ->
                val sort = LibrarySort(type, direction)
                LibrarySort.deserialize(sort.serialize()) shouldBe sort
            }
        }
    }

    @Test
    fun malformedIsDefault() {
        LibrarySort.deserialize("") shouldBe LibrarySort.default
        LibrarySort.deserialize("LAST_READ") shouldBe LibrarySort.default
    }

    @Test
    fun unknownTypeKeepsDirection() {
        val descending = LibrarySort(LibrarySort.Type.Alphabetical, LibrarySort.Direction.Descending)
        LibrarySort.deserialize("NOPE,DESCENDING") shouldBe descending
        LibrarySort.deserialize("NOPE,ASCENDING") shouldBe LibrarySort.default
    }

    @Test
    fun otherDirectionIsDescending() {
        val descending = LibrarySort(LibrarySort.Type.Random, LibrarySort.Direction.Descending)
        val ascending = LibrarySort(LibrarySort.Type.Random, LibrarySort.Direction.Ascending)
        LibrarySort.deserialize("RANDOM,sideways") shouldBe descending
        LibrarySort.deserialize("RANDOM,ASCENDING,extra") shouldBe ascending
    }

    @Test
    fun serializerObjectDelegates() {
        val sort = LibrarySort(LibrarySort.Type.UnreadCount, LibrarySort.Direction.Ascending)
        LibrarySort.Serializer.serialize(sort) shouldBe "UNREAD_COUNT,ASCENDING"
        LibrarySort.Serializer.deserialize("UNREAD_COUNT,ASCENDING") shouldBe sort
        LibrarySort.Serializer.deserialize("garbage") shouldBe LibrarySort.default
    }

    @Test
    fun categorySortReadsFlags() {
        val category = Category(id = 3L, name = "c", order = 0L, flags = 0b00011100L)
        category.sort shouldBe LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Descending)
        val trackerMean = LibrarySort(LibrarySort.Type.TrackerMean, LibrarySort.Direction.Ascending)
        category.copy(flags = 0b01100000L).sort shouldBe trackerMean
    }

    @Test
    fun nullCategorySortIsDefault() {
        val category: Category? = null
        category.sort shouldBe LibrarySort.default
    }
}
