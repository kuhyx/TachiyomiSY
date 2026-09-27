package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.domain.installFakeAndroidKeyStore
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * CBZ archiving writes through libarchive's JNI binding, which cannot load in a JVM test: the attempt
 * ends in a [LinkageError] once the output file exists. What runs before it is the password decision.
 */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderArchiveTest : DownloaderTestBase() {

    private var protect = false
    private var passwordSet = false

    @Before
    fun stubCrypto() {
        installFakeAndroidKeyStore()
        mockkObject(CbzCrypto)
        every { CbzCrypto.getPasswordProtectDlPref() } answers { protect }
        every { CbzCrypto.isPasswordSet() } answers { passwordSet }
    }

    private fun archive(name: String) {
        val mangaDir = requireNotNull(UniFile.fromFile(File(root, "Source/Title").apply { mkdirs() }))
        val tmpDir = requireNotNull(mangaDir.createDirectory("${name}_tmp"))
        shouldThrow<LinkageError> { downloader.archiveChapter(mangaDir, name, tmpDir) }
        File(root, "Source/Title/$name.cbz_tmp").exists() shouldBe true
    }

    @Test
    fun plainArchiveNeedsNatives() {
        archive("Ch 1")
    }

    @Test
    fun passwordNeedsBothSettings() {
        protect = true
        archive("Ch 2")
        passwordSet = true
        archive("Ch 3")
    }
}
