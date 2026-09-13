package eu.kanade.tachiyomi.util

import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Text of the first element matching [css], or [defaultValue] when nothing matches. */
public fun Element.selectText(css: String, defaultValue: String? = null): String? =
    select(css).first()?.text() ?: defaultValue

/** Text of the first element matching [css] parsed as an Int, or [defaultValue]. */
public fun Element.selectInt(css: String, defaultValue: Int = 0): Int =
    select(css).first()?.text()?.toInt() ?: defaultValue

/** The attribute named [css], or the element text when [css] is `"text"`. */
public fun Element.attrOrText(css: String): String = if (css != "text") attr(css) else text()

/**
 * Returns a Jsoup document for this response.
 * @param html the body of the response. Use only if the body was read before calling this method.
 */
public fun Response.asJsoup(html: String? = null): Document =
    Jsoup.parse(html ?: body.string(), request.url.toString())
