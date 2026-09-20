package eu.kanade.tachiyomi.source.online

import io.kotest.matchers.shouldBe
import okhttp3.Headers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The reflection contract Keiyoushi's `KeiSource` (extensions-lib 1.6) relies on: a
 * `Lazy` field named `headers$delegate` declared on [HttpSource] itself, which it replaces in
 * its `init` block to make `headers` non-lazy. Moving the property to a superclass keeps every
 * compile-time caller working and breaks every lib-1.6 extension at load time.
 */
internal class HttpSourceHeadersAbiTest {
    private val harness = SourceHarness()
    private val source = BareHttpSource()

    @BeforeEach
    fun setUp() = harness.install()

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun delegateFieldLivesOnHttpSource() {
        val field = HttpSource::class.java.getDeclaredField("headers\$delegate")
        Lazy::class.java.isAssignableFrom(field.type) shouldBe true
    }

    @Test
    fun swappedDelegateDrivesHeaders() {
        val swapped = Headers.headersOf("X-Kei", "swapped")
        val delegate = object : Lazy<Headers> {
            override val value: Headers get() = swapped
            override fun isInitialized(): Boolean = true
        }
        HttpSource::class.java.getDeclaredField("headers\$delegate").apply {
            isAccessible = true
            set(source, delegate)
        }
        source.headers shouldBe swapped
    }
}
