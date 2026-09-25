package eu.kanade.tachiyomi.data.sync.service

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.TokenErrorResponse
import com.google.api.client.auth.oauth2.TokenResponseException
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.sync.SyncPreferences
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class GoogleDriveServiceTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val preferences = SyncPreferences(FlowPreferenceStore())
    private var logged = mutableListOf<String>()

    @Before
    fun setUp() {
        logged = captureLogcat()
        startKoin { modules(module { single { preferences } }) }
        mockkConstructor(Credential::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
        releaseLogcat()
    }

    private fun signIn() {
        preferences.googleDriveAccessToken.set("access")
        preferences.googleDriveRefreshToken.set("refresh")
    }

    private fun tokenError(code: String, message: String?): TokenResponseException {
        val error = mockk<TokenResponseException>(relaxed = true)
        every { error.details } returns TokenErrorResponse().setError(code)
        every { error.message } returns message
        return error
    }

    @Test
    fun signedOutHasNoDrive() {
        GoogleDriveService(SecretsContext(app)).driveService.shouldBeNull()
        preferences.googleDriveAccessToken.set("access")
        GoogleDriveService(SecretsContext(app)).driveService.shouldBeNull()
    }

    @Test
    fun signedInBuildsTheDrive() {
        signIn()
        GoogleDriveService(SecretsContext(app)).driveService.shouldNotBeNull().applicationName shouldBe "TachiyomiSY"
    }

    @Test
    fun signInOpensTheBrowser() {
        val intent = GoogleDriveService(SecretsContext(app)).getSignInIntent()
        intent.action shouldBe Intent.ACTION_VIEW
        (intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) shouldBe Intent.FLAG_ACTIVITY_NEW_TASK
        val url = intent.dataString.shouldNotBeNull()
        url shouldContain "client_id=cid"
        url shouldContain "access_type=offline"
        url shouldContain "approval_prompt=force"
    }

    @Test
    fun refreshNeedsARefreshToken() = runTest {
        shouldThrow<IllegalStateException> { GoogleDriveService(SecretsContext(app)).refreshToken() }.message shouldBe
            "Not signed in to Google Drive"
    }

    @Test
    fun refreshStoresTheNewToken() = runTest {
        signIn()
        every { anyConstructed<Credential>().refreshToken() } returns true
        every { anyConstructed<Credential>().accessToken } returns "fresh"
        val service = GoogleDriveService(SecretsContext(app))
        service.refreshToken()
        preferences.googleDriveAccessToken.get() shouldBe "fresh"
        service.driveService.shouldNotBeNull()
    }

    @Test
    fun invalidGrantAsksForSignIn() = runTest {
        signIn()
        val service = GoogleDriveService(SecretsContext(app))
        every { anyConstructed<Credential>().refreshToken() } throws tokenError("invalid_grant", "revoked")
        shouldThrow<Exception> { service.refreshToken() }.message shouldBe "revoked"
        every { anyConstructed<Credential>().refreshToken() } throws tokenError("invalid_grant", null)
        shouldThrow<Exception> { service.refreshToken() }.message shouldBe "Unknown error"
        logged.any { it.startsWith("Refresh token is invalid, prompt user to sign in again") } shouldBe true
    }

    @Test
    fun otherTokenErrorsDisableSync() = runTest {
        signIn()
        val service = GoogleDriveService(SecretsContext(app))
        every { anyConstructed<Credential>().refreshToken() } throws tokenError("server_error", "down")
        shouldThrow<Exception> { service.refreshToken() }.message shouldBe "down"
        every { anyConstructed<Credential>().refreshToken() } throws tokenError("server_error", null)
        shouldThrow<Exception> { service.refreshToken() }.message shouldBe "Unknown error"
        logged.contains("Failed to refresh access token down") shouldBe true
        logged.contains("Google Drive sync will be disabled") shouldBe true
    }

    @Test
    fun networkErrorsDisableSync() = runTest {
        signIn()
        val service = GoogleDriveService(SecretsContext(app))
        every { anyConstructed<Credential>().refreshToken() } throws IOException("offline")
        shouldThrow<Exception> { service.refreshToken() }.message shouldBe "offline"
        every { anyConstructed<Credential>().refreshToken() } throws IOException()
        shouldThrow<Exception> { service.refreshToken() }.message shouldBe "Unknown error"
    }

    @Test
    fun authorizationCodeSignsIn() {
        mockkConstructor(GoogleAuthorizationCodeTokenRequest::class)
        every { anyConstructed<GoogleAuthorizationCodeTokenRequest>().execute() } returns
            GoogleTokenResponse().setAccessToken("a").setRefreshToken("r")
        val activity = uiThreadActivity()
        var outcome = ""
        val service = GoogleDriveService(SecretsContext(app))
        service.handleAuthorizationCode("code", activity, onSuccess = { outcome = "ok" }, onFailure = { outcome = it })
        outcome shouldBe "ok"
        preferences.googleDriveRefreshToken.get() shouldBe "r"
        service.driveService.shouldNotBeNull()
    }

    @Test
    fun authFailuresAreReported() {
        mockkConstructor(GoogleAuthorizationCodeTokenRequest::class)
        every { anyConstructed<GoogleAuthorizationCodeTokenRequest>().execute() } returns
            GoogleTokenResponse().setAccessToken("a").setRefreshToken("r")
        val outcomes = mutableListOf<String>()
        listOf(IOException("no secrets"), IOException()).forEach { failure ->
            preferences.googleDriveAccessToken.set("")
            GoogleDriveService(SecretsContext(app, failOn = mapOf(2 to failure))).handleAuthorizationCode(
                authorizationCode = "code",
                activity = uiThreadActivity(),
                onSuccess = { outcomes += "ok" },
                onFailure = { outcomes += it },
            )
        }
        outcomes shouldBe listOf("no secrets", "Unknown error")
        GoogleDriveService.REDIRECT_URI shouldBe "eu.kanade.google.oauth:/oauth2redirect"
    }

    private fun uiThreadActivity(): Activity = mockk {
        every { runOnUiThread(any()) } answers { firstArg<Runnable>().run() }
    }
}
