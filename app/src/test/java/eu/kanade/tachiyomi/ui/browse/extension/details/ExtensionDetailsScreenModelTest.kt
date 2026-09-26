package eu.kanade.tachiyomi.ui.browse.extension.details

import eu.kanade.domain.extension.interactor.ExtensionSourceItem
import eu.kanade.domain.extension.interactor.GetExtensionSources
import eu.kanade.domain.extension.interactor.installed
import eu.kanade.domain.source.interactor.ToggleIncognito
import eu.kanade.domain.source.interactor.ToggleSource
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExtensionDetailsScreenModelTest {
    private val koin = BrowseKoin()
    private val network = mockk<NetworkHelper>(relaxed = true)
    private val installedFlow = MutableStateFlow<List<Extension.Installed>>(emptyList())
    private val manager = mockk<ExtensionManager>(relaxed = true) {
        every { installedExtensionsFlow } returns installedFlow
    }
    private val getSources = mockk<GetExtensionSources>()
    private val toggleSource = mockk<ToggleSource>(relaxed = true)
    private val toggleIncognito = mockk<ToggleIncognito>(relaxed = true)
    private val http = mockk<HttpSource>(relaxed = true) {
        every { id } returns 2L
        every { name } returns "Site"
        every { lang } returns "en"
        every { baseUrl } returns "https://site.example"
        every { getHomeUrl() } returns "https://site.example"
    }
    private val plain = mockk<Source>(relaxed = true) {
        every { id } returns 3L
        every { name } returns "Plain"
        every { lang } returns "fr"
    }
    private val ext = installed("Ext").copy(sources = listOf(http, plain))

    @Before
    fun setUp() = koin.start(
        module {
            single { network }
            single { manager }
            single { getSources }
            single { toggleSource }
            single { toggleIncognito }
        },
    )

    @After
    fun tearDown() = koin.stop()

    private fun model() = ExtensionDetailsScreenModel(ext.pkgName, koin.app)

    @Test
    fun sourcesAreSorted() {
        every { getSources.subscribe(any()) } returns MutableStateFlow(
            listOf(
                ExtensionSourceItem(plain, enabled = true, labelAsName = false),
                ExtensionSourceItem(http, enabled = false, labelAsName = true),
                ExtensionSourceItem(http, enabled = true, labelAsName = true),
            ),
        )
        installedFlow.value = listOf(ext)
        val model = model()
        eventually { !model.state.value.isLoading }
        model.state.value.sources.map { it.enabled } shouldBe listOf(true, true, false)
    }

    @Test
    fun sourceErrorsLeaveNoSources() {
        every { getSources.subscribe(any()) } returns flow { error("db") }
        installedFlow.value = listOf(ext)
        val model = model()
        eventually { !model.state.value.isLoading }
        model.state.value.sources shouldBe emptyList()
    }

    @Test
    fun uninstallIsReported() {
        val model = model()
        runBlocking { withTimeout(10_000L) { model.events.first() } } shouldBe ExtensionDetailsEvent.Uninstalled
        model.clearCookies()
        model.uninstallExtension()
        model.toggleSources(enable = true)
        model.toggleIncognito(enable = true)
        verify(exactly = 0) { toggleIncognito.await(any(), any()) }
        model.state.value.isLoading shouldBe true
    }

    @Test
    fun actionsReachCollaborators() {
        every { getSources.subscribe(any()) } returns MutableStateFlow(emptyList())
        installedFlow.value = listOf(ext)
        val model = model()
        eventually { model.state.value.extension != null }
        model.uninstallExtension()
        verify { manager.installer.uninstallApk(ext.pkgName) }
        model.toggleSource(2L)
        verify { toggleSource.await(2L, any()) }
        model.toggleSources(enable = false)
        verify { toggleSource.await(listOf(2L, 3L), false) }
        model.toggleIncognito(enable = true)
        verify { toggleIncognito.await(ext.pkgName, true) }
    }

    @Test
    fun clearsCookiesPerUrl() {
        every { getSources.subscribe(any()) } returns MutableStateFlow(emptyList())
        every { network.cookieJar.remove(any()) } returns 2 andThenThrows IllegalStateException("jar")
        installedFlow.value = listOf(ext)
        val model = model()
        eventually { model.state.value.extension != null }
        model.clearCookies()
        verify(exactly = 1) { network.cookieJar.remove(any()) }
        model.clearCookies()
    }

    @Test
    fun incognitoFollowsThePreference() {
        every { getSources.subscribe(any()) } returns MutableStateFlow(emptyList())
        val model = model()
        koin.sourcePreferences.incognitoExtensions.set(setOf(ext.pkgName))
        eventually { model.state.value.isIncognito }
    }
}
