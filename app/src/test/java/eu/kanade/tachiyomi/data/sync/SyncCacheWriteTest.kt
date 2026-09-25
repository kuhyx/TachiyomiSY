package eu.kanade.tachiyomi.data.sync

import android.content.Context
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.sync.service.manga
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class SyncCacheWriteTest {

    @get:Rule
    val folder = TemporaryFolder()

    private var logged = mutableListOf<String>()

    @Before
    fun setUp() {
        logged = captureLogcat()
    }

    @After
    fun tearDown() = releaseLogcat()

    private fun contextIn(dir: File): Context = mockk { every { cacheDir } returns dir }

    @Test
    fun backupIsWrittenAsProtoBuf() {
        val backup = Backup(backupManga = listOf(manga("a")))
        val uri = writeSyncDataToCache(contextIn(folder.root), backup).shouldNotBeNull()
        val file = File(folder.root, "tachiyomi_sync_data.proto.gz")
        uri.path shouldBe file.path
        ProtoBuf.decodeFromByteArray(Backup.serializer(), file.readBytes()).backupManga.single().url shouldBe "a"
    }

    @Test
    fun unwritableCacheGivesNull() {
        File(folder.root, "tachiyomi_sync_data.proto.gz").mkdir()
        writeSyncDataToCache(contextIn(folder.root), Backup(backupManga = emptyList())).shouldBeNull()
        logged.single() shouldStartWith "Failed to write sync data to cache\n"
    }
}
