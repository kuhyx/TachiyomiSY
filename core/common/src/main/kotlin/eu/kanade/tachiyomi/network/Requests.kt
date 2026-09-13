@file:Suppress("FunctionName")

package eu.kanade.tachiyomi.network

import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit.MINUTES

private const val CACHE_MAX_AGE_MINUTES = 10
private val DEFAULT_CACHE_CONTROL = CacheControl.Builder().maxAge(CACHE_MAX_AGE_MINUTES, MINUTES).build()
private val DEFAULT_HEADERS = Headers.Builder().build()
private val DEFAULT_BODY: RequestBody = FormBody.Builder().build()

/** A GET request for [url] with [headers] and the default cache control. */
public fun GET(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request = GET(url.toHttpUrl(), headers, cache)

/**
 * @since extensions-lib 1.4
 */
public fun GET(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    return Request.Builder()
        .url(url)
        .headers(headers)
        .cacheControl(cache)
        .build()
}

/** A POST request for [url] with [headers] and the default cache control. */
public fun POST(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    return Request.Builder()
        .url(url)
        .post(body)
        .headers(headers)
        .cacheControl(cache)
        .build()
}

/** A PUT request for [url] with [headers] and the default cache control. */
public fun PUT(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    return Request.Builder()
        .url(url)
        .put(body)
        .headers(headers)
        .cacheControl(cache)
        .build()
}

/** A PATCH request for [url] with [headers] and the default cache control. */
public fun PATCH(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    return Request.Builder()
        .url(url)
        .patch(body)
        .headers(headers)
        .cacheControl(cache)
        .build()
}

/** A DELETE request for [url] with [headers] and the default cache control. */
public fun DELETE(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL,
): Request {
    return Request.Builder()
        .url(url)
        .delete(body)
        .headers(headers)
        .cacheControl(cache)
        .build()
}
