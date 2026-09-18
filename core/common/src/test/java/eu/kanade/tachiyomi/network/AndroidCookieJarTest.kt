package eu.kanade.tachiyomi.network

import android.webkit.CookieManager
import android.webkit.ValueCallback
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AndroidCookieJarTest {
    private val url = TEST_URL.toHttpUrl()
    private val manager: CookieManager = mockk()
    private lateinit var jar: AndroidCookieJar

    @BeforeEach
    fun setUp() {
        mockkStatic(CookieManager::class)
        every { CookieManager.getInstance() } returns manager
        every { manager.setCookie(any(), any()) } just Runs
        jar = AndroidCookieJar()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun saveWritesEachCookie() {
        val cookies = listOf(Cookie.parse(url, "a=1"), Cookie.parse(url, "b=2; Path=/")).map { checkNotNull(it) }
        jar.saveFromResponse(url, cookies)
        verify { manager.setCookie(TEST_URL, "a=1; path=/") }
        verify { manager.setCookie(TEST_URL, "b=2; path=/") }
    }

    @Test
    fun loadParsesStoredCookies() {
        every { manager.getCookie(TEST_URL) } returns "a=1; b=2"
        jar.loadForRequest(url).map { it.name to it.value } shouldContainExactly listOf("a" to "1", "b" to "2")
    }

    @Test
    fun getSkipsUnparseableEntries() {
        every { manager.getCookie(TEST_URL) } returns "a=1; =broken"
        jar.get(url).map { it.name } shouldContainExactly listOf("a")
    }

    @Test
    fun getIsEmptyWhenStoreHasNothing() {
        every { manager.getCookie(TEST_URL) } returns null
        jar.get(url) shouldBe emptyList()
        every { manager.getCookie(TEST_URL) } returns ""
        jar.get(url) shouldBe emptyList()
    }

    @Test
    fun removeExpiresAllByDefault() {
        every { manager.getCookie(TEST_URL) } returns "a=1; b=2"
        jar.remove(url) shouldBe 2
        verify { manager.setCookie(TEST_URL, "a=;Max-Age=-1") }
        verify { manager.setCookie(TEST_URL, " b=;Max-Age=-1") }
    }

    @Test
    fun removeFiltersByNameAndMaxAge() {
        every { manager.getCookie(TEST_URL) } returns "a=1;b=2;c=3"
        jar.remove(url, listOf("a", "c"), 0) shouldBe 2
        verify { manager.setCookie(TEST_URL, "a=;Max-Age=0") }
        verify { manager.setCookie(TEST_URL, "c=;Max-Age=0") }
        verify(exactly = 0) { manager.setCookie(TEST_URL, "b=;Max-Age=0") }
    }

    @Test
    fun removeIsZeroWithoutCookies() {
        every { manager.getCookie(TEST_URL) } returns null
        jar.remove(url, listOf("a")) shouldBe 0
        verify(exactly = 0) { manager.setCookie(any(), any()) }
    }

    @Test
    fun removeAllClearsTheStore() {
        every { manager.removeAllCookies(any()) } answers {
            firstArg<ValueCallback<Boolean>>().onReceiveValue(true)
        }
        jar.removeAll()
        verify { manager.removeAllCookies(any()) }
    }
}
