package exh.util

import androidx.core.graphics.toColorInt
import exh.metadata.metadata.base.RaisedTag
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.PURURIN_SOURCE_ID
import exh.source.TSUMINO_SOURCE_ID
import exh.source.lanraragiSourceIds
import exh.source.mangaDexSourceIds
import exh.source.nHentaiSourceIds
import java.util.Locale

internal object SourceTagsUtil {
    private const val TAG_TYPE_EXCLUDE = 69 // why not
    private const val TAG_TYPE_DEFAULT = 1
    private val spaceRegex = "\\s".toRegex()
    private val FIXED_TAG_SOURCE_IDS = setOf(EXH_SOURCE_ID, EH_SOURCE_ID, PURURIN_SOURCE_ID, TSUMINO_SOURCE_ID)

    fun getWrappedTag(
        sourceId: Long?,
        namespace: String? = null,
        tag: String? = null,
        fullTag: String? = null,
    ): String? {
        // Null is ruled out here so the constant arms below compare a plain Long.
        if (sourceId == null || !supportsWrappedTags(sourceId)) return null
        val parsed = toRaisedTag(namespace, tag, fullTag) ?: return null
        val parsedNamespace = parsed.namespace ?: return null
        val name = parsed.name.substringBefore('|').trim()
        return when (sourceId) {
            in nHentaiSourceIds -> wrapTagNHentai(parsedNamespace, name)
            in mangaDexSourceIds -> parsed.name
            PURURIN_SOURCE_ID -> name
            TSUMINO_SOURCE_ID -> wrapTagTsumino(parsedNamespace, name)
            else -> wrapTag(parsedNamespace, name)
        }
    }

    // The delegated-source id lists are filled at runtime, so this cannot be a constant set.
    private fun supportsWrappedTags(sourceId: Long): Boolean =
        sourceId in FIXED_TAG_SOURCE_IDS ||
            sourceId in nHentaiSourceIds ||
            sourceId in mangaDexSourceIds ||
            sourceId in lanraragiSourceIds

    private fun toRaisedTag(namespace: String?, tag: String?, fullTag: String?): RaisedTag? = when {
        fullTag != null -> parseTag(fullTag)
        namespace != null && tag != null -> RaisedTag(namespace, tag, TAG_TYPE_DEFAULT)
        else -> null
    }

    private fun wrapTag(namespace: String, tag: String) = if (tag.contains(spaceRegex)) {
        "$namespace:\"$tag$\""
    } else {
        "$namespace:$tag$"
    }

    private fun wrapTagNHentai(namespace: String, tag: String) = if (tag.contains(spaceRegex)) {
        if (namespace == "tag") {
            """"$tag""""
        } else {
            """$namespace:"$tag""""
        }
    } else {
        "$namespace:$tag"
    }

    private fun wrapTagTsumino(namespace: String, tag: String) = if (tag.contains(spaceRegex)) {
        if (namespace == "tags") {
            "\"${tag.replace(spaceRegex, "_")}\""
        } else {
            "\"$namespace: ${tag.replace(spaceRegex, "_")}\""
        }
    } else {
        if (namespace == "tags") {
            tag
        } else {
            "$namespace:$tag"
        }
    }

    fun parseTag(tag: String) = RaisedTag(
        if (tag.startsWith("-")) {
            tag.substringAfter("-")
        } else {
            tag
        }.substringBefore(':', missingDelimiterValue = "").trimOrNull(),
        tag.substringAfter(':', missingDelimiterValue = tag).trim(),
        if (tag.startsWith("-")) TAG_TYPE_EXCLUDE else TAG_TYPE_DEFAULT,
    )

    enum class GenreColor(val color: Int) {
        DOUJINSHI_COLOR("#f44336"),
        MANGA_COLOR("#ff9800"),
        ARTIST_CG_COLOR("#fbc02d"),
        GAME_CG_COLOR("#4caf50"),
        WESTERN_COLOR("#8bc34a"),
        NON_H_COLOR("#2196f3"),
        IMAGE_SET_COLOR("#3f51b5"),
        COSPLAY_COLOR("#9c27b0"),
        ASIAN_PORN_COLOR("#9575cd"),
        MISC_COLOR("#f06292"),
        ;

        constructor(color: String) : this(color.toColorInt())
    }

    fun getLocaleSourceUtil(language: String?): Locale? = LANGUAGE_TAGS[language]?.let(Locale::forLanguageTag)
}

// Source language names (as the galleries spell them) to BCP 47 tags.
private val LANGUAGE_TAGS = mapOf(
    "english" to "en",
    "eng" to "en",
    "chinese" to "zh",
    "spanish" to "es",
    "korean" to "ko",
    "russian" to "ru",
    "french" to "fr",
    "portuguese" to "pt",
    "thai" to "th",
    "german" to "de",
    "italian" to "it",
    "vietnamese" to "vi",
    "polish" to "pl",
    "hungarian" to "hu",
    "dutch" to "nl",
)
