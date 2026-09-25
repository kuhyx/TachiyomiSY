package eu.kanade.tachiyomi.data.saver

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.util.storage.cacheImageDir
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.system.ImageUtil
import java.io.ByteArrayInputStream
import java.io.File

/** Saving to a plain directory: the cache, or Pictures before Android 10. */
@RunWith(RobolectricTestRunner::class)
internal class ImageSaverFileTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val saver = ImageSaver(context)

    @Before
    fun setUp() {
        sniffAs(ImageUtil.ImageType.JPEG)
        plainFileUris()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun cacheSaveWritesTheFile() {
        val uri = saver.save(jpegPage(name = "page", location = Location.Cache))
        uri shouldBe Uri.fromFile(File(context.cacheImageDir, "page.jpg"))
        File(context.cacheImageDir, "page.jpg").readBytes().toList() shouldBe jpegBytes.toList()
    }

    @Test
    fun nonImageIsRejected() {
        val text = Image.Page(
            inputStream = { ByteArrayInputStream("text".toByteArray()) },
            name = "t",
            location = Location.Cache,
        )
        sniffAs(null)
        shouldThrow<IllegalArgumentException> { saver.save(text) }
    }

    @Test
    fun oldAndroidSavesToPictures() {
        withSdk(Build.VERSION_CODES.P) { saver.save(jpegPage(name = "old", location = Location.Pictures.create("s"))) }
        File(Location.Pictures.create("s").directory(context), "old.jpg").exists() shouldBe true
    }

    @Test
    fun picturesDirectoryNesting() {
        val root = Location.Pictures.create().directory(context)
        root.parentFile shouldBe Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        Location.Pictures.create("sub").directory(context) shouldBe File(root, "sub")
        Location.Cache.directory(context) shouldBe context.cacheImageDir
    }

    @Test
    fun coverDataIsAJpegStream() {
        val cover = Image.Cover(
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
            name = "c",
            location = Location.Cache,
        )
        cover.data().readBytes().isNotEmpty() shouldBe true
        jpegPage(name = "p", location = Location.Cache).data().readBytes().size shouldBe jpegBytes.size
    }
}
