package exh.util

import eu.kanade.tachiyomi.util.asJsoup
import exh.log.xLogW
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Document

internal fun Response.interceptAsHtml(block: (Document) -> Unit): Response {
    return if (body.contentType()?.type == "text" &&
        body.contentType()?.subtype == "html"
    ) {
        val bodyString = body.string()
        val rebuiltResponse = newBuilder()
            .body(bodyString.toResponseBody(body.contentType()))
            .build()
        try {
            // Search for captcha
            val parsed = asJsoup(html = bodyString)
            block(parsed)
        } catch (expected: Throwable) {
            // Ignore all errors
            xLogW("Interception error!", expected)
        } finally {
            close()
        }

        rebuiltResponse
    } else {
        this
    }
}
