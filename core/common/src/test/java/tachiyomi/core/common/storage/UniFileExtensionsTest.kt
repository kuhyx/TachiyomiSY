package tachiyomi.core.common.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.hippo.unifile.UniFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
internal class UniFileExtensionsTest {
    private lateinit var root: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("unifile").toFile()
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
        unmockkAll()
    }

    @Test
    fun extensionSplitsOnLastDot() {
        val file = UniFile.fromFile(File(root, "archive.tar.gz"))!!
        file.extension shouldBe "gz"
        file.nameWithoutExtension shouldBe "archive.tar"
    }

    @Test
    fun extensionIsNullWithoutName() {
        val file = mockk<UniFile>()
        every { file.name } returns null
        file.extension.shouldBeNull()
        file.nameWithoutExtension.shouldBeNull()
    }

    @Test
    fun displayablePathPrefersFilePath() {
        val file = File(root, "page.jpg")
        UniFile.fromFile(file)!!.displayablePath shouldBe file.path
    }

    @Test
    fun displayablePathFallsBackToUri() {
        val file = mockk<UniFile>()
        every { file.filePath } returns null
        every { file.uri } returns Uri.parse("content://provider/tree/doc")
        file.displayablePath shouldBe "content://provider/tree/doc"
    }

    @Test
    fun opensDescriptorForFile() {
        val file = File(root, "data.bin").apply { writeBytes(ByteArray(3)) }
        UniFile.fromFile(file)!!.openFileDescriptor(RuntimeEnvironment.getApplication(), "r").use {
            it.statSize shouldBe 3L
        }
    }

    @Test
    fun failsWhenResolverReturnsNull() {
        val resolver = mockk<ContentResolver>()
        every { resolver.openFileDescriptor(any(), any()) } returns null
        val context = mockk<Context>()
        every { context.contentResolver } returns resolver
        val file = UniFile.fromFile(File(root, "gone.bin"))!!
        val error = shouldThrow<IllegalStateException> { file.openFileDescriptor(context, "r") }
        error.message shouldContain "gone.bin"
    }
}
