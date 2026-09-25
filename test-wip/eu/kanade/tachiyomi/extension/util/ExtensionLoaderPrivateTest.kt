package eu.kanade.tachiyomi.extension.util

import android.app.Application
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import mihon.test.CapturingLogcat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File

private const val PKG = "eu.kanade.tachiyomi.extension.test"

@RunWith(RobolectricTestRunner::class)
internal class ExtensionLoaderPrivateTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val logcat = CapturingLogcat()

    @Before
    fun setUp() {
        logcat.install()
        ExtensionLoader.getPrivateExtensionDir(context).mkdirs()
    }

    @After
    fun tearDown() {
        ExtensionLoader.getPrivateExtensionDir(context).deleteRecursively()
        logcat.uninstall()
    }

    // An apk file with [bytes] of content, registered with the package manager as [info].
    private fun apk(name: String, info: PackageInfo?, bytes: String? = "apk"): File {
        val file = File(context.cacheDir, name)
        bytes?.let { file.writeText(it) }
        shadowOf(context.packageManager).setPackageArchiveInfo(file.absolutePath, info)
        return file
    }

    // The file installPrivateExtensionFile copies a package to.
    private fun installed(): File = File(ExtensionLoader.getPrivateExtensionDir(context), "$PKG.ext")

    @Test
    fun theDirectoryIsUnderFiles() {
        ExtensionLoader.getPrivateExtensionDir(context) shouldBe File(context.filesDir, "exts")
    }

    @Test
    fun aFileThatIsNotAPackage() {
        ExtensionLoader.installPrivateExtensionFile(context, apk("none.apk", info = null)) shouldBe false
    }

    @Test
    fun aPackageWithoutTheFeature() {
        val file = apk("plain.apk", info = extensionPackage(feature = false))
        ExtensionLoader.installPrivateExtensionFile(context, file) shouldBe false
    }

    @Test
    fun aFirstInstallIsCopied() {
        val file = apk("ext.apk", info = extensionPackage())
        ExtensionLoader.installPrivateExtensionFile(context, file) shouldBe true
        installed().readText() shouldBe "apk"
        installed().canWrite() shouldBe false
    }

    @Test
    fun aReplacementIsCopiedOver() {
        val current = installed().apply { writeText("old") }
        shadowOf(context.packageManager).setPackageArchiveInfo(current.absolutePath, extensionPackage())
        val file = apk("ext.apk", info = extensionPackage(versionCode = 5))
        ExtensionLoader.installPrivateExtensionFile(context, file) shouldBe true
        installed().readText() shouldBe "apk"
    }

    @Test
    fun aDowngradeIsRefused() {
        val current = installed().apply { writeText("old") }
        shadowOf(context.packageManager).setPackageArchiveInfo(current.absolutePath, extensionPackage(versionCode = 9))
        val file = apk("ext.apk", info = extensionPackage(versionCode = 1))
        ExtensionLoader.installPrivateExtensionFile(context, file) shouldBe false
        logcat.messages.single() shouldBe "Installed extension version is higher. Downgrading is not allowed."
        installed().readText() shouldBe "old"
    }

    @Test
    fun anUnsignedReplacementIsRefused() {
        val current = installed().apply { writeText("old") }
        shadowOf(context.packageManager).setPackageArchiveInfo(current.absolutePath, extensionPackage())
        val file = apk("ext.apk", info = extensionPackage(signatures = null))
        ExtensionLoader.installPrivateExtensionFile(context, file) shouldBe false
        logcat.messages.single() shouldBe "Extension to be installed is not signed."
    }

    @Test
    fun anotherSignerIsRefused() {
        val current = installed().apply { writeText("old") }
        shadowOf(context.packageManager).setPackageArchiveInfo(current.absolutePath, extensionPackage())
        val file = apk("ext.apk", info = extensionPackage(signatures = listOf("other")))
        ExtensionLoader.installPrivateExtensionFile(context, file) shouldBe false
        logcat.messages.single() shouldBe "Installed extension signature is not matched."
    }

    @Test
    fun aMissingSourceFileIsReported() {
        val file = apk("gone.apk", info = extensionPackage(), bytes = null)
        ExtensionLoader.installPrivateExtensionFile(context, file) shouldBe false
        logcat.messages.single().startsWith("Failed to copy extension file.") shouldBe true
        installed().exists() shouldBe false
    }

    @Test
    fun uninstallingDeletesTheFile() {
        installed().writeText("x")
        ExtensionLoader.uninstallPrivateExtension(context, PKG)
        installed().exists() shouldBe false
    }
}
