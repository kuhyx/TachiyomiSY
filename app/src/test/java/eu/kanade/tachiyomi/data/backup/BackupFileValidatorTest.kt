package eu.kanade.tachiyomi.data.backup

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
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
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.service.SourceManager
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class BackupFileValidatorTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val sourceManager = mockk<SourceManager>()
    private val trackerManager = mockk<TrackerManager>()

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<ProtoBuf> { ProtoBuf }
                    single { sourceManager }
                    single { trackerManager }
                },
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    private fun file(backup: Backup): Uri = Uri.fromFile(folder.newFile().apply { writeBytes(backupBytes(backup)) })

    private fun tracker(name: String, loggedIn: Boolean): BaseTracker {
        val tracker = mockk<BaseTracker>()
        every { tracker.name } returns name
        every { tracker.isLoggedIn } returns loggedIn
        return tracker
    }

    @Test
    fun unreadableFileIsAnError() {
        val uri = Uri.fromFile(folder.newFile().apply { writeBytes(byteArrayOf(0x7b, 0x7d)) })
        val error = shouldThrow<IllegalStateException> { BackupFileValidator(context).validate(uri) }
        error.cause.shouldBeInstanceOf<IOException>()
    }

    @Test
    fun missingSourcesAreListed() {
        every { sourceManager.get(1L) } returns mockk()
        every { sourceManager.get(2L) } returns null
        every { sourceManager.get(3L) } returns null
        every { sourceManager.getOrStub(3L) } returns StubSource(id = 3L, lang = "en", name = "Stub")
        every { trackerManager.get(1L) } returns tracker("Online", loggedIn = true)
        every { trackerManager.get(2L) } returns tracker("Offline", loggedIn = false)
        every { trackerManager.get(3L) } returns null
        val backup = Backup(
            backupManga = listOf(
                BackupManga(
                    source = 2L,
                    url = "/m",
                    tracking = listOf(1, 2, 3, 2).map { BackupTracking(syncId = it, libraryId = 0L) },
                ),
            ),
            backupSources = listOf(
                BackupSource(name = "Known", sourceId = 1L),
                BackupSource(name = "Missing", sourceId = 2L),
                BackupSource(name = "3", sourceId = 3L),
            ),
        )
        val results = BackupFileValidator(context, sourceManager, trackerManager).validate(file(backup))
        results.missingSources shouldBe listOf("Missing", "Stub (EN)")
        results.missingTrackers shouldBe listOf("Offline")
        results shouldBe BackupFileValidator.Results(listOf("Missing", "Stub (EN)"), listOf("Offline"))
    }

    @Test
    fun cleanBackupHasNothingMissing() {
        val results = BackupFileValidator(context).validate(file(Backup(backupManga = listOf(BackupManga(1L, "/m")))))
        results shouldBe BackupFileValidator.Results(emptyList(), emptyList())
        results.copy(missingSources = listOf("x")).toString() shouldBe
            "Results(missingSources=[x], missingTrackers=[])"
    }
}
