package eu.kanade.tachiyomi.extension.util

import android.os.Build
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import mihon.test.CapturingLogcat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class ExtensionLoaderTest {
    private val packages = FakePackages()
    private val context = packages.context
    private val logcat = CapturingLogcat()
    private val trustExtension = mockk<TrustExtension>()

    private val sdk = Build.VERSION.SDK_INT

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
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        packages.cleanUp()
        unmockkAll()
        logcat.uninstall()
    }

    // A private extension file named after pkgName, registered with the package manager.
    private fun privateExtension(
        pkgName: String,
        sourceDir: String? = null,
        readOnly: Boolean = false,
        extension: String = "ext",
    ): File {
        val file = File(ExtensionLoader.getPrivateExtensionDir(context), "$pkgName.$extension")
        file.writeText("apk")
        if (readOnly) {
            file.setReadOnly()
        }
        packages.registerArchive(
            path = file.absolutePath,
            info = extensionPackage(pkgName = pkgName, sourceDir = sourceDir),
        )
        return file
    }

    private fun loadedNames(): List<String> = ExtensionLoader.loadExtensions(context)
        .filterIsInstance<LoadResult.Success>()
        .map { it.extension.pkgName }

    @Test
    fun noExtensionsAtAll() {
        ExtensionLoader.getPrivateExtensionDir(context).deleteRecursively()
        ExtensionLoader.loadExtensions(context) shouldContainExactly emptyList()
    }

    @Test
    fun aSharedExtension() {
        packages.installShared(extensionPackage(pkgName = "pkg.shared"))
        loadedNames() shouldContainExactly listOf("pkg.shared")
    }

    @Test
    fun aSharedExtensionBeforeTiramisu() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.S)
        packages.installShared(extensionPackage(pkgName = "pkg.shared"))
        loadedNames() shouldContainExactly listOf("pkg.shared")
    }

    @Test
    fun aWritablePrivateIsLocked() {
        val file = privateExtension("pkg.private")
        loadedNames() shouldContainExactly listOf("pkg.private")
        file.isOwnerWritable() shouldBe false
    }

    @Test
    fun aReadOnlyPrivateKeepsPaths() {
        privateExtension("pkg.private", sourceDir = "/data/app/kept.apk", readOnly = true)
        loadedNames() shouldContainExactly listOf("pkg.private")
    }

    @Test
    fun otherFilesAreSkipped() {
        privateExtension("pkg.other", extension = "apk")
        File(ExtensionLoader.getPrivateExtensionDir(context), "sub").mkdirs()
        ExtensionLoader.loadExtensions(context) shouldContainExactly emptyList()
    }

    @Test
    fun anUnregisteredFileIsSkipped() {
        File(ExtensionLoader.getPrivateExtensionDir(context), "pkg.unknown.ext").writeText("apk")
        ExtensionLoader.loadExtensions(context) shouldContainExactly emptyList()
    }

    @Test
    fun theSharedPackageWins() {
        packages.installShared(extensionPackage(pkgName = "pkg.both"))
        privateExtension("pkg.both")
        loadedNames() shouldContainExactly listOf("pkg.both")
    }

    @Test
    fun anUnusableHeaderIsAnError() {
        packages.installShared(
            extensionPackage(pkgName = "pkg.broken", versionName = null),
        )
        ExtensionLoader.loadExtensions(context) shouldContainExactly listOf(LoadResult.Error)
    }

    @Test
    fun anUntrustedExtensionIsReported() {
        coEvery { trustExtension.isTrusted(any(), any()) } returns false
        packages.installShared(extensionPackage(pkgName = "pkg.untrusted"))
        val results = ExtensionLoader.loadExtensions(context)
        results.filterIsInstance<LoadResult.Untrusted>().single().extension.pkgName shouldBe "pkg.untrusted"
    }
}
