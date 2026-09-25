package exh.md.utils

import exh.md.dto.MangaAttributesDto
import exh.md.dto.MangaDataDto
import exh.md.dto.RelationshipDto
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun mangaAttributes(
    title: Map<String, String> = mapOf("en" to "Title"),
    altTitles: List<Map<String, String>> = emptyList(),
    description: Map<String, String> = emptyMap(),
    originalLanguage: String = "ja",
): MangaAttributesDto = MangaAttributesDto(
    title = buildJsonObject { title.forEach { (k, v) -> put(k, v) } },
    altTitles = altTitles,
    description = buildJsonObject { description.forEach { (k, v) -> put(k, v) } },
    links = null,
    originalLanguage = originalLanguage,
    lastVolume = null,
    lastChapter = null,
    contentRating = null,
    publicationDemographic = null,
    status = null,
    year = null,
    tags = emptyList(),
)

internal fun mangaData(
    id: String = "uuid-1",
    attributes: MangaAttributesDto = mangaAttributes(),
    relationships: List<RelationshipDto> = emptyList(),
): MangaDataDto = MangaDataDto(id = id, type = "manga", attributes = attributes, relationships = relationships)
