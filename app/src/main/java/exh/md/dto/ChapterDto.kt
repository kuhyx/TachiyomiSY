package exh.md.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ChapterListDto(
    override val limit: Int,
    override val offset: Int,
    override val total: Int,
    override val data: List<ChapterDataDto>,
) : ListCallDto<ChapterDataDto>

@Serializable
internal data class ChapterDto(
    val result: String,
    val data: ChapterDataDto,
)

@Serializable
internal data class ChapterDataDto(
    val id: String,
    val type: String,
    val attributes: ChapterAttributesDto,
    val relationships: List<RelationshipDto>,
)

@Serializable
internal data class ChapterAttributesDto(
    val title: String?,
    val volume: String?,
    val chapter: String?,
    val translatedLanguage: String,
    val externalUrl: String?,
    val pages: Int,
    val version: Int,
    val createdAt: String,
    val updatedAt: String,
    val publishAt: String,
    val readableAt: String,
)

@Serializable
internal data class GroupListDto(
    override val limit: Int,
    override val offset: Int,
    override val total: Int,
    override val data: List<GroupDataDto>,
) : ListCallDto<GroupDataDto>

@Serializable
internal data class GroupDto(
    val result: String,
    val data: GroupDataDto,
)

@Serializable
internal data class GroupDataDto(
    val id: String,
    val attributes: GroupAttributesDto,
)

@Serializable
internal data class GroupAttributesDto(
    val name: String,
)
