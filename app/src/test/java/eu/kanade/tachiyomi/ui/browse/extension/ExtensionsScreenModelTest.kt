package eu.kanade.tachiyomi.ui.browse.extension

import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.domain.extension.interactor.available
import eu.kanade.domain.extension.interactor.installed
import eu.kanade.domain.extension.interactor.untrusted
import eu.kanade.domain.extension.model.Extensions
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class ExtensionsScreenModelTest {
    private val koin = BrowseKoin()
    private val extensions = MutableStateFlow(Extensions(emptyList(), emptyList(), emptyList(), emptyList()))
    private val getExtensions = mockk<GetExtensionsByType> { every { subscribe() } returns extensions }
    private val manager = mockk<ExtensionManager>(relaxed = true)

    @Before
    fun setUp() = koin.start(
        module {
            single { manager }
            single { getExtensions }
        },
    )

    @After
    fun tearDown() = koin.stop()

    private fun model() = ExtensionsScreenModel().also { model -> eventually { !model.state.value.isLoading } }

    @Test
    fun emptyStoreIsEmpty() {
        val state = model().state.value
        state.isEmpty shouldBe true
        state.copy(updates = 2).updates shouldBe 2
    }

    @Test
    fun groupsBySection() {
        val update = installed("Upd", hasUpdate = true)
        extensions.value = Extensions(
            updates = listOf(update),
            installed = listOf(installed("Mine")),
            available = listOf(available("Far", listOf(1L to "en")), available("Near", listOf(2L to "en"))),
            untrusted = listOf(untrusted("Odd")),
        )
        val model = model()
        eventually { model.state.value.items.size == 3 }
        val headers = model.state.value.items.keys.toList()
        headers[0] shouldBe ExtensionUiModel.Header.Resource(MR.strings.ext_updates_pending)
        headers[1] shouldBe ExtensionUiModel.Header.Resource(MR.strings.ext_installed)
        model.state.value.items.getValue(headers[1]).map { it.extension.name } shouldBe listOf("Mine", "Odd")
        model.state.value.isEmpty shouldBe false
    }

    @Test
    fun untrustedShowsAsInstalled() {
        extensions.value = Extensions(emptyList(), emptyList(), emptyList(), listOf(untrusted("Odd")))
        val model = model()
        eventually { model.state.value.items.size == 1 }
    }

    @Test
    fun searchFiltersTheList() {
        extensions.value =
            Extensions(emptyList(), listOf(installed("Mine"), installed("Other")), emptyList(), emptyList())
        val model = model()
        model.search("mine")
        eventually { model.state.value.items.values.flatten().size == 1 }
        model.search(null)
        eventually { model.state.value.items.values.flatten().size == 2 }
    }

    @Test
    fun predicateMatchesNames() {
        val predicate = model().searchQueryPredicate(" , mine ,")
        predicate(installed("Mine")) shouldBe true
        predicate(untrusted("Odd")) shouldBe false
        model().searchQueryPredicate(" ")(untrusted("Odd")) shouldBe true
    }

    @Test
    fun predicateMatchesInstalled() {
        val http = mockk<HttpSource> {
            every { name } returns "Site"
            every { id } returns 9L
            every { getHomeUrl() } returns "https://home.example"
        }
        val plain = mockk<Source> {
            every { name } returns "Plain"
            every { id } returns 8L
        }
        val ext = installed("Ext").copy(sources = listOf(plain, http))
        val model = model()
        model.searchQueryPredicate("site")(ext) shouldBe true
        model.searchQueryPredicate("home.example")(ext) shouldBe true
        model.searchQueryPredicate("9")(ext) shouldBe true
        model.searchQueryPredicate("none")(ext) shouldBe false
    }

    @Test
    fun predicateMatchesAvailable() {
        val ext = available("Ext", listOf(5L to "en"))
        val model = model()
        model.searchQueryPredicate("ext en")(ext) shouldBe true
        model.searchQueryPredicate("https://ext")(ext) shouldBe true
        model.searchQueryPredicate("5")(ext) shouldBe true
        model.searchQueryPredicate("none")(ext) shouldBe false
    }

    @Test
    fun refreshTogglesTheFlag() {
        val model = model()
        model.findAvailableExtensions()
        eventually { model.state.value.isRefreshing }
        eventually { !model.state.value.isRefreshing }
    }

    @Test
    fun preferencesReachTheState() {
        val model = model()
        koin.sourcePreferences.extensionUpdatesCount.set(4)
        // SHIZUKU would fall back to the default here: Shizuku is not installed under Robolectric.
        koin.basePreferences.extensionInstaller.set(BasePreferences.ExtensionInstaller.PRIVATE)
        eventually { model.state.value.updates == 4 }
        eventually { model.state.value.installer == BasePreferences.ExtensionInstaller.PRIVATE }
        ExtensionUiModel.Header.Text("en").copy().text shouldBe "en"
        ExtensionUiModel.Item(installed("a"), InstallStep.Idle).copy().installStep shouldBe InstallStep.Idle
    }
}
