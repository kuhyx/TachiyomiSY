package eu.kanade.tachiyomi.data.saver

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.system.ImageUtil
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Android 10+ saves to Pictures through MediaStore; the resolver is a stub. */
@RunWith(RobolectricTestRunner::class)
internal class ImageSaverMediaStoreTest {

    private val resolver: ContentResolver = mockk(relaxed = true)
    private val context: Context = mockk<Context>(relaxed = true).also {
        every { it.contentResolver } returns resolver
    }
    private val inserted: Uri = Uri.parse("content://media/external/images/media/9")
    private val output = ByteArrayOutputStream()
    private val saver = ImageSaver(context)

    @Before
    fun setUp() {
        sniffAs(ImageUtil.ImageType.JPEG)
        every { resolver.query(any(), any(), any(), any(), any()) } returns null
        every { resolver.insert(any(), any()) } returns inserted
        every { resolver.openOutputStream(inserted, "w") } returns output
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun savePicture(): Uri = saver.save(jpegPage(name = "pic", location = Location.Pictures.create("album")))

    @Test
    fun unknownMimeGoesToFiles() {
        mimeKnown(false)
        savePicture() shouldBe inserted
        verify { resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), any()) }
        output.toByteArray().toList() shouldBe jpegBytes.toList()
    }

    @Test
    fun knownMimeGoesToImages() {
        mimeKnown(true)
        savePicture()
        verify {
            resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), any())
        }
    }

    @Test
    fun existingEntryIsReused() {
        val cursor = MatrixCursor(arrayOf(MediaStore.MediaColumns._ID)).apply { addRow(arrayOf(4L)) }
        every { resolver.query(any(), any(), any(), any(), any()) } returns cursor
        val existing = Uri.parse("${MediaStore.Images.Media.EXTERNAL_CONTENT_URI}/4")
        every { resolver.openOutputStream(existing, "w") } returns output
        savePicture() shouldBe existing
        verify(exactly = 0) { resolver.insert(any(), any()) }
    }

    @Test
    fun emptyOrUnreadableQueryInserts() {
        every { resolver.query(any(), any(), any(), any(), any()) } returns MatrixCursor(arrayOf("_id"))
        savePicture() shouldBe inserted
        val stuck = mockk<Cursor>(relaxed = true)
        every { stuck.count } returns 1
        every { stuck.moveToFirst() } returns false
        every { resolver.query(any(), any(), any(), any(), any()) } returns stuck
        savePicture() shouldBe inserted
    }

    @Test
    fun failedInsertThrows() {
        every { resolver.insert(any(), any()) } returns null
        shouldThrow<IOException> { savePicture() }
    }

    @Test
    fun failedWriteThrows() {
        every { resolver.openOutputStream(inserted, "w") } returns null
        shouldThrow<IOException> { savePicture() }
    }
}
