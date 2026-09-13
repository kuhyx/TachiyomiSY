package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/** Adds the configured User-Agent to requests that do not set one. */
public class UserAgentInterceptor(
    private val defaultUserAgentProvider: () -> String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        return if (originalRequest.header(USER_AGENT).isNullOrEmpty()) {
            val newRequest = originalRequest
                .newBuilder()
                .removeHeader(USER_AGENT)
                .addHeader(USER_AGENT, defaultUserAgentProvider())
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}

private const val USER_AGENT = "User-Agent"
