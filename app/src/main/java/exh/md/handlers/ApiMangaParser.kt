package exh.md.handlers

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import exh.log.xLogE
import exh.md.dto.ChapterDataDto
import exh.md.dto.ChapterDto
import exh.md.dto.MangaDto
import exh.md.utils.MdConstants
import exh.md.utils.MdUtil
import exh.md.utils.getTitleFromManga
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.metadata.metadata.base.raise
import exh.util.floor
import exh.util.nullIfEmpty
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.InsertFlatMetadata
import uy.kohesive.injekt.injectLazy

internal class ApiMangaParser(
    private val lang: String,
) {
    private val getManga: GetManga by injectLazy()
    private val insertFlatMetadata: InsertFlatMetadata by injectLazy()
    private val getFlatMetadataById: GetFlatMetadataById by injectLazy()

    val metaClass = MangaDexSearchMetadata::class

    // Use reflection to create a new instance of metadata.
    private fun newMetaInstance() = MangaDexSearchMetadata()

    suspend fun parseToManga(
        manga: SManga,
        sourceId: Long,
        input: MangaDto,
        extras: MangaDetailsExtras,
        preferences: MangaDetailsPreferences,
    ): SManga {
        val mangaId = getManga.await(manga.url, sourceId)?.id
        val metadata = if (mangaId != null) {
            val flatMetadata = getFlatMetadataById.await(mangaId)
            flatMetadata?.raise(metaClass) ?: newMetaInstance()
        } else {
            newMetaInstance()
        }

        parseIntoMetadata(metadata, input, extras, preferences)
        if (mangaId != null) {
            metadata.mangaId = mangaId
            insertFlatMetadata.await(metadata.flatten())
        }

        return metadata.createMangaInfo(manga)
    }

    fun parseIntoMetadata(
        metadata: MangaDexSearchMetadata,
        mangaDto: MangaDto,
        extras: MangaDetailsExtras,
        preferences: MangaDetailsPreferences,
    ) {
        val (simpleChapters, statistics, coverFileName) = extras
        with(metadata) {
            try {
                val mangaAttributesDto = mangaDto.data.attributes
                mdUuid = mangaDto.data.id
                title = MdUtil.getTitleFromManga(mangaAttributesDto, lang, preferences.preferExtensionLangTitle)
                altTitles = mangaAttributesDto.altTitles
                    .filter { it.containsKey(lang) || it.containsKey("${mangaAttributesDto.originalLanguage}-ro") }
                    .mapNotNull { it.values.singleOrNull() }
                    .nullIfEmpty()
                cover = mangaDto.data.coverUrl(coverFileName, preferences.coverQuality)
                description = mangaAttributesDto.description(lang, altTitles, preferences)
                authors = mangaDto.data.relationshipNames(MdConstants.Types.author)
                artists = mangaDto.data.relationshipNames(MdConstants.Types.artist)
                langFlag = mangaAttributesDto.originalLanguage
                lastChapterNumber = mangaAttributesDto.lastChapter?.toFloatOrNull()?.floor()
                statistics?.rating?.let {
                    rating = it.bayesian?.toFloat()
                    // manga.users = it.users
                }
                applyExternalLinks(mangaAttributesDto)
                // val filteredChapters = filterChapterForChecking(networkApiManga)
                status = mangaAttributesDto.mangaStatus(parseStatus(mangaAttributesDto.status), simpleChapters)
                if (tags.isNotEmpty()) tags.clear()
                tags += mangaAttributesDto.genreTags(lang)
            } catch (expected: Exception) {
                // Logged whatever the cause; the caller carries on.
                xLogE("Parse into metadata error", expected)
                throw expected
            }
        }
    }

    /* private fun filterChapterForChecking(serializer: ApiMangaSerializer): List<ChapterSerializer> {
         serializer.data.chapters ?: return emptyList()
         return serializer.data.chapters.asSequence()
             .filter { langs.contains(it.language) }
             .filter {
                 it.chapter?.let { chapterNumber ->
                     if (chapterNumber.toDoubleOrNull() == null) {
                         return@filter false
                     }
                     return@filter true
                 }
                 return@filter false
             }.toList()
     }*/

    /*private fun isOneShot(chapter: ChapterSerializer, finalChapterNumber: String): Boolean {
        return chapter.title.equals("oneshot", true) ||
            (
                (chapter.chapter.isNullOrEmpty() || chapter.chapter == "0") &&
                    MdUtil.validOneShotFinalChapters.contains(finalChapterNumber)
                )
    }*/

    private fun parseStatus(status: String?) = when (status) {
        "ongoing" -> SManga.ONGOING
        "completed" -> SManga.PUBLISHING_FINISHED
        "cancelled" -> SManga.CANCELLED
        "hiatus" -> SManga.ON_HIATUS
        else -> SManga.UNKNOWN
    }

    fun chapterListParse(chapterListResponse: List<ChapterDataDto>, groupMap: Map<String, String>): List<SChapter> {
        val now = System.currentTimeMillis()
        return chapterListResponse
            .filterNot { MdUtil.parseDate(it.attributes.publishAt) > now && it.attributes.externalUrl == null }
            .map {
                mapChapter(it, groupMap)
            }
    }

    fun chapterParseForMangaId(chapterDto: ChapterDto): String? =
        chapterDto.data.relationships.find { it.type.equals("manga", true) }?.id

    fun StringBuilder.appends(string: String): StringBuilder = append("$string ")

    private fun mapChapter(
        networkChapter: ChapterDataDto,
        groups: Map<String, String>,
    ): SChapter {
        val attributes = networkChapter.attributes
        val key = MdUtil.chapterSuffix + networkChapter.id
        val chapterName = StringBuilder()
        // Build chapter name

        if (attributes.volume != null) {
            val vol = "Vol." + attributes.volume
            chapterName.appends(vol)
            // todo
            // chapter.vol = vol
        }

        if (attributes.chapter.isNullOrBlank().not()) {
            val chp = "Ch.${attributes.chapter}"
            chapterName.appends(chp)
            // chapter.chapter_txt = chp
        }

        if (!attributes.title.isNullOrBlank()) {
            if (chapterName.isNotEmpty()) {
                chapterName.appends("-")
            }
            chapterName.append(attributes.title)
        }

        // if volume, chapter and title is empty its a oneshot
        if (chapterName.isEmpty()) {
            chapterName.append("Oneshot")
        }
        /*if ((status == 2 || status == 3)) {
            if (finalChapterNumber != null) {
                if ((isOneShot(networkChapter, finalChapterNumber) && totalChapterCount == 1) ||
                    networkChapter.chapter == finalChapterNumber && finalChapterNumber.toIntOrNull() != 0
                ) {
                    chapterName.add("[END]")
                }
            }
        }*/

        val name = chapterName.toString()
        // Convert from unix time
        val dateUpload = MdUtil.parseDate(attributes.readableAt)

        val scanlatorName = networkChapter.relationships
            .filter {
                it.type == MdConstants.Types.scanlator
            }
            .mapNotNull { groups[it.id] }
            .map {
                if (it == "no group") {
                    "No Group"
                } else {
                    it
                }
            }
            .toSet()
            .ifEmpty { setOf("No Group") }

        val scanlator = MdUtil.getScanlatorString(scanlatorName)

        // chapter.mangadex_chapter_id = MdUtil.getChapterId(chapter.url)

        // chapter.language = MdLang.fromIsoCode(attributes.translatedLanguage)?.prettyPrint ?: ""

        return SChapter(
            url = key,
            name = name,
            scanlator = scanlator,
            dateUpload = dateUpload,
        )
    }
}
