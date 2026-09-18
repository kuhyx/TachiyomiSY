package eu.kanade.tachiyomi.util.storage

import eu.kanade.tachiyomi.core.security.SecurityPreferences
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import mihon.core.common.archive.ArchiveReader
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.system.ImageUtil
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.SequenceInputStream

@RunWith(RobolectricTestRunner::class)
internal class CbzCryptoArchivesTest {
    private lateinit var crypto: TestCbzCrypto

    @Before
    fun setUp() {
        CryptoTestEnv.install()
        crypto = TestCbzCrypto()
    }

    @After
    fun tearDown() {
        CryptoTestEnv.restore()
        unmockkAll()
    }

    @Test
    fun readsPasswordProtectPreference() {
        crypto.getPasswordProtectDlPref() shouldBe false
        CryptoTestEnv.prefs.passwordProtectDownloads.set(true)
        crypto.getPasswordProtectDlPref() shouldBe true
    }

    @Test
    fun paddingIsNullWhenUnprotected() {
        crypto.createComicInfoPadding().shouldBeNull()
    }

    @Test
    fun paddingIsRandomAlphanumeric() {
        CryptoTestEnv.prefs.passwordProtectDownloads.set(true)
        val padding = crypto.createComicInfoPadding()!!
        padding.length shouldBeInRange PADDING_MIN..<PADDING_MIN + PADDING_RANGE
        padding.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' } shouldBe true
        crypto.createComicInfoPadding() shouldNotBe padding
    }

    @Test
    fun mapsEncryptionTypeToOption() {
        crypto.getPreferredEncryptionAlgo() shouldBe "zip:encryption=aes256".toByteArray()
        CryptoTestEnv.prefs.encryptionType.set(SecurityPreferences.EncryptionType.AES_128)
        crypto.getPreferredEncryptionAlgo() shouldBe "zip:encryption=aes128".toByteArray()
        CryptoTestEnv.prefs.encryptionType.set(SecurityPreferences.EncryptionType.ZIP_STANDARD)
        crypto.getPreferredEncryptionAlgo() shouldBe "zip:encryption=zipcrypt".toByteArray()
    }

    @Test
    fun detectsCoverAndRewinds() {
        val stream = ByteArrayInputStream("PK..COVER.JPG..".toByteArray())
        crypto.detectCoverImageArchive(stream) shouldBe true
        stream.read() shouldBe 'P'.code
        crypto.detectCoverImageArchive(ByteArrayInputStream("PK..page1.jpg".toByteArray())) shouldBe false
    }

    @Test
    fun detectsCoverInUnmarkableStream() {
        crypto.detectCoverImageArchive(unmarkable("PK..cover.jpg..")) shouldBe true
        crypto.detectCoverImageArchive(unmarkable("PK..page1.jpg..")) shouldBe false
    }

    @Test
    fun noCoverWhenEntryMissing() {
        val reader = mockk<ArchiveReader>()
        every { reader.getInputStream("cover.jpg") } returns null
        with(crypto) { reader.getCoverStream() }.shouldBeNull()
    }

    @Test
    fun noCoverWhenNotAnImage() {
        val reader = mockk<ArchiveReader>()
        every { reader.getInputStream("cover.jpg") } returns ByteArrayInputStream(ByteArray(1))
        mockkObject(ImageUtil)
        every { ImageUtil.isImage("cover.jpg", any()) } returns false
        with(crypto) { reader.getCoverStream() }.shouldBeNull()
    }

    @Test
    fun noCoverWhenReopenFails() {
        val reader = mockk<ArchiveReader>()
        every { reader.getInputStream("cover.jpg") } returnsMany listOf(ByteArrayInputStream(ByteArray(1)), null)
        mockkObject(ImageUtil)
        every { ImageUtil.isImage("cover.jpg", any()) } returns true
        with(crypto) { reader.getCoverStream() }.shouldBeNull()
    }

    @Test
    fun coverKeepsABufferedStream() {
        val reader = mockk<ArchiveReader>()
        val cover: InputStream = ByteArrayInputStream("cover".toByteArray()).buffered()
        every { reader.getInputStream("cover.jpg") } returnsMany listOf(ByteArrayInputStream(ByteArray(1)), cover)
        mockkObject(ImageUtil)
        every { ImageUtil.isImage("cover.jpg", any()) } returns true
        with(crypto) { reader.getCoverStream() } shouldBeSameInstanceAs cover
    }

    @Test
    fun coverStreamReopensEntry() {
        val reader = mockk<ArchiveReader>()
        val probe: InputStream = ByteArrayInputStream("probe".toByteArray())
        val cover: InputStream = ByteArrayInputStream("cover".toByteArray())
        every { reader.getInputStream("cover.jpg") } returnsMany listOf(probe, cover)
        with(crypto) { reader.getCoverStream() }!!.readBytes() shouldBe "cover".toByteArray()
    }
}

/** An [InputStream] without mark support; [SequenceInputStream] never supports it. */
internal fun unmarkable(content: String): InputStream =
    SequenceInputStream(ByteArrayInputStream(content.toByteArray()), ByteArrayInputStream(ByteArray(0)))
