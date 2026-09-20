package exh.md.handlers

import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.model.Page
import exh.md.handlers.BilibiliHandler.BilibiliPageDto
import exh.md.handlers.BilibiliHandler.BilibiliReader
import exh.md.handlers.BilibiliHandler.BilibiliResultDto
import exh.md.handlers.BilibiliHandler.Companion.BASE_API_ENDPOINT
import exh.md.handlers.BilibiliHandler.Companion.JSON_MEDIA_TYPE
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

internal suspend fun BilibiliHandler.fetchPageList(externalUrl: String, chapterNumber: String): List<Page> {
    // Sometimes the urls direct it to the manga page instead, so we try to find the correct chapter
    // Though these seem to be older chapters, so maybe remove this later
    val chapterUrl = if (externalUrl.contains("mc\\d*/\\d*".toRegex())) {
        getChapterUrl(externalUrl)
    } else {
        val mangaUrl = getMangaUrl(externalUrl)
        val chapters = getChapterList(mangaUrl)
        val chapter = chapters
            .find { it.chapter_number == chapterNumber.toFloatOrNull() }
            ?: throw NoSuchElementException("Unknown chapter $chapterNumber")
        chapter.url
    }

    return fetchPageList(chapterUrl)
}

internal suspend fun BilibiliHandler.fetchPageList(chapterUrl: String): List<Page> {
    val response = client.newCall(pageListRequest(chapterUrl)).awaitSuccess()
    return pageListParse(response)
}

internal fun BilibiliHandler.pageListRequest(chapterUrl: String): Request {
    val chapterId = chapterUrl.substringAfterLast("/").toInt()

    val jsonPayload = buildJsonObject { put("ep_id", chapterId) }
    val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

    val newHeaders = headers
        .newBuilder()
        .add(CONTENT_LENGTH, requestBody.contentLength().toString())
        .add(CONTENT_TYPE, requestBody.contentType().toString())
        .set(REFERER_HEADER, baseUrl + chapterUrl)
        .build()

    return POST(
        "$baseUrl/$BASE_API_ENDPOINT/GetImageIndex?device=pc&platform=web",
        headers = newHeaders,
        body = requestBody,
    )
}

internal fun BilibiliHandler.pageListParse(response: Response): List<Page> {
    val result = with(json) { response.parseAs<BilibiliResultDto<BilibiliReader>>() }

    if (result.code != 0) {
        return emptyList()
    }

    return result.data!!.images
        .mapIndexed { i, page -> Page(i, page.path, "") }
}

internal suspend fun BilibiliHandler.getImageUrl(page: Page): String {
    val response = client.newCall(imageUrlRequest(page)).awaitSuccess()
    return imageUrlParse(response)
}

internal fun BilibiliHandler.imageUrlRequest(page: Page): Request {
    val jsonPayload = buildJsonObject {
        put("urls", buildJsonArray { add(page.url) }.toString())
    }
    val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE)

    val newHeaders = headers.newBuilder()
        .add(CONTENT_LENGTH, requestBody.contentLength().toString())
        .add(CONTENT_TYPE, requestBody.contentType().toString())
        .build()

    return POST(
        "$baseUrl/$BASE_API_ENDPOINT/ImageToken?device=pc&platform=web",
        headers = newHeaders,
        body = requestBody,
    )
}

internal fun BilibiliHandler.imageUrlParse(response: Response): String {
    val result = with(json) {
        response.parseAs<BilibiliResultDto<List<BilibiliPageDto>>>()
    }
    val page = result.data!![0]

    return "${page.url}?token=${page.token}"
}
