package eu.kanade.tachiyomi.util.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class FileExtensionsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun cacheImageDirLivesInTheCache() {
        context.cacheImageDir shouldBe File(context.cacheDir, "shared_image")
    }

    @Test
    fun uriComesFromTheFileProvider() {
        val file = File(context.cacheImageDir, "img.png")
        file.getUriCompat(context).toString() shouldContain ".provider"
    }

    @Test
    fun copiesAFileReadOnly() {
        val source = folder.newFile("source.txt").apply { writeText("data") }
        val target = File(folder.root, "sub/target.txt")
        source.copyAndSetReadOnlyTo(target) shouldBe target
        target.readText() shouldBe "data"
        target.canWrite() shouldBe false
        shouldThrow<FileAlreadyExistsException> { source.copyAndSetReadOnlyTo(target) }
        source.copyAndSetReadOnlyTo(target, overwrite = true, bufferSize = 2).readText() shouldBe "data"
    }

    @Test
    fun aBareTargetNameHasNoParent() {
        val source = folder.newFile("bare.txt").apply { writeText("b") }
        val target = File("file-extensions-test.tmp")
        try {
            source.copyAndSetReadOnlyTo(target).readText() shouldBe "b"
        } finally {
            target.delete()
        }
    }

    @Test
    fun copiesADirectory() {
        val source = folder.newFolder("dir")
        val target = File(folder.root, "copy")
        source.copyAndSetReadOnlyTo(target).isDirectory shouldBe true
        val blocked = File(folder.root, "blocked").apply { writeText("file") }
        shouldThrow<FileSystemException> { source.copyAndSetReadOnlyTo(File(blocked, "inner")) }
    }

    @Test
    fun missingSourceAndStubbornTarget() {
        val missing = File(folder.root, "missing")
        shouldThrow<NoSuchFileException> { missing.copyAndSetReadOnlyTo(File(folder.root, "x")) }
        val source = folder.newFile("src").apply { writeText("s") }
        val stubborn = folder.newFolder("stubborn").also { File(it, "child").writeText("c") }
        shouldThrow<FileAlreadyExistsException> { source.copyAndSetReadOnlyTo(stubborn, overwrite = true) }
    }
}
