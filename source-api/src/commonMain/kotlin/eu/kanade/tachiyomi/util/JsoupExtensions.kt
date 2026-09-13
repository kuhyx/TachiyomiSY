package eu.kanade.tachiyomi.util

import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

public fun Element.selectText(css: String, defaultValue: String? = null): String? {
    return select(css).first()?.text() ?: defaultValue
}

public fun Element.selectInt(css: String, defaultValue: Int = 0): Int {
    return select(css).first()?.text()?.toInt() ?: defaultValue
}

public fun Element.attrOrText(css: String): String {
    return if (css != "text") attr(css) else text()
}

/**
 * Returns a Jsoup document for this response.
 * @param html the body of the response. Use only if the body was read before calling this method.
 */
public fun Response.asJsoup(html: String? = null): Document {
    return Jsoup.parse(html ?: body.string(), request.url.toString())
}
