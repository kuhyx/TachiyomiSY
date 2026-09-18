package tachiyomi.core.common.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import com.hippo.unifile.UniFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.util.ReflectionHelpers
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
internal class UniFileTempFileManagerTest {
    private lateinit var root: File
    private lateinit var context: Context
    private lateinit var manager: UniFileTempFileManager
    private var sdk: Int = 0

    @Before
    fun setUp() {
        root = Files.createTempDirectory("tempfiles").toFile()
        context = RuntimeEnvironment.getApplication()
        manager = UniFileTempFileManager(context)
        sdk = Build.VERSION.SDK_INT
    }

    @After
    fun tearDown() {
        setSdk(sdk)
        manager.deleteTempFiles()
        root.deleteRecursively()
        unmockkAll()
    }

    @Test
    fun copiesIntoCacheWithKernelCopy() {
        val source = File(root, "photo.png").apply { writeText("abc") }
        val copy = manager.createTempFile(UniFile.fromFile(source)!!)
        copy.parentFile shouldBe File(context.externalCacheDir, "tmp")
        copy.name shouldStartWith "photo"
        copy.readText() shouldBe "abc"
    }

    @Test
    fun padsMissingNameToMinimumPrefix() {
        val source = File(root, "x").apply { writeText("abc") }
        val file = mockk<UniFile>()
        every { file.name } returns null
        every { file.uri } returns Uri.fromFile(source)
        val copy = manager.createTempFile(file)
        copy.name shouldStartWith "   "
        copy.readText() shouldBe "abc"
    }

    @Test
    fun copiesBufferedBeforeQ() {
        setSdk(Build.VERSION_CODES.P)
        val payload = ByteArray(20_000) { it.toByte() }
        val source = File(root, "big.bin").apply { writeBytes(payload) }
        manager.createTempFile(UniFile.fromFile(source)!!).readBytes() shouldBe payload
    }

    @Test
    fun copiesEmptyFileBeforeQ() {
        setSdk(Build.VERSION_CODES.P)
        val source = File(root, "empty.bin").apply { writeBytes(ByteArray(0)) }
        manager.createTempFile(UniFile.fromFile(source)!!).length() shouldBe 0L
    }

    @Test
    fun failsWithoutInputStream() {
        val resolver = mockk<ContentResolver>()
        every { resolver.openInputStream(any()) } returns null
        val broken = mockk<Context>()
        every { broken.externalCacheDir } returns root
        every { broken.contentResolver } returns resolver
        val source = UniFile.fromFile(File(root, "missing.bin"))!!
        shouldThrow<NullPointerException> { UniFileTempFileManager(broken).createTempFile(source) }
    }

    @Test
    fun deleteRemovesTempDirectory() {
        val source = File(root, "photo.png").apply { writeText("abc") }
        manager.createTempFile(UniFile.fromFile(source)!!)
        File(context.externalCacheDir, "tmp").exists() shouldBe true
        manager.deleteTempFiles()
        File(context.externalCacheDir, "tmp").exists() shouldBe false
    }
}

/** Robolectric pins one SDK per class; the pre-Q copy path needs the field changed in place. */
internal fun setSdk(version: Int) {
    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", version)
}
