package eu.kanade.tachiyomi.network

import io.kotest.matchers.types.shouldBeInstanceOf
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.dnsoverhttps.DnsOverHttps
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import java.lang.reflect.InvocationTargetException

internal const val TEST_URL: String = "https://example.com/page"

/** A response to [request] with [code], [body] and [headers], built without any network. */
internal fun cannedResponse(
    request: Request,
    code: Int = 200,
    body: String = "",
    headers: Headers = Headers.headersOf(),
): Response = Response.Builder()
    .request(request)
    .protocol(Protocol.HTTP_1_1)
    .code(code)
    .message("canned")
    .headers(headers)
    .body(body.toResponseBody())
    .build()

/** Like [cannedResponse] but carrying a network response, so callers treat it as uncached. */
internal fun networkResponse(request: Request, code: Int = 200): Response = cannedResponse(request, code)
    .newBuilder()
    .networkResponse(cannedResponse(request, code))
    .build()

/** The last interceptor of a test client: answers every request through its lambda and records each request. */
internal class CannedServer(private val respond: (Request) -> Response) : Interceptor {
    val requests: MutableList<Request> = mutableListOf()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        requests += request
        return respond(request)
    }
}

/** A client that runs [interceptors] in order and ends at [server]; no request ever leaves the JVM. */
internal fun clientOf(server: CannedServer, vararg interceptors: Interceptor): OkHttpClient {
    val builder = OkHttpClient.Builder()
    interceptors.forEach { builder.addInterceptor(it) }
    return builder.addInterceptor(server).build()
}

/** A body whose [isClosed] flips when the response is closed; its declared length defaults to unknown. */
internal class ClosableBody(private val text: String = "body", private val length: Long = -1L) : ResponseBody() {
    var isClosed: Boolean = false
        private set

    override fun contentType(): MediaType? = "text/plain".toMediaType()

    override fun contentLength(): Long = length

    override fun source(): BufferedSource = object : ForwardingSource(Buffer().writeUtf8(text)) {
        override fun close() {
            isClosed = true
            super.close()
        }
    }.buffer()
}

/** One [ProgressListener.update] call. */
internal data class ProgressUpdate(val bytesRead: Long, val contentLength: Long, val done: Boolean)

/** Records every progress callback. */
internal class RecordingListener : ProgressListener {
    val updates: MutableList<ProgressUpdate> = mutableListOf()

    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
        updates += ProgressUpdate(bytesRead, contentLength, done)
    }
}

/** The DNS-over-HTTPS resolver [configure] installs on a fresh client, by endpoint host. */
internal fun dohHost(configure: OkHttpClient.Builder.() -> OkHttpClient.Builder): String {
    val dns = OkHttpClient.Builder().configure().build().dns
    return dns.shouldBeInstanceOf<DnsOverHttps>().url.host
}

/**
 * Invokes the static [name] of the class [className] whose parameters accept [args] in order, unwrapping the
 * reflective wrapper so the member's own exception surfaces. Reaches deprecated top-level functions without
 * the deprecation warning the module compiles as an error.
 */
internal fun invokeStatic(className: String, name: String, args: List<Any?>): Any? {
    val methods = Class.forName(className).declaredMethods
    val method = methods.first { candidate ->
        candidate.name == name &&
            candidate.parameterCount == args.size &&
            candidate.parameterTypes.zip(args).all { (type, arg) -> arg == null || type.accepts(arg) }
    }
    method.isAccessible = true
    return try {
        method.invoke(null, *args.toTypedArray())
    } catch (e: InvocationTargetException) {
        throw e.targetException
    }
}

private fun Class<*>.accepts(arg: Any): Boolean = when (this) {
    Int::class.javaPrimitiveType -> arg is Int
    Long::class.javaPrimitiveType -> arg is Long
    Boolean::class.javaPrimitiveType -> arg is Boolean
    else -> isInstance(arg)
}
