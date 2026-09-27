package eu.kanade.tachiyomi.extension

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.LoadResult
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
internal class ExtensionManagerAvailableTest {

    private val harness = ExtensionManagerHarness()
    private val locale = Locale.getDefault()

    @Before
    fun setUp() {
        harness.start()
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Locale.setDefault(locale)
        Dispatchers.resetMain()
        harness.stop()
    }

    private fun installed(vararg extensions: Extension.Installed) {
        harness.loaded = extensions.map { LoadResult.Success(it) }
    }

    @Test
    fun apiFailureToasts() = runTest {
        coEvery { harness.repository.fetchExtensions() } throws IllegalStateException("offline")
        val manager = harness.manager()
        manager.findAvailableExtensions()
        ShadowToast.getTextOfLatestToast() shouldBe "Failed to fetch available extensions"
        manager.availableExtensionMapFlow.value shouldBe emptyMap()
    }

    @Test
    fun listingsAreStored() = runTest {
        val listing = anAvailableExtension(sources = listOf(anAvailableSource(3)))
        coEvery { harness.repository.fetchExtensions() } returns listOf(listing)
        val manager = harness.manager()
        manager.findAvailableExtensions()
        manager.availableExtensionMapFlow.value shouldBe mapOf("pkg.one" to listing)
        manager.getSourceData(3)?.id shouldBe 3L
        manager.subLanguagesEnabledOnFirstRun shouldBe true
    }

    @Test
    fun subLanguagesJoinOnFirstRun() {
        Locale.setDefault(Locale.CHINESE)
        val manager = harness.manager()
        val langs = listOf("zh", "zh-Hans", "zh-Hant", "en")
            .mapIndexed { i, lang -> anAvailableSource(i.toLong(), lang) }
        manager.enableAdditionalSubLanguages(listOf(anAvailableExtension(sources = langs)))
        harness.preferences.enabledLanguages.get() shouldBe
            harness.preferences.enabledLanguages.defaultValue() + setOf("zh-Hans", "zh-Hant")
    }

    @Test
    fun subLanguagesOnlyOnce() {
        val manager = harness.manager()
        manager.enableAdditionalSubLanguages(emptyList())
        manager.subLanguagesEnabledOnFirstRun shouldBe false
        manager.subLanguagesEnabledOnFirstRun = true
        manager.enableAdditionalSubLanguages(listOf(anAvailableExtension(sources = listOf(anAvailableSource(1, "xx")))))
        harness.preferences.enabledLanguages.isSet() shouldBe false
    }

    @Test
    fun noListingsClearsTheCount() {
        harness.preferences.extensionUpdatesCount.set(4)
        harness.manager().updateInstalledStatuses(emptyList())
        harness.preferences.extensionUpdatesCount.get() shouldBe 0
    }

    @Test
    fun unlistedBecomeObsolete() {
        installed(anInstalledExtension("pkg.gone"), anInstalledExtension("pkg.old", isObsolete = true))
        val manager = harness.manager()
        manager.updateInstalledStatuses(listOf(anAvailableExtension("pkg.other")))
        manager.installedExtensionMapFlow.value.getValue("pkg.gone").isObsolete shouldBe true
    }

    @Test
    fun nothingToChangeKeepsTheMap() {
        installed(anInstalledExtension("pkg.old", isObsolete = true))
        val manager = harness.manager()
        val before = manager.installedExtensionMapFlow.value
        manager.updateInstalledStatuses(listOf(anAvailableExtension("pkg.other")))
        (manager.installedExtensionMapFlow.value === before) shouldBe true
    }

    @Test
    fun blacklistedBecomeRedundant() {
        installed(anInstalledExtension(BLACKLISTED_PKG))
        val manager = harness.manager()
        val listing = listOf(anAvailableExtension(BLACKLISTED_PKG, versionCode = 9))
        manager.updateInstalledStatuses(listing)
        manager.installedExtensionMapFlow.value.getValue(BLACKLISTED_PKG).isRedundant shouldBe true
        manager.updateInstalledStatuses(listing)
        manager.installedExtensionMapFlow.value.getValue(BLACKLISTED_PKG).hasUpdate shouldBe true
    }

    @Test
    fun listedGetUpdateFlags() {
        installed(anInstalledExtension("pkg.a", versionCode = 1), anInstalledExtension("pkg.b", versionCode = 5))
        val manager = harness.manager()
        manager.updateInstalledStatuses(
            listOf(anAvailableExtension("pkg.a", versionCode = 2), anAvailableExtension("pkg.b", versionCode = 2)),
        )
        manager.installedExtensionMapFlow.value.getValue("pkg.a").hasUpdate shouldBe true
        manager.installedExtensionMapFlow.value.getValue("pkg.b").hasUpdate shouldBe false
        manager.installedExtensionMapFlow.value.getValue("pkg.b").store shouldBe fixtureStore
        harness.preferences.extensionUpdatesCount.get() shouldBe 1
    }
}
