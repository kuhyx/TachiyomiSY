package eu.kanade.tachiyomi.data.backup

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream

/** A backup as the app writes it: protobuf, gzipped unless [gzip] is off. */
internal fun backupBytes(backup: Backup, gzip: Boolean = true): ByteArray {
    val plain = ProtoBuf.encodeToByteArray(Backup.serializer(), backup)
    if (!gzip) return plain
    return ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(plain) } }.toByteArray()
}

@RunWith(RobolectricTestRunner::class)
internal class BackupDecoderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val backup = Backup(backupManga = listOf(BackupManga(source = 1L, url = "/m", title = "M")))

    @Before
    fun setUp() {
        startKoin { modules(module { single<ProtoBuf> { ProtoBuf } }) }
    }

    @After
    fun tearDown() = stopKoin()

    private fun file(bytes: ByteArray): Uri = Uri.fromFile(folder.newFile().apply { writeBytes(bytes) })

    @Test
    fun gzippedBackupDecodes() {
        BackupDecoder(context).decode(file(backupBytes(backup))).backupManga.single().url shouldBe "/m"
    }

    @Test
    fun plainBackupDecodes() {
        val decoded = BackupDecoder(context, ProtoBuf).decode(file(backupBytes(backup, gzip = false)))
        decoded.backupManga.single().title shouldBe "M"
    }

    @Test
    fun jsonBackupsAreRejected() {
        listOf("{}", "{\"a\":1}", "{\n}").forEach { json ->
            shouldThrow<IOException> { BackupDecoder(context).decode(file(json.toByteArray())) }.message shouldBe
                "JSON backup not supported"
        }
    }

    @Test
    fun garbageIsRejected() {
        shouldThrow<IOException> { BackupDecoder(context).decode(file(byteArrayOf(0x0A, 0x7F))) }.message shouldBe
            "Backup file is corrupted"
    }
}
