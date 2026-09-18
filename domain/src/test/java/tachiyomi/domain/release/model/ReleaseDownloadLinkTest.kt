package tachiyomi.domain.release.model

import android.os.Build
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
internal class ReleaseDownloadLinkTest {

    private val release = Release(
        version = "v1.2.3",
        info = "notes",
        releaseLink = "https://example.org/release",
        assets = listOf(UNIVERSAL, ARM64, ARM32, X86, X86_64),
    )

    @Test
    fun arm64PicksArm64Asset() {
        withAbi("arm64-v8a")

        release.getDownloadLink() shouldBe ARM64
    }

    @Test
    fun arm32PicksArm32Asset() {
        withAbi("armeabi-v7a")

        release.getDownloadLink() shouldBe ARM32
    }

    @Test
    fun x86PicksX86Asset() {
        withAbi("x86")

        release.getDownloadLink() shouldBe X86
    }

    @Test
    fun x8664PicksX8664Asset() {
        withAbi("x86_64")

        release.getDownloadLink() shouldBe X86_64
    }

    @Test
    fun unknownAbiPicksUniversal() {
        withAbi("mips")

        release.getDownloadLink() shouldBe UNIVERSAL
    }

    @Test
    fun noMatchFallsBackToFirst() {
        withAbi("arm64-v8a")

        release.copy(assets = listOf(X86, X86_64)).getDownloadLink() shouldBe X86
    }

    private fun withAbi(abi: String) {
        ReflectionHelpers.setStaticField(Build::class.java, "SUPPORTED_ABIS", arrayOf(abi))
    }

    private companion object {
        const val UNIVERSAL = "https://example.org/TachiyomiSY-1.2.3.apk"
        const val ARM64 = "https://example.org/TachiyomiSY-arm64-v8a-1.2.3.apk"
        const val ARM32 = "https://example.org/TachiyomiSY-armeabi-v7a-1.2.3.apk"
        const val X86 = "https://example.org/TachiyomiSY-x86-1.2.3.apk"
        const val X86_64 = "https://example.org/TachiyomiSY-x86_64-1.2.3.apk"
    }
}
