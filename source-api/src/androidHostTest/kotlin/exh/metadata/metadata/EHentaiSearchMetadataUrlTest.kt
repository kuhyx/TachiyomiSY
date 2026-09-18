package exh.metadata.metadata

import android.net.Uri
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EHentaiSearchMetadataUrlTest {
    private val fullUrl = "https://e-hentai.org/g/123456/abcdef0123/"

    @BeforeEach
    fun setUp() {
        mockkStatic(Uri::class)
        val parsed = mockk<Uri> {
            every { pathSegments } returns listOf("g", "123456", "abcdef0123")
        }
        every { Uri.parse(fullUrl) } returns parsed
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun galleryIdFromFullUrl() {
        EHentaiSearchMetadata.galleryId(fullUrl) shouldBe "123456"
    }

    @Test
    fun galleryTokenFromFullUrl() {
        EHentaiSearchMetadata.galleryToken(fullUrl) shouldBe "abcdef0123"
    }

    @Test
    fun galleryIdFromRelativeUrl() {
        EHentaiSearchMetadata.galleryId("/g/42/deadbeef/?nw=always") shouldBe "42"
        EHentaiSearchMetadata.galleryToken("/g/42/deadbeef/?nw=always") shouldBe "deadbeef"
    }

    @Test
    fun normalizeUrlRebuildsRelative() {
        EHentaiSearchMetadata.normalizeUrl(fullUrl) shouldBe "/g/123456/abcdef0123/?nw=always"
        EHentaiSearchMetadata.normalizeUrl("g/7/tok") shouldBe "/g/7/tok/?nw=always"
    }

    @Test
    fun idAndTokenToUrlSkipsWarning() {
        EHentaiSearchMetadata.idAndTokenToUrl("1", "t") shouldBe "/g/1/t/?nw=always"
    }

    @Test
    fun tagTypesAreDistinct() {
        listOf(
            EHentaiSearchMetadata.TAG_TYPE_NORMAL,
            EHentaiSearchMetadata.TAG_TYPE_LIGHT,
            EHentaiSearchMetadata.TAG_TYPE_WEAK,
        ) shouldBe listOf(0, 1, 2)
    }

    @Test
    fun namespacesMatchSite() {
        listOf(
            EHentaiSearchMetadata.EH_GENRE_NAMESPACE,
            EHentaiSearchMetadata.EH_LANGUAGE_NAMESPACE,
            EHentaiSearchMetadata.EH_META_NAMESPACE,
            EHentaiSearchMetadata.EH_UPLOADER_NAMESPACE,
            EHentaiSearchMetadata.EH_VISIBILITY_NAMESPACE,
        ) shouldBe listOf("genre", "language", "meta", "uploader", "visibility")
    }
}
