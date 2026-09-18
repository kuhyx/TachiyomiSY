package mihon.core.common.archive

import android.os.ParcelFileDescriptor
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import eu.kanade.tachiyomi.util.storage.installFakeAndroidKeyStore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import me.zhanghai.android.libarchive.ArchiveException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.FileDescriptor

internal class ArchiveReaderTest {
    private val pfd: ParcelFileDescriptor = mockk {
        every { statSize } returns 42L
        every { fileDescriptor } returns FileDescriptor()
    }

    @BeforeEach
    fun setUp() {
        // CbzCrypto opens the AndroidKeyStore when its object initialises, even under mockkObject.
        installFakeAndroidKeyStore()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun reader(
        vararg entries: FakeEntry,
        configure: FakeArchiveNative.() -> Unit = {},
    ): Pair<ArchiveReader, FakeArchiveNative> {
        val native = FakeArchiveNative(entries.toList()).apply(configure)
        return ArchiveReader(pfd, native) to native
    }

    @Test
    fun mapsAndScansForEncryption() {
        val (reader, native) = reader(FakeEntry("a"), FakeEntry("b"))
        reader.size shouldBe 42L
        reader.address shouldBe FakeArchiveNative.MAPPING
        reader.archiveHashCode shouldBe pfd.hashCode()
        reader.encrypted shouldBe false
        reader.wrongPassword.shouldBeNull()
        native.calls.first() shouldBe "mmap(42)"
        native.calls.last() shouldBe "readFree(100)"
        reader.close()
        native.calls.last() shouldBe "munmap(${FakeArchiveNative.MAPPING},42)"
    }

    @Test
    fun useEntriesListsAndCloses() {
        val (reader, native) = reader(FakeEntry("a"), FakeEntry("b", isFile = false))
        reader.useEntries { entries -> entries.map { it.name }.toList() } shouldContainExactly listOf("a", "b")
        native.calls.last() shouldBe "readFree(101)"
    }

    @Test
    fun useEntriesCopyBehavesTheSame() {
        // Kotlin inlines useEntries at every call site; the copy in the class only runs reflectively.
        val (reader, _) = reader(FakeEntry("x"))
        val method = ArchiveReader::class.java.getMethod("useEntries", Function1::class.java)
        val block: (Sequence<ArchiveEntry>) -> List<String> = { entries -> entries.map { it.name }.toList() }
        method.invoke(reader, block) shouldBe listOf("x")
    }

    @Test
    fun streamPositionsOnNamedEntry() {
        val (reader, native) = reader(
            FakeEntry("a", data = "A".toByteArray()),
            FakeEntry("b", data = "B".toByteArray()),
        )
        reader.getInputStream("b")!!.use { it.read() shouldBe 'B'.code }
        reader.getInputStream("missing").shouldBeNull()
        native.calls.count { it.startsWith("readFree") } shouldBe 3
    }

    @Test
    fun scanFailureClosesAndRethrows() {
        val (reader, native) = reader(FakeEntry("a"))
        native.headerError = ArchiveException(1, "corrupt")
        shouldThrow<ArchiveException> { reader.getInputStream("a") } shouldBeSameInstanceAs native.headerError
        native.calls.last() shouldBe "readFree(101)"
    }

    @Test
    fun cryptoScanFailureRethrows() {
        val error = ArchiveException(1, "corrupt")
        shouldThrow<ArchiveException> { reader(FakeEntry("a")) { headerError = error } } shouldBeSameInstanceAs error
    }

    @Test
    fun correctPasswordReadsEntry() {
        mockkObject(CbzCrypto)
        every { CbzCrypto.getDecryptedPasswordCbz() } returns "pw".toByteArray()
        val (reader, native) = reader(
            FakeEntry("plain"),
            FakeEntry("secret", isEncrypted = true, data = "S".toByteArray()),
        )
        reader.encrypted shouldBe true
        reader.wrongPassword shouldBe false
        native.passphrases.single().decodeToString() shouldBe "pw"
    }

    @Test
    fun wrongPasswordIsRecorded() {
        mockkObject(CbzCrypto)
        every { CbzCrypto.getDecryptedPasswordCbz() } returns "pw".toByteArray()
        val (reader, _) = reader(FakeEntry("secret", isEncrypted = true)) {
            dataError = ArchiveException(1, "Incorrect passphrase")
        }
        reader.encrypted shouldBe true
        reader.wrongPassword shouldBe true
    }

    @Test
    fun otherReadFailuresPropagate() {
        mockkObject(CbzCrypto)
        every { CbzCrypto.getDecryptedPasswordCbz() } returns "pw".toByteArray()
        val error = ArchiveException(1, "truncated")
        shouldThrow<ArchiveException> {
            reader(FakeEntry("secret", isEncrypted = true)) { dataError = error }
        } shouldBeSameInstanceAs error
    }

    @Test
    fun publicCtorUsesLibarchive() {
        // The unit-test android.jar throws on every Os call; the real binding never gets further.
        shouldThrow<RuntimeException> { ArchiveReader(pfd) }.message shouldContain "mmap"
    }
}
