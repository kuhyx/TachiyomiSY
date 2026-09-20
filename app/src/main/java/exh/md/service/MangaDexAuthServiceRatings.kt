package exh.md.service

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import exh.md.dto.RatingDto
import exh.md.dto.RatingResponseDto
import exh.md.dto.ResultDto
import exh.md.utils.MdApi
import exh.md.utils.MdUtil
import exh.md.utils.encodeToBody
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

internal suspend fun MangaDexAuthService.followManga(mangaId: String): ResultDto {
    return with(MdUtil.jsonParser) {
        client.newCall(
            POST(
                "${MdApi.manga}/$mangaId/follow",
                headers,
                cache = CacheControl.FORCE_NETWORK,
            ),
        ).awaitSuccess().parseAs()
    }
}

internal suspend fun MangaDexAuthService.unfollowManga(mangaId: String): ResultDto {
    return with(MdUtil.jsonParser) {
        client.newCall(
            Request.Builder()
                .url("${MdApi.manga}/$mangaId/follow")
                .delete()
                .headers(headers)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build(),
        ).awaitSuccess().parseAs()
    }
}

internal suspend fun MangaDexAuthService.updateMangaRating(mangaId: String, rating: Int): ResultDto {
    return with(MdUtil.jsonParser) {
        client.newCall(
            POST(
                "${MdApi.rating}/$mangaId",
                headers,
                body = MdUtil.encodeToBody(RatingDto(rating)),
                cache = CacheControl.FORCE_NETWORK,
            ),
        ).awaitSuccess().parseAs()
    }
}

internal suspend fun MangaDexAuthService.deleteMangaRating(mangaId: String): ResultDto {
    return with(MdUtil.jsonParser) {
        client.newCall(
            Request.Builder()
                .delete()
                .url("${MdApi.rating}/$mangaId")
                .headers(headers)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build(),
        ).awaitSuccess().parseAs()
    }
}

internal suspend fun MangaDexAuthService.mangasRating(vararg mangaIds: String): RatingResponseDto {
    return with(MdUtil.jsonParser) {
        client.newCall(
            GET(
                MdApi.rating.toHttpUrl()
                    .newBuilder()
                    .apply {
                        mangaIds.forEach {
                            addQueryParameter("manga[]", it)
                        }
                    }
                    .build(),
                headers,
                cache = CacheControl.FORCE_NETWORK,
            ),
        ).awaitSuccess().parseAs()
    }
}
