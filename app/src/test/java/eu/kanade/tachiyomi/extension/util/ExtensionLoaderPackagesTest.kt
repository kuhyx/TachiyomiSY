package eu.kanade.tachiyomi.extension.util

import android.content.pm.PackageInfo
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExtensionLoaderPackagesTest {
    private fun info(versionCode: Int, isShared: Boolean): ExtensionLoader.ExtensionInfo =
        ExtensionLoader.ExtensionInfo(
            packageInfo = extensionPackage(versionCode = versionCode),
            isShared = isShared,
        )

    @Test
    fun onlyOneSideInstalled() {
        val shared = info(versionCode = 2, isShared = true)
        val private = info(versionCode = 2, isShared = false)
        ExtensionLoader.selectExtensionPackage(shared = null, private = private) shouldBe private
        ExtensionLoader.selectExtensionPackage(shared = shared, private = null) shouldBe shared
        ExtensionLoader.selectExtensionPackage(shared = null, private = null).shouldBeNull()
    }

    @Test
    fun theNewerPackageWins() {
        val shared = info(versionCode = 2, isShared = true)
        ExtensionLoader.selectExtensionPackage(
            shared = shared,
            private = info(versionCode = 1, isShared = false),
        ) shouldBe shared
        ExtensionLoader.selectExtensionPackage(
            shared = shared,
            private = info(versionCode = 2, isShared = false),
        ) shouldBe shared
        val newerPrivate = info(versionCode = 3, isShared = false)
        ExtensionLoader.selectExtensionPackage(shared = shared, private = newerPrivate) shouldBe newerPrivate
    }

    @Test
    fun theExtensionFeatureFlag() {
        ExtensionLoader.isPackageAnExtension(extensionPackage()) shouldBe true
        ExtensionLoader.isPackageAnExtension(extensionPackage(feature = false)) shouldBe false
        ExtensionLoader.isPackageAnExtension(PackageInfo()) shouldBe false
    }

    @Test
    fun signaturesAreHashed() {
        val single = ExtensionLoader.getSignatures(extensionPackage(signatures = listOf("one")))
        single?.size shouldBe 1
        single?.single()?.length shouldBe 64
        val multiple = ExtensionLoader.getSignatures(
            extensionPackage(signatures = listOf("one", "two"), multipleSigners = true),
        )
        multiple?.size shouldBe 2
        multiple?.first() shouldBe single?.first()
    }

    @Test
    fun unsignedPackagesHaveNone() {
        ExtensionLoader.getSignatures(extensionPackage(signatures = null)).shouldBeNull()
        ExtensionLoader.getSignatures(
            extensionPackage(signatures = null, multipleSigners = true),
        ).shouldBeNull()
    }

    @Test
    fun aPackageWithoutSigningInfo() {
        shouldThrow<NullPointerException> {
            ExtensionLoader.getSignatures(extensionPackage(signed = false))
        }
    }

    @Test
    fun theExtensionInfoDataClass() {
        val info = info(versionCode = 1, isShared = true)
        info.isShared shouldBe true
        info.copy(isShared = false).isShared shouldBe false
        info.packageInfo.packageName shouldBe "eu.kanade.tachiyomi.extension.test"
        (info == info.copy()) shouldBe true
        info.hashCode() shouldBe info.copy().hashCode()
        info.toString().contains("ExtensionInfo") shouldBe true
    }
}
