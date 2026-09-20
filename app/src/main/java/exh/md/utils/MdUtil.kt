package exh.md.utils

import eu.kanade.tachiyomi.source.model.SManga
import exh.md.dto.MangaDataDto
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal object MdUtil {
    const val cdnUrl = "https://uploads.mangadex.org"
    const val baseUrl = "https://mangadex.org"
    const val chapterSuffix = "/chapter/"

    const val similarBaseApi = "https://api.similarmanga.com/similar/"

    const val mangaLimit = 20

    val jsonParser =
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            allowSpecialFloatingPointValues = true
            useArrayPolymorphism = true
            prettyPrint = true
        }

    private const val scanlatorSeparator = " & "

    val markdownLinksRegex = """\[([^]]+)]\(([^)]+)\)""".toRegex()
    val markdownItalicBoldRegex = """\*+\s*([^*]*)\s*\*+""".toRegex()
    val markdownItalicRegex = "_+\\s*([^_]*)\\s*_+".toRegex()

    val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSS", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }

    internal var codeVerifier: String? = null

    fun buildMangaUrl(mangaUuid: String): String = "/manga/$mangaUuid"

    // Get the ID from the manga url
    fun getMangaId(url: String): String = url.trimEnd('/').substringAfterLast("/")

    fun getChapterId(url: String) = url.substringAfterLast("/")

    fun getScanlatorString(scanlators: Set<String>): String = scanlators.sorted().joinToString(scanlatorSeparator)

    fun parseDate(dateAsString: String): Long =
        dateFormatter.parse(dateAsString)?.time ?: 0

    fun createMangaEntry(json: MangaDataDto, lang: String): SManga {
        return SManga(
            url = buildMangaUrl(json.id),
            title = getTitleFromManga(json.attributes, lang, true),
            thumbnailUrl = json.relationships
                .firstOrNull { relationshipDto -> relationshipDto.type == MdConstants.Types.coverArt }
                ?.attributes
                ?.fileName
                ?.let { coverFileName ->
                    cdnCoverUrl(json.id, coverFileName)
                }
                .orEmpty(),
        )
    }

    fun cdnCoverUrl(dexId: String, fileName: String): String = "$cdnUrl/covers/$dexId/$fileName"
}
