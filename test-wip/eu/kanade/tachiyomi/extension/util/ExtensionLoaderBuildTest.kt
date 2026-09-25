package eu.kanade.tachiyomi.extension.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import mihon.test.CapturingLogcat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExtensionLoaderBuildTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val logcat = CapturingLogcat()
    private val trustExtension = mockk<TrustExtension>()
    private val header = ExtensionLoader.ExtensionHeader(
        name = "Ext Name",
        versionName = "1.6.0",
        versionCode = 4L,
        libVersion = 1.6,
        isNsfw = false,
    )

    @Before
    fun setUp() {
        logcat.install()
        mockkObject(ExtensionLoader)
        every { ExtensionLoader.trustExtension } returns trustExtension
        coEvery { trustExtension.isTrusted(any(), any()) } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
        logcat.uninstall()
    }

    private fun sources(vararg langs: String) {
        mockkStatic("eu.kanade.tachiyomi.extension.util.ExtensionLoaderSourcesKt")
        every { ExtensionLoader.loadSources(any(), any(), any(), any()) } returns langs.map { language ->
            val source = mockk<Source>()
            every { source.lang } returns language
            source
        }
    }

    private fun build(isNsfw: Boolean = false, isShared: Boolean = true): LoadResult =
        ExtensionLoader.buildExtension(
            context = context,
            extensionInfo = ExtensionLoader.ExtensionInfo(
                packageInfo = extensionPackage(metaData = extensionMetaData(sourceFactory = "Factory")),
                isShared = isShared,
            ),
            header = header.copy(isNsfw = isNsfw),
        )

    @Test
    fun anUnsignedPackageIsNotLoaded() = runTest {
        ExtensionLoader.checkTrust(
            pkgInfo = extensionPackage(signatures = null),
            header = header,
        ) shouldBe LoadResult.Error
        ExtensionLoader.checkTrust(
            pkgInfo = extensionPackage(signatures = emptyList()),
            header = header,
        ) shouldBe LoadResult.Error
        logcat.messages.distinct().single() shouldBe
            "Package eu.kanade.tachiyomi.extension.test isn't signed"
    }

    @Test
    fun anUntrustedPackage() = runTest {
        coEvery { trustExtension.isTrusted(any(), any()) } returns false
        val result = ExtensionLoader.checkTrust(pkgInfo = extensionPackage(), header = header)
        val untrusted = (result as LoadResult.Untrusted).extension
        untrusted.pkgName shouldBe "eu.kanade.tachiyomi.extension.test"
        untrusted.signatureHash.length shouldBe 64
        untrusted.versionCode shouldBe 4L
        logcat.messages.single() shouldBe "Extension eu.kanade.tachiyomi.extension.test isn't trusted"
    }

    @Test
    fun aTrustedPackageMayBeLoaded() = runTest {
        ExtensionLoader.checkTrust(pkgInfo = extensionPackage(), header = header) shouldBe null
    }

    @Test
    fun nsfwIsRefusedWhenNotAllowed() {
        every { ExtensionLoader.loadNsfwSource } returns false
        build(isNsfw = true) shouldBe LoadResult.Error
        logcat.messages.single() shouldBe "NSFW extension eu.kanade.tachiyomi.extension.test not allowed"
    }

    @Test
    fun sourcesThatFailToLoad() {
        every { ExtensionLoader.loadNsfwSource } returns true
        mockkStatic("eu.kanade.tachiyomi.extension.util.ExtensionLoaderSourcesKt")
        every { ExtensionLoader.loadSources(any(), any(), any(), any()) } returns null
        build() shouldBe LoadResult.Error
    }

    @Test
    fun aPackageWithoutSources() {
        every { ExtensionLoader.loadNsfwSource } returns false
        sources()
        val extension = (build() as LoadResult.Success).extension
        extension.lang shouldBe ""
        extension.sources.isEmpty() shouldBe true
        extension.isShared shouldBe true
        extension.pkgFactory shouldBe "Factory"
    }

    @Test
    fun aSingleLanguagePackage() {
        every { ExtensionLoader.loadNsfwSource } returns true
        sources("en")
        val extension = (build(isNsfw = true, isShared = false) as LoadResult.Success).extension
        extension.lang shouldBe "en"
        extension.isNsfw shouldBe true
        extension.isShared shouldBe false
        extension.name shouldBe "Ext Name"
        extension.versionCode shouldBe 4L
    }

    @Test
    fun aMultiLanguagePackage() {
        every { ExtensionLoader.loadNsfwSource } returns true
        sources("en", "fr")
        (build() as LoadResult.Success).extension.lang shouldBe "all"
    }

    @Test
    fun nsfwNotAllowedIsAlwaysAnError() {
        ExtensionLoader.nsfwNotAllowed("pkg") shouldBe LoadResult.Error
        logcat.messages.single() shouldBe "NSFW extension pkg not allowed"
    }
}
