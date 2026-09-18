package tachiyomi.domain.storage.service

import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.storage.FolderProvider
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
internal class StorageManagerTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private lateinit var firstRoot: File
    private lateinit var secondRoot: File

    @Before
    fun setUp() {
        firstRoot = Files.createTempDirectory("storage-first").toFile()
        secondRoot = Files.createTempDirectory("storage-second").toFile()
    }

    @After
    fun tearDown() {
        firstRoot.deleteRecursively()
        secondRoot.deleteRecursively()
    }

    @Test
    fun foldersFollowBaseDirectory() = runTest {
        val manager = storageManager(initial = firstRoot.fileUri(), next = secondRoot.fileUri())
        manager.getDownloadsDirectory()?.filePath shouldBe File(firstRoot, "downloads").path

        manager.changes.first()

        manager.getAutomaticBackupsDirectory()?.filePath shouldBe File(secondRoot, "autobackup").path
        manager.getDownloadsDirectory()?.filePath shouldBe File(secondRoot, "downloads").path
        manager.getLocalSourceDirectory()?.filePath shouldBe File(secondRoot, "local").path
        manager.getLogsDirectory()?.filePath shouldBe File(secondRoot, "logs").path
        File(secondRoot, "downloads/.nomedia").isFile shouldBe true
        File(secondRoot, "local").isDirectory shouldBe true
    }

    @Test
    fun missingBaseGivesNothing() = runTest {
        val missing = File(secondRoot, "gone").fileUri()
        val manager = storageManager(initial = "https://example.org/not-a-folder", next = missing)
        manager.getAutomaticBackupsDirectory() shouldBe null

        manager.changes.first()

        manager.getAutomaticBackupsDirectory() shouldBe null
        manager.getDownloadsDirectory() shouldBe null
        manager.getLocalSourceDirectory() shouldBe null
        manager.getLogsDirectory() shouldBe null
        File(secondRoot, "gone").exists() shouldBe false
    }

    private fun storageManager(initial: String, next: String): StorageManager {
        val preference = ScriptedPreference(initial, flowOf(initial, next))
        val store: PreferenceStore = mockk {
            every { getString(any(), any()) } returns preference
        }
        val folderProvider: FolderProvider = mockk {
            every { path() } returns initial
        }
        return StorageManager(context, StoragePreferences(folderProvider, store))
    }

    private fun File.fileUri(): String = "file://$absolutePath"
}

/** A string preference whose [changes] is the scripted [flow], so the manager's collector sees a move. */
private class ScriptedPreference(
    private var current: String,
    private val flow: Flow<String>,
) : Preference<String> {
    private val initial = current

    override fun key(): String = "storage_dir"

    override fun get(): String = current

    override fun set(value: String) {
        current = value
    }

    override fun isSet(): Boolean = current != initial

    override fun delete() {
        current = initial
    }

    override fun defaultValue(): String = initial

    override fun changes(): Flow<String> = flow

    override fun stateIn(scope: CoroutineScope): StateFlow<String> =
        changes().stateIn(scope, SharingStarted.Eagerly, get())
}
