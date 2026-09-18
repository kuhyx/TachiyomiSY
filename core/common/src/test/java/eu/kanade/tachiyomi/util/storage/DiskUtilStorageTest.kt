package eu.kanade.tachiyomi.util.storage

import android.content.Context
import android.os.Environment
import com.hippo.unifile.UniFile
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowEnvironment
import org.robolectric.shadows.ShadowStatFs
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
internal class DiskUtilStorageTest {
    private lateinit var root: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("diskutil").toFile()
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
        unmockkAll()
    }

    @Test
    fun keepsMountedExternalRoots() {
        val mounted = File(root, "mounted/Android/data/pkg/files")
        // Without an "/Android/" segment the whole path is the storage root.
        val readOnly = File(root, "readonly/files")
        val removed = File(root, "removed/Android/files")
        ShadowEnvironment.setExternalStorageState(File(root, "mounted"), Environment.MEDIA_MOUNTED)
        ShadowEnvironment.setExternalStorageState(readOnly, Environment.MEDIA_MOUNTED_READ_ONLY)
        ShadowEnvironment.setExternalStorageState(File(root, "removed"), Environment.MEDIA_REMOVED)
        val context = mockk<Context>()
        every { context.getExternalFilesDirs(null) } returns arrayOf(mounted, null, readOnly, removed)

        DiskUtil.getExternalStorages(context) shouldContainExactly listOf(File(root, "mounted"), readOnly)
    }

    @Test
    fun noStoragesWithoutDirs() {
        val context = mockk<Context>()
        every { context.getExternalFilesDirs(null) } returns arrayOfNulls<File>(1)
        DiskUtil.getExternalStorages(context) shouldBe emptyList()
    }

    @Test
    fun directorySizeSumsRecursively() {
        File(root, "a.bin").writeBytes(ByteArray(3))
        val sub = File(root, "sub").apply { mkdirs() }
        File(sub, "b.bin").writeBytes(ByteArray(4))
        DiskUtil.getDirectorySize(root) shouldBe 7L
        DiskUtil.getDirectorySize(File(root, "a.bin")) shouldBe 3L
    }

    @Test
    fun sizeIgnoresUnlistableDir() {
        DiskUtil.getDirectorySize(UnlistableDir()) shouldBe 0L
    }

    @Test
    fun totalSpaceComesFromStatFs() {
        ShadowStatFs.registerStats(root, 100, 40, 30)
        DiskUtil.getTotalStorageSpace(root) shouldBe 100L * ShadowStatFs.BLOCK_SIZE
    }

    @Test
    fun availableSpaceComesFromStatFs() {
        ShadowStatFs.registerStats(root, 100, 40, 30)
        DiskUtil.getAvailableStorageSpace(root) shouldBe 30L * ShadowStatFs.BLOCK_SIZE
        DiskUtil.getAvailableStorageSpace(UniFile.fromFile(root)!!) shouldBe 30L * ShadowStatFs.BLOCK_SIZE
    }

    @Test
    fun spaceIsMinusOneWhenStatFails() {
        DiskUtil.getTotalStorageSpace(ThrowingFile()) shouldBe -1L
        DiskUtil.getAvailableStorageSpace(ThrowingFile()) shouldBe -1L
        val broken = mockk<UniFile>()
        every { broken.uri } throws IllegalStateException("no uri")
        DiskUtil.getAvailableStorageSpace(broken) shouldBe -1L
    }
}

/** A directory whose listing fails, as `File.listFiles` reports with null. */
private class UnlistableDir : File("unlistable") {
    override fun isDirectory(): Boolean = true

    override fun listFiles(): Array<File>? = null

    private companion object {
        private const val serialVersionUID = 1L
    }
}

/** A file whose path cannot be resolved, so `StatFs` never gets constructed. */
private class ThrowingFile : File("throwing") {
    override fun getAbsolutePath(): String = error("no path")

    private companion object {
        private const val serialVersionUID = 1L
    }
}
