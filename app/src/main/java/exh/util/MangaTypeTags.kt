package exh.util

import uy.kohesive.injekt.api.get

internal fun isMangaTag(tag: String): Boolean {
    return tag.contains("manga", true) ||
        tag.contains("манга", true)
}

internal fun isManhuaTag(tag: String): Boolean {
    return tag.contains("manhua", true) ||
        tag.contains("маньхуа", true)
}

internal fun isManhwaTag(tag: String): Boolean {
    return tag.contains("manhwa", true) ||
        tag.contains("манхва", true)
}

internal fun isComicTag(tag: String): Boolean {
    return tag.contains("comic", true) ||
        tag.contains("комикс", true)
}

internal fun isWebtoonTag(tag: String): Boolean {
    return tag.contains("long strip", true) ||
        tag.contains("webtoon", true)
}
