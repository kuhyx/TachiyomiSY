package mihon.gradle.transforms

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gradle.api.artifacts.transform.TransformOutputs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal fun File.writeJar(vararg entries: String): File {
    ZipOutputStream(outputStream()).use { out ->
        entries.forEach { name ->
            out.putNextEntry(ZipEntry(name))
            if (!name.endsWith("/")) out.write(name.toByteArray())
            out.closeEntry()
        }
    }
    return this
}

internal fun File.entryNames(): List<String> = ZipFile(this).use { zip ->
    zip.entries().asSequence().map { it.name }.toList()
}

internal class MultiReleaseJarsTest {
    @TempDir
    lateinit var dir: File

    private val versioned = arrayOf("a/", "a/B.class", "META-INF/versions/", "META-INF/versions/11/a/B.class")

    @Test
    fun detectsVersionsEntries() {
        hasMultiReleaseEntries(File(dir, "mr.jar").writeJar(*versioned)) shouldBe true
        hasMultiReleaseEntries(File(dir, "plain.jar").writeJar("a/", "a/B.class")) shouldBe false
    }

    @Test
    fun copyDropsVersionsEntriesOnly() {
        val source = File(dir, "mr.jar").writeJar(*versioned)
        val target = File(dir, "out.jar")
        copyWithoutMultiRelease(source, target)
        target.entryNames() shouldContainExactly listOf("a/", "a/B.class")
        ZipFile(target).use { zip ->
            zip.getInputStream(zip.getEntry("a/B.class")).readBytes().decodeToString() shouldBe "a/B.class"
        }
    }

    @Test
    fun cleanJarPassesThroughUntouched() {
        val plain = File(dir, "plain.jar").writeJar("a/B.class")
        val outputs = mockk<TransformOutputs>(relaxed = true)
        stripMultiRelease(plain, outputs)
        verify(exactly = 1) { outputs.file(plain) }
    }

    @Test
    fun versionedJarIsRewritten() {
        val source = File(dir, "mr.jar").writeJar(*versioned)
        val target = File(dir, "workspace/mr.jar").also { it.parentFile.mkdirs() }
        val outputs = mockk<TransformOutputs> { every { file("mr.jar") } returns target }
        stripMultiRelease(source, outputs)
        target.entryNames() shouldContainExactly listOf("a/", "a/B.class")
    }
}
