package eu.kanade.tachiyomi.network

import okhttp3.Response

/**
 * Exception that handles HTTP codes considered not successful by OkHttp.
 * Use it to have a standardized error message in the app across the extensions.
 *
 * @see Response.isSuccessful
 * @since tachiyomix 1.6
 * @property code the HTTP status code.
 */
public class HttpException(public val code: Int) : IllegalStateException("HTTP error $code")
