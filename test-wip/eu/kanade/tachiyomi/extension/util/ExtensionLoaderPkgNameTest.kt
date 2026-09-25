package eu.kanade.tachiyomi.extension.util

import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.nulls.shouldBeNull
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
import java.io.File

private const val PKG = "pkg.named"

@RunWith(RobolectricTestRunner::class)
internal class ExtensionLoaderPkgNameTest {
    private val packages = FakePackages()
    private val context = packages.context
    private val logcat = CapturingLogcat()
    private val trustExtension = mockk<TrustExtension>()

    @Before
    fun setUp() {
        logcat.install()
        mockkObject(ExtensionLoader)
        every { ExtensionLoader.trustExtension } returns trustExtension
        every { ExtensionLoader.loadNsfwSource } returns true
        coEvery { trustExtension.isTrusted(any(), any()) } returns true
        mockkStatic("eu.kanade.tachiyomi.extension.util.ExtensionLoaderSourcesKt")
        every { ExtensionLoader.loadSources(any(), any(), any(), any()) } returns listOf(
            mockk<Source> { every { lang } returns "en" },
        )
        ExtensionLoader.getPrivateExtensionDir(context).mkdirs()
    }

    @After
    fun tearDown() {
        packages.cleanUp()
        unmockkAll()
        logcat.uninstall()
    }

    private fun privateFile(sourceDir: String? = null, versionCode: Int = 1): File {
        val file = File(ExtensionLoader.getPrivateExtensionDir(context), "$PKG.ext")
        file.writeText("apk")
        packages.registerArchive(
            path = file.absolutePath,
            info = extensionPackage(pkgName = PKG, sourceDir = sourceDir, versionCode = versionCode),
        )
        return file
    }

    @Test
    fun anUnknownPackage() = runTest {
        ExtensionLoader.loadExtensionFromPkgName(context, PKG) shouldBe LoadResult.Error
        logcat.messages.single() shouldBe "Extension package is not found ($PKG)"
    }

    @Test
    fun aPrivatePackage() = runTest {
        privateFile()
        val extension = (ExtensionLoader.loadExtensionFromPkgName(context, PKG) as LoadResult.Success).extension
        extension.pkgName shouldBe PKG
        extension.isShared shouldBe false
    }

    @Test
    fun aPrivatePackageKeepsItsPaths() = runTest {
        privateFile(sourceDir = "/data/app/kept.apk")
        ExtensionLoader.getExtensionPackageInfo(context, PKG)?.applicationInfo?.sourceDir shouldBe
            "/data/app/kept.apk"
    }

    @Test
    fun aSharedPackage() = runTest {
        packages.installShared(extensionPackage(pkgName = PKG))
        val extension = (ExtensionLoader.loadExtensionFromPkgName(context, PKG) as LoadResult.Success).extension
        extension.isShared shouldBe true
    }

    @Test
    fun aSharedNonExtension() {
        packages.installShared(extensionPackage(pkgName = PKG, feature = false))
        ExtensionLoader.getExtensionPackageInfo(context, PKG).shouldBeNull()
    }

    @Test
    fun anUnregisteredPrivateFile() {
        File(ExtensionLoader.getPrivateExtensionDir(context), "$PKG.ext").writeText("apk")
        ExtensionLoader.getExtensionPackageInfo(context, PKG).shouldBeNull()
    }

    @Test
    fun theNewerOfTheTwoPackages() {
        packages.installShared(extensionPackage(pkgName = PKG, versionCode = 1))
        privateFile(versionCode = 9)
        ExtensionLoader.getExtensionPackageInfo(context, PKG)?.let {
            @Suppress("DEPRECATION")
            it.versionCode
        } shouldBe 9
    }

    @Test
    fun thePackageInfoOfAnUnknownPkg() {
        ExtensionLoader.getExtensionPackageInfo(context, "pkg.missing").shouldBeNull()
    }
}
