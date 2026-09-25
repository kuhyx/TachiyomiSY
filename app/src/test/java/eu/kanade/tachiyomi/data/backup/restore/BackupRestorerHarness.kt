package eu.kanade.tachiyomi.data.backup.restore

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.backup.BackupKoin
import eu.kanade.tachiyomi.data.backup.BackupNotifier
import eu.kanade.tachiyomi.data.backup.backupBytes
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.restore.restorers.CategoriesRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.ExtensionStoreRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.MangaRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.PreferenceRestorer
import eu.kanade.tachiyomi.data.backup.restore.restorers.SavedSearchRestorer
import io.mockk.coEvery
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File

/** A context whose external cache is [cacheDir]; point it at a plain file to make the error log unwritable. */
internal class CacheContext(base: Context, private val cacheDir: File) : ContextWrapper(base) {
    override fun getExternalCacheDir(): File = cacheDir
}

/** A [BackupRestorer] over mocked restorers and a mocked notifier, reading backups from [dir]. */
internal class BackupRestorerHarness(private val dir: File) {
    val app: Application = ApplicationProvider.getApplicationContext()
    val graph = BackupKoin()
    val notifier: BackupNotifier = mockk(relaxed = true)
    val categoriesRestorer: CategoriesRestorer = mockk(relaxed = true)
    val preferenceRestorer: PreferenceRestorer = mockk(relaxed = true)
    val extensionStoreRestorer: ExtensionStoreRestorer = mockk(relaxed = true)
    val mangaRestorer: MangaRestorer = mockk(relaxed = true)
    val savedSearchRestorer: SavedSearchRestorer = mockk(relaxed = true)

    fun start() {
        startKoin { modules(graph.module()) }
        coEvery { mangaRestorer.sortByNew(any()) } answers { firstArg() }
    }

    fun stop() = stopKoin()

    fun restorer(context: Context = CacheContext(app, dir), isSync: Boolean = false): BackupRestorer = BackupRestorer(
        context = context,
        notifier = notifier,
        isSync = isSync,
        database = graph.database,
        categoriesRestorer = categoriesRestorer,
        preferenceRestorer = preferenceRestorer,
        extensionStoreRestorer = extensionStoreRestorer,
        mangaRestorer = mangaRestorer,
        savedSearchRestorer = savedSearchRestorer,
    )

    fun write(backup: Backup): Uri = Uri.fromFile(File.createTempFile("backup", ".tachibk", dir)).also { uri ->
        File(checkNotNull(uri.path)).writeBytes(backupBytes(backup))
    }
}
