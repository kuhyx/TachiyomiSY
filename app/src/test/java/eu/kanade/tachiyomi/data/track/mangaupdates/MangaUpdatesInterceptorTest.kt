package eu.kanade.tachiyomi.data.track.mangaupdates

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.TrackerHarness
import eu.kanade.tachiyomi.data.track.anilist.response
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

internal class MangaUpdatesInterceptorTest {

    private lateinit var mangaUpdates: MangaUpdates
    private val original = Request.Builder().url("https://api.mangaupdates.com/v1/account/profile").build()
    private val proceeded = mutableListOf<Request>()

    private val chain = mockk<Interceptor.Chain>().also { chain ->
        every { chain.request() } returns original
        every { chain.proceed(any()) } answers {
            val request = firstArg<Request>()
            proceeded += request
            response(request, 200, "{}")
        }
    }

    @BeforeEach
    fun setUp() {
        TrackerHarness.start()
        mangaUpdates = MangaUpdates(TrackerManager.MANGAUPDATES)
    }

    @AfterEach
    fun tearDown() {
        TrackerHarness.stop()
    }

    @Test
    fun unauthenticatedFails() {
        shouldThrow<IOException> { MangaUpdatesInterceptor(mangaUpdates).intercept(chain) }.message shouldBe
            "Not authenticated with MangaUpdates"
    }

    @Test
    fun sessionTokenAddsHeaders() {
        val interceptor = MangaUpdatesInterceptor(mangaUpdates)
        interceptor.newAuth("sess")
        interceptor.intercept(chain).code shouldBe 200
        val sent = proceeded.single()
        sent.header("Authorization") shouldBe "Bearer sess"
        checkNotNull(sent.header("User-Agent")).startsWith("TachiyomiSY v") shouldBe true
    }

    @Test
    fun storedSessionIsRestored() {
        mangaUpdates.saveCredentials("555", "stored")
        MangaUpdatesInterceptor(mangaUpdates).intercept(chain)
        proceeded.single().header("Authorization") shouldBe "Bearer stored"
    }

    @Test
    fun clearingAuthFails() {
        val interceptor = MangaUpdatesInterceptor(mangaUpdates)
        interceptor.newAuth("sess")
        interceptor.newAuth(null)
        shouldThrow<IOException> { interceptor.intercept(chain) }
    }
}
