package eu.kanade.tachiyomi.source.online.all

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import exh.util.trimOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Element
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val JPEG_QUALITY_LOSSLESS = 100
private const val THUMB_DOMAIN = "ehgt.org"
private const val BLANK_THUMB = "blank.gif"
private const val BLANK_PREVIEW_THUMB = "https://$THUMB_DOMAIN/g/$BLANK_THUMB"

/*
 * Page previews of an e-hentai gallery: the sprite-sheet thumbnails parsed off the gallery page,
 * and the interceptor that crops one out of the sheet when the reader asks for it.
 */

// Parse normal previews with regular expressions.
internal fun parseNormalPreview(element: Element): EHentaiThumbnailPreview {
    val imgElement = element.selectFirst("img")
    val index = imgElement?.attr("alt")?.toInt()
        ?: element.child(0).attr("title").removePrefix("Page ").substringBefore(":").toInt()
    val styleElement = if (imgElement != null) {
        element
    } else {
        element.child(0)
    }
    val styles = styleElement.attr("style").split(";").mapNotNull { it.trimOrNull() }
    val width = styles.first { it.startsWith("width:") }
        .removePrefix("width:")
        .removeSuffix("px")
        .toInt()

    val height = styles.first { it.startsWith("height:") }
        .removePrefix("height:")
        .removeSuffix("px")
        .toInt()

    val background = styles.first { it.startsWith("background:") }
        .removePrefix("background:")
        .split(" ")

    val url = background.first { it.startsWith("url(") }
        .removePrefix("url(")
        .removeSuffix(")")

    val widthOffset = background.first { it.startsWith("-") }
        .removePrefix("-")
        .removeSuffix("px")
        .toInt()

    return EHentaiThumbnailPreview(url, width, height, widthOffset, index)
}
internal data class EHentaiThumbnailPreview(
    val imageUrl: String,
    val width: Int,
    val height: Int,
    val widthOffset: Int,
    val index: Int,
) {
    fun toUrl(): String {
        return BLANK_PREVIEW_THUMB.toHttpUrl().newBuilder()
            .addQueryParameter("imageUrl", imageUrl)
            .addQueryParameter("width", width.toString())
            .addQueryParameter("height", height.toString())
            .addQueryParameter("widthOffset", widthOffset.toString())
            .build()
            .toString()
    }

    companion object {
        fun parseFromUrl(url: HttpUrl) = EHentaiThumbnailPreview(
            imageUrl = url.queryParameter("imageUrl")!!,
            width = url.queryParameter("width")!!.toInt(),
            height = url.queryParameter("height")!!.toInt(),
            widthOffset = url.queryParameter("widthOffset")!!.toInt(),
            index = -1,
        )
    }
}

internal class ThumbnailPreviewInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host != THUMB_DOMAIN || !request.url.pathSegments.contains(BLANK_THUMB)) {
            return chain.proceed(request)
        }

        val thumbnailPreview = EHentaiThumbnailPreview.parseFromUrl(request.url)
        val response = chain.proceed(request.newBuilder().url(thumbnailPreview.imageUrl).build())
        return if (response.isSuccessful) response.croppedTo(thumbnailPreview) else response
    }
}

// The site serves one sprite per gallery page; the preview is its slice at the given offset.
internal fun Response.croppedTo(thumbnailPreview: EHentaiThumbnailPreview): Response {
    val body = ByteArrayOutputStream()
        .use {
            val bitmap = BitmapFactory.decodeStream(body.byteStream())
                ?: throw IOException("Null bitmap($thumbnailPreview)")
            Bitmap.createBitmap(
                bitmap,
                thumbnailPreview.widthOffset,
                0,
                thumbnailPreview.width.coerceAtMost(bitmap.width - thumbnailPreview.widthOffset),
                thumbnailPreview.height.coerceAtMost(bitmap.height),
            ).compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY_LOSSLESS, it)
            it.toByteArray()
        }
        .toResponseBody("image/jpeg".toMediaType())

    return newBuilder().body(body).build()
}
