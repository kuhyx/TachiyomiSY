package eu.kanade.tachiyomi.data.track.komga

import kotlinx.serialization.Serializable

@Serializable
internal data class SeriesDto(
    val id: String,
    val libraryId: String,
    val name: String,
    val created: String?,
    val lastModified: String?,
    val fileLastModified: String,
    val booksCount: Int,
    val booksReadCount: Int,
    val booksUnreadCount: Int,
    val booksInProgressCount: Int,
    val metadata: SeriesMetadataDto,
    val booksMetadata: BookMetadataAggregationDto,
)

@Serializable
internal data class SeriesMetadataDto(
    val status: String,
    val created: String?,
    val lastModified: String?,
    val title: String,
    val titleSort: String,
    val summary: String,
    val summaryLock: Boolean,
    val readingDirection: String,
    val readingDirectionLock: Boolean,
    val publisher: String,
    val publisherLock: Boolean,
    val ageRating: Int?,
    val ageRatingLock: Boolean,
    val language: String,
    val languageLock: Boolean,
    val genres: Set<String>,
    val genresLock: Boolean,
    val tags: Set<String>,
    val tagsLock: Boolean,
)

@Serializable
internal data class BookMetadataAggregationDto(
    val authors: List<AuthorDto> = emptyList(),
    val releaseDate: String?,
    val summary: String,
    val summaryNumber: String,

    val created: String,
    val lastModified: String,
)

@Serializable
internal data class AuthorDto(
    val name: String,
    val role: String,
)

@Serializable
internal data class ReadProgressUpdateDto(
    val lastBookRead: Int,
)

@Serializable
internal data class ReadProgressUpdateV2Dto(
    val lastBookNumberSortRead: Double,
)

@Serializable
internal data class ReadListDto(
    val id: String,
    val name: String,
    val bookIds: List<String>,
    val createdDate: String,
    val lastModifiedDate: String,
    val filtered: Boolean,
)

@Serializable
internal data class ReadProgressDto(
    val booksCount: Int,
    val booksReadCount: Int,
    val booksUnreadCount: Int,
    val booksInProgressCount: Int,
    val lastReadContinuousIndex: Int,
) {
    fun toV2() = ReadProgressV2Dto(
        booksCount,
        booksReadCount,
        booksUnreadCount,
        booksInProgressCount,
        lastReadContinuousIndex.toDouble(),
        booksCount.toFloat(),
    )
}

@Serializable
internal data class ReadProgressV2Dto(
    val booksCount: Int,
    val booksReadCount: Int,
    val booksUnreadCount: Int,
    val booksInProgressCount: Int,
    val lastReadContinuousNumberSort: Double,
    val maxNumberSort: Float,
)
