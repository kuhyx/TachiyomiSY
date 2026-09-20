package exh.md.service

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import exh.md.dto.AtHomeDto
import exh.md.dto.AtHomeImageReportDto
import exh.md.dto.ChapterDto
import exh.md.dto.ChapterListDto
import exh.md.dto.ResultDto
import exh.md.utils.MdApi
import exh.md.utils.MdConstants
import exh.md.utils.MdUtil
import exh.md.utils.encodeToBody
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrl

internal suspend fun MangaDexService.viewChapters(
    id: String,
    translatedLanguage: String,
    offset: Int,
    blockedGroups: String,
    blockedUploaders: String,
): ChapterListDto {
    val url = MdApi.manga.toHttpUrl()
        .newBuilder()
        .apply {
            addPathSegment(id)
            addPathSegment("feed")
            addQueryParameter(LIMIT, "500")
            addQueryParameter(INCLUDES, MdConstants.Types.scanlator)
            addQueryParameter("order[volume]", "desc")
            addQueryParameter("order[chapter]", "desc")
            addQueryParameter(CONTENT_RATING, "safe")
            addQueryParameter(CONTENT_RATING, "suggestive")
            addQueryParameter(CONTENT_RATING, "erotica")
            addQueryParameter(CONTENT_RATING, "pornographic")
            addQueryParameter("translatedLanguage[]", translatedLanguage)
            addQueryParameter("offset", offset.toString())
            blockedGroups.splitString().forEach {
                addQueryParameter("excludedGroups[]", it)
            }
            blockedUploaders.splitString().forEach {
                addQueryParameter("excludedUploaders[]", it)
            }
        }
        .build()

    return with(MdUtil.jsonParser) {
        client.newCall(
            GET(
                url,
                headers = headers,
                cache = CacheControl.FORCE_NETWORK,
            ),
        ).awaitSuccess().parseAs()
    }
}

internal suspend fun MangaDexService.viewChapter(id: String): ChapterDto {
    return with(MdUtil.jsonParser) {
        client.newCall(GET("${MdApi.chapter}/$id", headers = headers, cache = CacheControl.FORCE_NETWORK))
            .awaitSuccess()
            .parseAs()
    }
}

internal suspend fun MangaDexService.atHomeImageReport(atHomeImageReportDto: AtHomeImageReportDto): ResultDto {
    return with(MdUtil.jsonParser) {
        client.newCall(
            POST(
                MdConstants.atHomeReportUrl,
                body = MdUtil.encodeToBody(atHomeImageReportDto),
                headers = headers,
                cache = CacheControl.FORCE_NETWORK,
            ),
        ).awaitSuccess().parseAs()
    }
}

internal suspend fun MangaDexService.getAtHomeServer(
    atHomeRequestUrl: String,
): AtHomeDto {
    return with(MdUtil.jsonParser) {
        client.newCall(GET(atHomeRequestUrl, headers, CacheControl.FORCE_NETWORK))
            .awaitSuccess()
            .parseAs()
    }
}
