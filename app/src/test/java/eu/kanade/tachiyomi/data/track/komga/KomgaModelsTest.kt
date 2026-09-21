package eu.kanade.tachiyomi.data.track.komga

import eu.kanade.tachiyomi.data.track.fixture
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class KomgaModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private fun load(name: String): String = fixture("eu/kanade/tachiyomi/data/track/komga/$name")

    @Test
    fun seriesDecodes() {
        val series = json.decodeFromString<SeriesDto>(load("series.json"))
        series.id shouldBe "series-1"
        series.libraryId shouldBe "lib-1"
        series.name shouldBe "Komga Series"
        series.created shouldBe "2024-01-01T00:00:00Z"
        series.lastModified shouldBe "2024-02-01T00:00:00Z"
        series.fileLastModified shouldBe "2024-02-01T00:00:00Z"
        series.booksCount shouldBe 10
        series.booksReadCount shouldBe 4
        series.booksUnreadCount shouldBe 5
        series.booksInProgressCount shouldBe 1
        json.decodeFromString<SeriesDto>(json.encodeToString(series)) shouldBe series
        val built = SeriesDto(
            id = "series-1",
            libraryId = "lib-1",
            name = "Komga Series",
            created = "2024-01-01T00:00:00Z",
            lastModified = "2024-02-01T00:00:00Z",
            fileLastModified = "2024-02-01T00:00:00Z",
            booksCount = 10,
            booksReadCount = 4,
            booksUnreadCount = 5,
            booksInProgressCount = 1,
            metadata = series.metadata,
            booksMetadata = series.booksMetadata,
        )
        built shouldBe series
        built.hashCode() shouldBe series.hashCode()
        built.toString() shouldBe series.toString()
    }

    @Test
    fun seriesMetadataDecodes() {
        val metadata = json.decodeFromString<SeriesDto>(load("series.json")).metadata
        metadata.status shouldBe "ONGOING"
        metadata.created shouldBe "2024-01-01T00:00:00Z"
        metadata.lastModified shouldBe null
        metadata.title shouldBe "Komga Title"
        metadata.titleSort shouldBe "komga title"
        metadata.summary shouldBe "A summary"
        metadata.summaryLock shouldBe false
        metadata.readingDirection shouldBe "LEFT_TO_RIGHT"
        metadata.readingDirectionLock shouldBe false
        metadata.publisher shouldBe "Pub"
        metadata.publisherLock shouldBe false
        metadata.ageRating shouldBe 12
        metadata.ageRatingLock shouldBe false
        metadata.language shouldBe "en"
        metadata.languageLock shouldBe false
        metadata.genres shouldBe setOf("action")
        metadata.genresLock shouldBe false
        metadata.tags shouldBe setOf("tag")
        metadata.tagsLock shouldBe true
        val built = SeriesMetadataDto(
            status = "ONGOING",
            created = "2024-01-01T00:00:00Z",
            lastModified = null,
            title = "Komga Title",
            titleSort = "komga title",
            summary = "A summary",
            summaryLock = false,
            readingDirection = "LEFT_TO_RIGHT",
            readingDirectionLock = false,
            publisher = "Pub",
            publisherLock = false,
            ageRating = 12,
            ageRatingLock = false,
            language = "en",
            languageLock = false,
            genres = setOf("action"),
            genresLock = false,
            tags = setOf("tag"),
            tagsLock = true,
        )
        built shouldBe metadata
        built.toString() shouldBe metadata.toString()
    }

    @Test
    fun booksMetadataDecodes() {
        val books = json.decodeFromString<SeriesDto>(load("series.json")).booksMetadata
        books.authors shouldBe listOf(AuthorDto(name = "Author", role = "writer"))
        books.authors.single().name shouldBe "Author"
        books.authors.single().role shouldBe "writer"
        books.releaseDate shouldBe "2020-01-01"
        books.summary shouldBe "books summary"
        books.summaryNumber shouldBe "1"
        books.created shouldBe "2024-01-01T00:00:00Z"
        books.lastModified shouldBe "2024-02-01T00:00:00Z"
        val bare = BookMetadataAggregationDto(
            releaseDate = null,
            summary = "",
            summaryNumber = "",
            created = "c",
            lastModified = "m",
        )
        bare.authors shouldBe emptyList()
    }

    @Test
    fun readListDecodes() {
        val readList = json.decodeFromString<ReadListDto>(load("readlist.json"))
        readList shouldBe ReadListDto(
            id = "readlist-1",
            name = "My Read List",
            bookIds = listOf("b1", "b2"),
            createdDate = "2024-01-01T00:00:00Z",
            lastModifiedDate = "2024-02-01T00:00:00Z",
            filtered = false,
        )
        readList.id shouldBe "readlist-1"
        readList.name shouldBe "My Read List"
        readList.bookIds shouldBe listOf("b1", "b2")
        readList.createdDate shouldBe "2024-01-01T00:00:00Z"
        readList.lastModifiedDate shouldBe "2024-02-01T00:00:00Z"
        readList.filtered shouldBe false
    }

    @Test
    fun progressConvertsToV2() {
        val v1 = json.decodeFromString<ReadProgressDto>(load("progress_v1.json"))
        v1.booksCount shouldBe 2
        v1.booksReadCount shouldBe 2
        v1.booksUnreadCount shouldBe 0
        v1.booksInProgressCount shouldBe 0
        v1.lastReadContinuousIndex shouldBe 2
        v1 shouldBe ReadProgressDto(
            booksCount = 2,
            booksReadCount = 2,
            booksUnreadCount = 0,
            booksInProgressCount = 0,
            lastReadContinuousIndex = 2,
        )
        json.encodeToString(v1) shouldBe load("progress_v1.json").replace(Regex("\\s"), "")
        v1.toV2() shouldBe ReadProgressV2Dto(
            booksCount = 2,
            booksReadCount = 2,
            booksUnreadCount = 0,
            booksInProgressCount = 0,
            lastReadContinuousNumberSort = 2.0,
            maxNumberSort = 2f,
        )

        val v2 = json.decodeFromString<ReadProgressV2Dto>(load("progress_v2.json"))
        v2.booksCount shouldBe 10
        v2.booksReadCount shouldBe 4
        v2.booksUnreadCount shouldBe 5
        v2.booksInProgressCount shouldBe 1
        v2.lastReadContinuousNumberSort shouldBe 4.5
        v2.maxNumberSort shouldBe 10f
    }

    @Test
    fun updatePayloadsEncode() {
        json.encodeToString(ReadProgressUpdateDto(lastBookRead = 3)) shouldBe """{"lastBookRead":3}"""
        json.encodeToString(ReadProgressUpdateV2Dto(lastBookNumberSortRead = 3.5)) shouldBe
            """{"lastBookNumberSortRead":3.5}"""
        json.decodeFromString<ReadProgressUpdateDto>("""{"lastBookRead":3}""").lastBookRead shouldBe 3
        json.decodeFromString<ReadProgressUpdateV2Dto>("""{"lastBookNumberSortRead":3.5}""")
            .lastBookNumberSortRead shouldBe 3.5
    }
}
