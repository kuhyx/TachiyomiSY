package eu.kanade.presentation.util

import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.network.HttpException
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.data.source.NoResultsException
import tachiyomi.domain.source.model.SourceNotInstalledException
import java.io.IOException
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
internal class ExceptionFormatterTest {

    private val app = ApplicationProvider.getApplicationContext<Context>()

    private fun contextWith(online: Boolean): Context {
        val manager = mockk<ConnectivityManager>()
        val capabilities = mockk<NetworkCapabilities>()
        every { manager.activeNetwork } returns mockk<Network>()
        every { manager.getNetworkCapabilities(any()) } returns capabilities
        every { capabilities.hasTransport(any()) } returns online
        return object : ContextWrapper(app) {
            override fun getSystemService(name: String): Any? =
                if (name == CONNECTIVITY_SERVICE) manager else super.getSystemService(name)
        }
    }

    private fun Throwable.format(online: Boolean = true): String = with(contextWith(online)) { formattedMessage }

    @Test
    fun httpErrorsNameTheCode() {
        HttpException(404).format() shouldBe "HTTP 404, check website in WebView"
    }

    @Test
    fun unknownHostOffline() {
        UnknownHostException("a.b").format(online = false) shouldBe "No Internet connection"
    }

    @Test
    fun unknownHostOnline() {
        UnknownHostException("a.b").format() shouldBe "Couldn't reach a.b"
        UnknownHostException().format() shouldBe "Couldn't reach "
    }

    @Test
    fun sourceErrorsHaveTheirOwnText() {
        NoResultsException().format() shouldBe "No results found"
        SourceNotInstalledException().format() shouldBe "Source not found"
    }

    @Test
    fun plainExceptionsShowTheMessage() {
        Exception("plain").format() shouldBe "plain"
        IOException("io").format() shouldBe "io"
        Exception().format() shouldBe "Exception"
        IOException().format() shouldBe "IOException"
    }

    @Test
    fun otherClassesArePrefixed() {
        IllegalStateException("boom").format() shouldBe "IllegalStateException: boom"
    }
}
