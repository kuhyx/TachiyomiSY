package eu.kanade.tachiyomi.source.online.all

import android.net.Uri
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.all.EHentai.ParsedManga
import eu.kanade.tachiyomi.util.asJsoup
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.util.nullIfBlank
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import tachiyomi.core.common.util.lang.withIOContext
import java.io.IOException

internal fun EHentai.realImageUrlParse(response: Response, page: Page): String {
    with(response.asJsoup()) {
        val currentImage = getElementById("img")!!.attr("src")
        // Each press of the retry button will choose another server
        select("#loadfail").attr("onclick").nullIfBlank()?.let {
            page.url = addParam(page.url, "nl", it.substring(it.indexOf('\'') + 1 until it.lastIndexOf('\'')))
        }
        if (currentImage == "https://ehgt.org/g/509.gif") {
            throw IOException("Exceeded page quota")
        }
        return currentImage
    }
}

internal suspend fun EHentai.fetchFavorites(): Pair<List<ParsedManga>, List<String>> {
    val favoriteUrl = "$baseUrl/favorites.php"
    val result = mutableListOf<ParsedManga>()
    var page = 1

    var favNames: List<String>? = null

    do {
        val response2 = withIOContext {
            client.newCall(
                exGet(
                    favoriteUrl,
                    next = page,
                    cacheControl = CacheControl.FORCE_NETWORK,
                ),
            ).await()
        }
        val doc = response2.asJsoup()

        // Parse favorites
        val parsed = galleryListParser.parse(doc)
        result += parsed.first

        // Parse fav names
        if (favNames == null) {
            favNames = doc.select(".fp:not(.fps)").mapNotNull {
                it.child(2).text()
            }
        }
        // Next page

        page = parsed.first.lastOrNull()?.manga?.url?.let { EHentaiSearchMetadata.galleryId(it) }?.toInt() ?: 0
    } while (parsed.second != null)

    return Pair(result.toList(), favNames.orEmpty())
}

internal fun EHentai.getGalleryUrlFromPage(uri: Uri): String {
    val lastSplit = uri.pathSegments.last().split("-")
    val pageNum = lastSplit.last()
    val gallery = lastSplit.first()
    val pageToken = uri.pathSegments.elementAt(1)

    val json = buildJsonObject {
        put("method", "gtoken")
        put(
            "pagelist",
            buildJsonArray {
                add(
                    buildJsonArray {
                        add(gallery.toInt())
                        add(pageToken)
                        add(pageNum.toInt())
                    },
                )
            },
        )
    }

    val outJson = Json.decodeFromString<JsonObject>(
        client.newCall(
            Request.Builder()
                .url(EHentai.EH_API_BASE)
                .post(json.toString().toRequestBody(EHentai.JSON))
                .build(),
        ).execute().body.string(),
    )

    val obj = outJson["tokenlist"]!!.jsonArray.first().jsonObject
    return "${uri.scheme}://${uri.host}/g/${obj["gid"]!!.jsonPrimitive.int}/${
        obj["token"]!!.jsonPrimitive.content
    }/"
}

internal suspend fun EHentai.mangaUrlFromUri(uri: Uri): String? {
    return when (uri.pathSegments.firstOrNull()) {
        "g" -> {
            // Is already gallery page, do nothing
            uri.toString()
        }
        "s" -> {
            // Is page, fetch gallery token and use that
            getGalleryUrlFromPage(uri)
        }
        else -> {
            null
        }
    }
}
