package exh.md.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class MangaListDto(
    override val limit: Int,
    override val offset: Int,
    override val total: Int,
    override val data: List<MangaDataDto>,
) : ListCallDto<MangaDataDto>

@Serializable
internal data class MangaDto(
    val result: String,
    val data: MangaDataDto,
)

@Serializable
internal data class MangaDataDto(
    val id: String,
    val type: String,
    val attributes: MangaAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
internal data class MangaAttributesDto(
    val title: JsonElement,
    val altTitles: List<Map<String, String>>,
    val description: JsonElement,
    val links: JsonElement?,
    val originalLanguage: String,
    val lastVolume: String?,
    val lastChapter: String?,
    val contentRating: String?,
    val publicationDemographic: String?,
    val status: String?,
    val year: Int?,
    val tags: List<TagDto>,
)

@Serializable
internal data class TagDto(
    val id: String,
    val attributes: TagAttributesDto,
)

@Serializable
internal data class TagAttributesDto(
    val name: Map<String, String>,
)

@Serializable
internal data class RelationshipDto(
    val id: String,
    val type: String,
    val attributes: IncludesAttributesDto? = null,
)

@Serializable
internal data class IncludesAttributesDto(
    val name: String? = null,
    val fileName: String? = null,
)

@Serializable
internal data class AuthorListDto(
    val results: List<AuthorDto>,
)

@Serializable
internal data class AuthorDto(
    val result: String,
    val data: AuthorDataDto,
)

@Serializable
internal data class AuthorDataDto(
    val id: String,
    val attributes: AuthorAttributesDto,
)

@Serializable
internal data class AuthorAttributesDto(
    val name: String,
)

@Serializable
internal data class ReadingStatusDto(
    val status: String?,
)

@Serializable
internal data class ReadingStatusMapDto(
    val statuses: Map<String, String?>,
)

@Serializable
internal data class ReadChapterDto(
    val data: List<String>,
)

@Serializable
internal data class CoverListDto(
    val data: List<CoverDto>,
)

@Serializable
internal data class CoverDto(
    val id: String,
    val attributes: CoverAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
internal data class CoverAttributesDto(
    val fileName: String,
)

@Serializable
internal data class AggregateDto(
    val result: String,
    val volumes: Map<String, AggregateVolume>,
)

@Serializable
internal data class AggregateVolume(
    val volume: String,
    val count: String,
    val chapters: Map<String, AggregateChapter>,
)

@Serializable
internal data class AggregateChapter(
    val chapter: String,
    val count: String,
)
