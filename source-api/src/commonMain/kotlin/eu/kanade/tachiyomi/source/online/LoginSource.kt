package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source

public interface LoginSource : Source {
    public val requiresLogin: Boolean

    public val twoFactorAuth: AuthSupport

    public fun isLogged(): Boolean

    public fun getUsername(): String

    public fun getPassword(): String

    public suspend fun login(username: String, password: String, twoFactorCode: String?): Boolean = false

    public suspend fun login(authCode: String): Boolean = false

    public suspend fun logout(): Boolean

    public enum class AuthSupport {
        NOT_SUPPORTED,
        SUPPORTED,
        REQUIRED,
    }
}
