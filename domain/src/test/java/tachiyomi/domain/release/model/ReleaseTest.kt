package tachiyomi.domain.release.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class ReleaseTest {

    private val release = Release(
        version = "v1.2.3",
        info = "notes",
        releaseLink = "https://example.org/release",
        assets = listOf("https://example.org/TachiyomiSY-1.2.3.apk"),
    )

    @Test
    fun componentsExposeFields() {
        release.component1() shouldBe "v1.2.3"
        release.component2() shouldBe "notes"
        release.component3() shouldBe "https://example.org/release"
        release.component4() shouldBe listOf("https://example.org/TachiyomiSY-1.2.3.apk")
    }

    @Test
    fun dataClassSurface() {
        release shouldBe release.copy()
        release.copy(version = "v2.0.0") shouldNotBe release
        release.hashCode() shouldBe release.copy().hashCode()
        release.toString() shouldBe "Release(version=v1.2.3, info=notes, releaseLink=https://example.org/release, " +
            "assets=[https://example.org/TachiyomiSY-1.2.3.apk])"
    }
}
