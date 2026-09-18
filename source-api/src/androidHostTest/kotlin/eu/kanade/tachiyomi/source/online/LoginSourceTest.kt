package eu.kanade.tachiyomi.source.online

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** A [LoginSource] with stored credentials that keeps both login defaults. */
private class LoginStubSource : StubSource(), LoginSource {
    override val requiresLogin: Boolean = true
    override val twoFactorAuth: LoginSource.AuthSupport = LoginSource.AuthSupport.SUPPORTED

    override fun isLogged(): Boolean = true

    override fun getUsername(): String = "user"

    override fun getPassword(): String = "pass"

    override suspend fun logout(): Boolean = true
}

/** The login defaults of [LoginSource] and its second-factor enum. */
internal class LoginSourceTest {
    private val source = LoginStubSource()

    @Test
    fun credentialLoginDefaultsToFalse() = runTest {
        source.login("user", "pass", null) shouldBe false
        source.login("user", "pass", "123456") shouldBe false
    }

    @Test
    fun authCodeLoginDefaultsToFalse() = runTest {
        source.login("code") shouldBe false
    }

    @Test
    fun implementationMembersExposed() = runTest {
        source.requiresLogin shouldBe true
        source.twoFactorAuth shouldBe LoginSource.AuthSupport.SUPPORTED
        source.isLogged() shouldBe true
        source.getUsername() shouldBe "user"
        source.getPassword() shouldBe "pass"
        source.logout() shouldBe true
    }

    @Test
    fun authSupportListsThreeLevels() {
        LoginSource.AuthSupport.entries shouldContainExactly listOf(
            LoginSource.AuthSupport.NOT_SUPPORTED,
            LoginSource.AuthSupport.SUPPORTED,
            LoginSource.AuthSupport.REQUIRED,
        )
        LoginSource.AuthSupport.valueOf("REQUIRED") shouldBe LoginSource.AuthSupport.REQUIRED
    }
}
