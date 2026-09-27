package eu.kanade.tachiyomi.extension.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Before Android P there is no SigningInfo: flags and signatures take the legacy route. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
internal class ExtensionLoaderLegacyTest {

    @Test
    fun flagsSkipSigningCertificates() {
        (ExtensionLoader.PACKAGE_FLAGS and PackageManager.GET_SIGNING_CERTIFICATES) shouldBe 0
        (ExtensionLoader.PACKAGE_FLAGS and PackageManager.GET_META_DATA) shouldBe PackageManager.GET_META_DATA
    }

    @Test
    fun signaturesComeFromThePackage() {
        val info = PackageInfo()
        // The field is deprecated from P on; set it the way the platform does, by reflection.
        PackageInfo::class.java.getField("signatures").set(info, arrayOf(Signature("cafe".toByteArray())))
        ExtensionLoader.getSignatures(info)?.size shouldBe 1
    }
}
