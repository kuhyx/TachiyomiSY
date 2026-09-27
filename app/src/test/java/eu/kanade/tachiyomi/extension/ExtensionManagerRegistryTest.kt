package eu.kanade.tachiyomi.extension

import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.ColorDrawable
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.api.ExtensionUpdateNotifier
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.source.Source
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class ExtensionManagerRegistryTest {

    private val harness = ExtensionManagerHarness()
    private val source = mockk<Source> { every { id } returns SOURCE }

    @Before
    fun setUp() {
        harness.start()
        harness.loaded = listOf(LoadResult.Success(anInstalledExtension("pkg.one", sources = listOf(source))))
    }

    @After
    fun tearDown() = harness.stop()

    private fun notice() = shadowOf(harness.context.getSystemService(NotificationManager::class.java))
        .getNotification(Notifications.ID_UPDATES_TO_EXTS)

    private fun ready() = harness.manager().apply { awaitInstalled { it.isNotEmpty() } }

    @Test
    fun packageIsFoundBySource() = runTest {
        val manager = ready()
        manager.getExtensionPackage(SOURCE) shouldBe "pkg.one"
        manager.getExtensionPackage(OTHER) shouldBe null
        manager.getExtensionPackageAsFlow(SOURCE).first() shouldBe "pkg.one"
        manager.getExtensionPackageAsFlow(OTHER).first() shouldBe null
    }

    @Test
    fun iconsAreCachedPerPackage() {
        val manager = ready()
        val cached = ColorDrawable()
        manager.iconMap["pkg.one"] = cached
        manager.getAppIconForSource(SOURCE) shouldBe cached
    }

    @Test
    fun missingIconIsLoaded() {
        val manager = ready()
        every { ExtensionLoader.getExtensionPackageInfo(any(), "pkg.one") } returns PackageInfo().apply {
            applicationInfo = ApplicationInfo().apply { packageName = harness.context.packageName }
        }
        manager.getAppIconForSource(SOURCE) shouldNotBe null
        manager.iconMap.keys shouldBe setOf("pkg.one")
    }

    @Test
    fun builtInSourcesHaveIcons() {
        val manager = harness.manager()
        manager.getAppIconForSource(EH_SOURCE_ID) shouldNotBe null
        manager.getAppIconForSource(EXH_SOURCE_ID) shouldNotBe null
        manager.getAppIconForSource(MERGED_SOURCE_ID) shouldNotBe null
        manager.getAppIconForSource(OTHER) shouldBe null
    }

    @Test
    fun blacklistedNeverRegisters() {
        val manager = harness.manager()
        manager.registerNewExtension(anInstalledExtension(BLACKLISTED_PKG))
        manager.registerUpdatedExtension(anInstalledExtension(BLACKLISTED_PKG))
        manager.installedExtensionMapFlow.value.keys shouldBe setOf("pkg.one")
    }

    @Test
    fun registrationsAreKept() {
        val manager = harness.manager()
        manager.registerNewExtension(anInstalledExtension("pkg.two"))
        manager.registerUpdatedExtension(anInstalledExtension("pkg.one", versionCode = 7))
        manager.installedExtensionMapFlow.value.keys shouldBe setOf("pkg.one", "pkg.two")
        manager.installedExtensionMapFlow.value.getValue("pkg.one").versionCode shouldBe 7L
        manager.unregisterExtension("pkg.two")
        manager.installedExtensionMapFlow.value.keys shouldBe setOf("pkg.one")
    }

    @Test
    fun noUpdatesDismissesTheNotice() {
        val manager = harness.manager()
        manager.registerNewExtension(anInstalledExtension("pkg.two", hasUpdate = true))
        manager.updatePendingUpdatesCount()
        harness.preferences.extensionUpdatesCount.get() shouldBe 1
        ExtensionUpdateNotifier(harness.context).promptUpdates(listOf("Ext"))
        notice() shouldNotBe null
        manager.unregisterExtension("pkg.two")
        manager.updatePendingUpdatesCount()
        harness.preferences.extensionUpdatesCount.get() shouldBe 0
        notice() shouldBe null
    }

    private companion object {
        const val SOURCE = 5L
        const val OTHER = 6L
    }
}
