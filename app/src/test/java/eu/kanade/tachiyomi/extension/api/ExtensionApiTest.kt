package eu.kanade.tachiyomi.extension.api

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.anAvailableExtension
import eu.kanade.tachiyomi.extension.anInstalledExtension
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.source.online.MemoPreferenceStore
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.repository.ExtensionStoreRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.preference.Preference
import java.time.Instant

private const val BLACKLISTED = "eu.kanade.tachiyomi.extension.all.ehentai"

@RunWith(RobolectricTestRunner::class)
internal class ExtensionApiTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val store = MemoPreferenceStore()
    private val repository = mockk<ExtensionStoreRepository>()
    private val extensionManager = mockk<ExtensionManager>()
    private val sourcePreferences = SourcePreferences(store)
    private var loaded: List<LoadResult> = emptyList()

    @Before
    fun setUp() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                Notifications.CHANNEL_EXTENSIONS_UPDATE,
                "Extension updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        coEvery { repository.refreshAll() } returns Unit
        mockkObject(ExtensionLoader)
        every { ExtensionLoader.loadExtensions(any()) } answers { loaded }
        stopKoin()
        startKoin {
            modules(
                module {
                    single { repository }
                    single { sourcePreferences }
                    single { SecurityPreferences(store) }
                    single { UpdateExtensionStores(repository) }
                    single { extensionManager }
                    single<tachiyomi.core.common.preference.PreferenceStore> { store }
                },
            )
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun lastCheck(): Preference<Long> = store.getLong(Preference.appStateKey("last_ext_check"), 0)

    @Test
    fun findExtensionsAsksTheRepo() = runTest {
        coEvery { repository.fetchExtensions() } returns listOf(anAvailableExtension())
        ExtensionApi().findExtensions().map { it.pkgName } shouldContainExactly listOf("pkg.one")
    }

    @Test
    fun aCheckWithinADayIsSkipped() = runTest {
        lastCheck().set(Instant.now().toEpochMilli())
        ExtensionApi().checkForUpdates(context).shouldBeNull()
    }

    @Test
    fun aNewerVersionIsAnUpdate() = runTest {
        coEvery { repository.fetchExtensions() } returns listOf(anAvailableExtension(versionCode = 9L))
        loaded = listOf(LoadResult.Success(anInstalledExtension()))
        val updates = ExtensionApi().checkForUpdates(context)
        updates?.map { it.pkgName } shouldContainExactly listOf("pkg.one")
        (lastCheck().get() > 0) shouldBe true
    }

    @Test
    fun aNewerLibVersionIsAnUpdate() = runTest {
        loaded = listOf(LoadResult.Success(anInstalledExtension(libVersion = 1.4)))
        every { extensionManager.availableExtensionsFlow } returns MutableStateFlow(
            listOf(anAvailableExtension(versionCode = 1L)),
        )
        val updates = ExtensionApi().checkForUpdates(context, fromAvailableExtensionList = true)
        updates?.map { it.pkgName } shouldContainExactly listOf("pkg.one")
        // The available list was used, so the daily check timestamp stays untouched.
        lastCheck().get() shouldBe 0L
    }

    @Test
    fun anUpToDateExtension() = runTest {
        coEvery { repository.fetchExtensions() } returns listOf(anAvailableExtension(versionCode = 1L))
        loaded = listOf(LoadResult.Success(anInstalledExtension(versionCode = 1L)))
        ExtensionApi().checkForUpdates(context)?.isEmpty() shouldBe true
        shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications.isEmpty() shouldBe true
    }

    @Test
    fun anExtensionNoStoreLists() = runTest {
        coEvery { repository.fetchExtensions() } returns listOf(anAvailableExtension(pkgName = "pkg.other"))
        loaded = listOf(LoadResult.Success(anInstalledExtension()))
        ExtensionApi().checkForUpdates(context)?.isEmpty() shouldBe true
    }

    @Test
    fun blacklistedAreDropped() = runTest {
        sourcePreferences.enableSourceBlacklist.set(true)
        coEvery { repository.fetchExtensions() } returns listOf(
            anAvailableExtension(pkgName = BLACKLISTED, versionCode = 9L),
        )
        loaded = listOf(LoadResult.Success(anInstalledExtension(pkgName = BLACKLISTED)))
        ExtensionApi().checkForUpdates(context)?.isEmpty() shouldBe true
    }

    @Test
    fun blacklistedSurviveWhenOff() = runTest {
        sourcePreferences.enableSourceBlacklist.set(false)
        coEvery { repository.fetchExtensions() } returns listOf(
            anAvailableExtension(pkgName = BLACKLISTED, versionCode = 9L),
        )
        loaded = listOf(LoadResult.Success(anInstalledExtension(pkgName = BLACKLISTED)))
        ExtensionApi().checkForUpdates(context)?.map { it.pkgName } shouldContainExactly listOf(BLACKLISTED)
        shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications.size shouldBe 1
    }

    @Test
    fun untrustedResultsAreNotUpdated() = runTest {
        coEvery { repository.fetchExtensions() } returns listOf(anAvailableExtension(versionCode = 9L))
        loaded = listOf(LoadResult.Error)
        ExtensionApi().checkForUpdates(context)?.isEmpty() shouldBe true
    }

    @Test
    fun theAvailableSourceStub() {
        val source: Extension.Available.Source =
            eu.kanade.tachiyomi.extension.anAvailableSource(id = 3L, lang = "fr")
        source.toStubSource().id shouldBe 3L
        source.toStubSource().lang shouldBe "fr"
    }
}
