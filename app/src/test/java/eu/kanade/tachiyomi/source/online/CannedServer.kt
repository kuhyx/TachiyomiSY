package eu.kanade.tachiyomi.source.online

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/** An [OkHttpClient] whose only interceptor answers every request from [body] and [code], recording each request. */
internal class CannedServer {
    val requests: MutableList<Request> = mutableListOf()
    var body: String = ""
    var code: Int = 200
    var headers: Map<String, String> = emptyMap()

    /** Bodies served in order; once drained, [body] answers. */
    val queue: ArrayDeque<String> = ArrayDeque()

    /** When set, picks the body per request (after [queue]); may throw to fail the call. */
    var answers: ((Request) -> String)? = null

    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            requests += request
            val builder = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("canned")
                .body((queue.removeFirstOrNull() ?: answers?.invoke(request) ?: body).toResponseBody())
            headers.forEach { (name, value) -> builder.addHeader(name, value) }
            builder.build()
        }
        .build()

    /** The recorded request at [index]. */
    fun request(index: Int = 0): Request = requests[index]
}
