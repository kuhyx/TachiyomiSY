package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source

/** A source with a user account; credentials live in the source's preferences. */

public interface LoginSource : Source {
    /** True when browsing needs an account. */
    public val requiresLogin: Boolean

    /** Which second factor the login supports. */

    public val twoFactorAuth: AuthSupport

    /** True when credentials are stored. */

    public fun isLogged(): Boolean

    /** The stored username. */

    public fun getUsername(): String

    /** The stored password. */

    public fun getPassword(): String

    /** Logs in with credentials; returns whether it succeeded. */

    public suspend fun login(username: String, password: String, twoFactorCode: String?): Boolean = false

    /** Logs in with an OAuth-style code; returns whether it succeeded. */

    public suspend fun login(authCode: String): Boolean = false

    /** Clears the session; returns whether it succeeded. */

    public suspend fun logout(): Boolean

    /** Second-factor support of a source. */

    public enum class AuthSupport {
        /** No second factor. */
        NOT_SUPPORTED,

        /** A second factor may be given. */
        SUPPORTED,

        /** A second factor is mandatory. */
        REQUIRED,
    }
}
