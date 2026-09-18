package mihon.core.common.archive

import android.system.StructStat
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import eu.kanade.tachiyomi.util.storage.installFakeAndroidKeyStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import me.zhanghai.android.libarchive.ArchiveEntry.AE_IFREG
import me.zhanghai.android.libarchive.ArchiveException
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.nio.file.Files

/** Robolectric for the real [android.system.StructStat] and file descriptors behind [UniFile]. */
@RunWith(RobolectricTestRunner::class)
internal class ZipWriterTest {
    private val context = RuntimeEnvironment.getApplication()
    private val root = Files.createTempDirectory("zip-writer").toFile()
    private val target = UniFile.fromFile(File(root, "out.cbz"))!!
    private val native = FakeArchiveNative()

    @Before
    fun setUp() {
        // CbzCrypto opens the AndroidKeyStore when its object initialises, even under mockkObject.
        installFakeAndroidKeyStore()
    }

    @After
    fun tearDown() {
        unmockkAll()
        root.deleteRecursively()
    }

    @Test
    fun opensAStoredZipOnTheTarget() {
        ZipWriter(context, target, false, native).use { it.context shouldBeSameInstanceAs context }
        native.calls shouldContainExactly listOf(
            "writeNew=100",
            "entryNew2=101",
            "setCharset(UTF-8)",
            "writeSetFormatZip",
            "writeZipSetCompressionStore",
            "writeOpenFd",
            "entryFree(101)",
            "writeFree(100)",
        )
    }

    @Test
    fun encryptionSetsAlgoAndPassword() {
        mockkObject(CbzCrypto)
        every { CbzCrypto.getPreferredEncryptionAlgo() } returns "zip:encryption=aes256".toByteArray()
        every { CbzCrypto.getDecryptedPasswordCbz() } returns "pw".toByteArray()
        ZipWriter(context, target, true, native).close()
        native.calls.slice(5..6) shouldContainExactly
            listOf("writeSetOptions(zip:encryption=aes256)", "writeSetPassphrase")
        native.passphrases.single().decodeToString() shouldBe "pw"
    }

    @Test
    fun openFailureReleasesEverything() {
        native.writeOpenError = ArchiveException(1, "cannot open")
        val error = shouldThrow<ArchiveException> { ZipWriter(context, target, false, native) }
        error shouldBeSameInstanceAs native.writeOpenError
        native.calls.takeLast(2) shouldContainExactly listOf("entryFree(101)", "writeFree(100)")
    }

    @Test
    fun writesAFileInBufferSizedChunks() {
        val source = File(root, "page.bin").apply { writeBytes(ByteArray(10_000) { it.toByte() }) }
        native.fileData = source.readBytes()
        native.stat = StructStat(1L, 2L, 3, 4L, 5, 6, 7L, 10_000L, 9L, 10L, 11L, 12L, 13L)
        ZipWriter(context, target, false, native).use { it.write(UniFile.fromFile(source)!!) }
        native.written.single().let { (name, data) ->
            name shouldBe "page.bin"
            data shouldBe source.readBytes()
        }
        native.calls.filter { it.startsWith("writeData") } shouldContainExactly
            listOf("writeData(8192)", "writeData(1808)")
        native.calls.filter { it.startsWith("entrySetStat") } shouldContainExactly listOf("entrySetStat(10000)")
    }

    @Test
    fun writesBytesAsARegularFileEntry() {
        val data = ByteArray(9_000) { (it % 7).toByte() }
        ZipWriter(context, target, false, native).use { it.write(data, "cover.jpg") }
        native.written.single() shouldBe ("cover.jpg" to data)
        native.calls.filter { it.startsWith("entrySet") || it.startsWith("writeData") } shouldContainExactly listOf(
            "entrySetSize(9000)",
            "entrySetFiletype($AE_IFREG)",
            "writeData(8192)",
            "writeData(808)",
        )
    }

    @Test
    fun emptyBytesWriteOnlyAHeader() {
        ZipWriter(context, target, false, native).use { it.write(ByteArray(0), "empty") }
        native.written.single() shouldBe ("empty" to ByteArray(0))
        native.calls.none { it.startsWith("writeData") } shouldBe true
    }

    @Test
    fun statIsCopiedFieldByField() {
        val stat = StructStat(1L, 2L, 3, 4L, 5, 6, 7L, 8L, 9L, 10L, 11L, 12L, 13L).toArchiveStat()
        stat.stDev shouldBe 1L
        stat.stIno shouldBe 2L
        stat.stMode shouldBe 3
        stat.stNlink shouldBe 4
        stat.stUid shouldBe 5
        stat.stGid shouldBe 6
        stat.stRdev shouldBe 7L
        stat.stSize shouldBe 8L
        stat.stAtim.tvSec shouldBe 9L
        stat.stMtim.tvSec shouldBe 10L
        stat.stCtim.tvSec shouldBe 11L
        stat.stBlksize shouldBe 12L
        stat.stBlocks shouldBe 13L
    }

    @Test
    fun publicCtorUsesLibarchive() {
        // The JNI library only exists for Android ABIs, so the real binding cannot initialise here.
        shouldThrow<LinkageError> { ZipWriter(context, target, false) }
    }
}
