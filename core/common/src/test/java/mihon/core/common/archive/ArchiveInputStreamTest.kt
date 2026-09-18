package mihon.core.common.archive

import eu.kanade.tachiyomi.util.storage.CbzCrypto
import eu.kanade.tachiyomi.util.storage.installFakeAndroidKeyStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import me.zhanghai.android.libarchive.ArchiveException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ArchiveInputStreamTest {
    private val entries = listOf(
        FakeEntry("a.txt", data = "hello".toByteArray()),
        FakeEntry(null, rawName = "b.txt".toByteArray(), isFile = false),
        FakeEntry(null, rawName = null, isEncrypted = true),
    )
    private val native = FakeArchiveNative(entries)

    @BeforeEach
    fun setUp() {
        // CbzCrypto opens the AndroidKeyStore when its object initialises, even under mockkObject.
        installFakeAndroidKeyStore()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun opensWithUtf8AndEveryFormat() {
        ArchiveInputStream(0x10L, 5L, false, native).close()
        native.calls shouldContainExactly listOf(
            "readNew=100",
            "setCharset(UTF-8)",
            "readSupportFilterAll",
            "readSupportFormatAll",
            "readOpenMemoryUnsafe(100,16,5)",
            "readFree(100)",
        )
    }

    @Test
    fun encryptedGetsThePassphrase() {
        mockkObject(CbzCrypto)
        every { CbzCrypto.getDecryptedPasswordCbz() } returns "secret".toByteArray()
        ArchiveInputStream(0L, 0L, true, native).close()
        native.passphrases.single().decodeToString() shouldBe "secret"
        native.calls[1] shouldBe "readAddPassphrase(100)"
    }

    @Test
    fun openFailureFreesTheArchive() {
        native.openError = ArchiveException(1, "bad archive")
        val error = shouldThrow<ArchiveException> { ArchiveInputStream(0L, 0L, false, native) }
        error shouldBeSameInstanceAs native.openError
        native.calls.last() shouldBe "readFree(100)"
    }

    @Test
    fun entriesDecodeNameTypeCrypto() {
        val stream = ArchiveInputStream(0L, 0L, false, native)
        stream.getNextEntry() shouldBe ArchiveEntry("a.txt", isFile = true, isEncrypted = false)
        stream.getNextEntry() shouldBe ArchiveEntry("b.txt", isFile = false, isEncrypted = false)
        // No name at all: the entry is skipped as the end of the archive.
        stream.getNextEntry().shouldBeNull()
        stream.getNextEntry().shouldBeNull()
    }

    @Test
    fun readsBytesOfTheCurrentEntry() {
        val stream = ArchiveInputStream(0L, 0L, false, native)
        stream.getNextEntry()
        stream.read() shouldBe 'h'.code
        val bytes = ByteArray(8)
        stream.read(bytes, 1, 4) shouldBe 4
        bytes.copyOfRange(1, 5).decodeToString() shouldBe "ello"
        stream.read(bytes, 0, 8) shouldBe -1
        stream.read() shouldBe -1
    }

    @Test
    fun closeFreesOnce() {
        val stream = ArchiveInputStream(0L, 0L, false, native)
        stream.close()
        stream.close()
        native.calls.count { it.startsWith("readFree") } shouldBe 1
    }

    @Test
    fun publicCtorUsesLibarchive() {
        // The JNI library only exists for Android ABIs, so the real binding cannot initialise here.
        shouldThrow<LinkageError> { ArchiveInputStream(0L, 0L, false) }
    }
}
