package eu.kanade.tachiyomi.extension.util

import android.app.Application
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import mihon.test.CapturingLogcat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExtensionLoaderHeaderTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val logcat = CapturingLogcat()

    @Before
    fun setUp() {
        logcat.install()
    }

    @After
    fun tearDown() = logcat.uninstall()

    private fun header(
        metaData: Bundle? = extensionMetaData(),
        versionName: String? = "1.6.0",
        versionCode: Int = 7,
    ): ExtensionLoader.ExtensionHeader? = ExtensionLoader.extensionHeader(
        context = context,
        pkgInfo = extensionPackage(metaData = metaData, versionName = versionName, versionCode = versionCode),
    )

    @Test
    fun aCompleteManifest() {
        val header = header()
        header?.name shouldBe "Ext Name"
        header?.versionName shouldBe "1.6.0"
        header?.versionCode shouldBe 7L
        header?.libVersion shouldBe 1.6
        header?.isNsfw shouldBe false
    }

    @Test
    fun theNameFallsBackToTheLabel() {
        val header = header(metaData = extensionMetaData(name = null))
        header?.name shouldBe "eu.kanade.tachiyomi.extension.test"
    }

    @Test
    fun aMissingVersionName() {
        header(versionName = null).shouldBeNull()
        header(versionName = "").shouldBeNull()
        logcat.messages shouldBe List(2) { "Missing versionName for extension Ext Name" }
    }

    @Test
    fun theLibVersionFallsBack() {
        header(metaData = extensionMetaData(libVersion = 0f), versionName = "1.4.7")?.libVersion shouldBe 1.4
    }

    @Test
    fun anUnparseableLibVersion() {
        header(metaData = extensionMetaData(libVersion = 0f), versionName = "nope").shouldBeNull()
    }

    @Test
    fun anUnsupportedLibVersion() {
        header(metaData = extensionMetaData(libVersion = 1.5f)).shouldBeNull()
        logcat.messages.single() shouldBe "Lib version is 1.5, while only version(s) 1.4, 1.6 are supported"
    }

    @Test
    fun nsfwFromEitherFlag() {
        header(metaData = extensionMetaData(contentWarning = 1))?.isNsfw shouldBe true
        header(metaData = extensionMetaData(nsfw = 1))?.isNsfw shouldBe true
        header(metaData = extensionMetaData(nsfw = 2))?.isNsfw shouldBe false
    }

    @Test
    fun theHeaderDataClass() {
        val header = ExtensionLoader.ExtensionHeader(
            name = "n",
            versionName = "v",
            versionCode = 1L,
            libVersion = 1.6,
            isNsfw = false,
        )
        header.copy(isNsfw = true).isNsfw shouldBe true
        (header == header.copy()) shouldBe true
        header.hashCode() shouldBe header.copy().hashCode()
        header.toString().contains("ExtensionHeader") shouldBe true
    }

    @Test
    fun theSupportedLibVersions() {
        ExtensionLoader.SUPPORTED_LIB_VERSIONS shouldBe listOf(1.4, 1.6)
        ExtensionLoader.PRIVATE_EXTENSION_EXTENSION shouldBe "ext"
        (ExtensionLoader.PACKAGE_FLAGS > 0) shouldBe true
    }
}
