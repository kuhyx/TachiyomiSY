package eu.kanade.tachiyomi.ui.browse.extension

import android.content.pm.PackageInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.domain.extension.interactor.GetExtensionsByType
import eu.kanade.domain.extension.interactor.available
import eu.kanade.domain.extension.interactor.installed
import eu.kanade.domain.extension.interactor.untrusted
import eu.kanade.domain.extension.model.Extensions
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.TabHost
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class ExtensionsTabTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = BrowseKoin()
    private val stale = installed("Stale", hasUpdate = true)
    private val mine = installed("Mine")
    private val far = available("Far", listOf(5L to "en"))
    private val odd = untrusted("Odd")
    private val extensions = MutableStateFlow(Extensions(listOf(stale), listOf(mine), listOf(far), listOf(odd)))
    private val steps = MutableSharedFlow<InstallStep>()
    private val manager = mockk<ExtensionManager>(relaxed = true) {
        every { installer.downloadAndInstall(any(), any()) } returns steps
        every { availableExtensionMapFlow } returns MutableStateFlow(mapOf(stale.pkgName to far))
        every { untrustedExtensionMapFlow } returns MutableStateFlow(emptyMap())
    }

    @Before
    fun setUp() = koin.start(
        module {
            single { manager }
            single<GetExtensionsByType> { mockk { every { subscribe() } returns extensions } }
        },
    )

    @After
    fun tearDown() = koin.stop()

    private fun host(): Pair<TabHost, ExtensionsScreenModel> {
        val model = ExtensionsScreenModel()
        val host = TabHost { extensionsTab(model) }
        host.show(compose)
        compose.waitUntil(timeoutMillis = 10_000) { !model.state.value.isLoading }
        compose.waitForIdle()
        return host to model
    }

    private fun installedPackage(name: String) {
        shadowOf(koin.app.packageManager).installPackage(PackageInfo().apply { packageName = name })
    }

    @Test
    fun clickingOpensOrInstalls() {
        val (host) = host()
        compose.onNodeWithText("Mine").performClick()
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<ExtensionDetailsScreen>()
        compose.onNodeWithText("Far").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { steps.subscriptionCount.value == 1 }
    }

    @Test
    fun updatesAreOffered() {
        host()
        compose.onNodeWithText("Update all").performClick()
        compose.onAllNodesWithContentDescription("Update")[0].performClick()
        compose.waitForIdle()
        verify(atLeast = 1) { manager.availableExtensionMapFlow }
    }

    @Test
    fun secondaryActionsOpenScreens() {
        val (host) = host()
        compose.onAllNodesWithContentDescription("Open in WebView")[0].performClick()
        compose.waitForIdle()
        host.navigator.lastItem::class.simpleName shouldBe "WebViewScreen"
        compose.onAllNodesWithContentDescription("Settings")[0].performClick()
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<ExtensionDetailsScreen>()
    }

    @Test
    fun untrustedAsksFirst() {
        host()
        compose.onNodeWithText("Odd").performClick()
        compose.onNodeWithText("Trust").performClick()
        compose.waitForIdle()
        verify { manager.untrustedExtensionMapFlow }
        compose.onAllNodesWithContentDescription("Trust")[0].performClick()
        compose.onNodeWithText("Uninstall").performClick()
        compose.waitForIdle()
        verify { manager.installer.uninstallApk(odd.pkgName) }
    }

    @Test
    fun longClickInstallsOrRemoves() {
        installedPackage(mine.pkgName)
        host()
        compose.onNodeWithText("Far").performTouchInput { longClick() }
        compose.waitUntil(timeoutMillis = 5_000) { steps.subscriptionCount.value == 1 }
        compose.onNodeWithText("Mine").performTouchInput { longClick() }
        verify { manager.installer.uninstallApk(mine.pkgName) }
    }

    @Test
    fun privateExtensionsAskToRemove() {
        host()
        compose.onNodeWithText("Stale").performTouchInput { longClick() }
        compose.onNodeWithText("Remove Extension?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Stale").performTouchInput { longClick() }
        compose.onNodeWithText("Remove").performClick()
        compose.waitForIdle()
        verify { manager.installer.uninstallApk(stale.pkgName) }
    }

    @Test
    fun pendingInstallsCanBeCancelled() {
        host()
        compose.onNodeWithText("Far").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { steps.subscriptionCount.value == 1 }
        runBlocking { steps.emit(InstallStep.Pending) }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription("Cancel").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithContentDescription("Cancel")[0].performClick()
        verify { manager.installer.cancelInstall(far.pkgName) }
    }

    @Test
    fun overflowAndBack() {
        val (host, model) = host()
        host.content.titleRes shouldBe MR.strings.label_extensions
        host.content.badgeNumber shouldBe null
        host.action("Filter")
        compose.waitForIdle()
        host.navigator.lastItem.shouldBeInstanceOf<ExtensionFilterScreen>()
        host.action("Extension stores")
        model.search("mine")
        compose.waitForIdle()
        host.pressBack(compose)
        model.state.value.searchQuery shouldBe null
    }

    @Test
    fun emptyStoreOffersStores() {
        extensions.value = Extensions(emptyList(), emptyList(), emptyList(), emptyList())
        koin.sourcePreferences.extensionUpdatesCount.set(3)
        val (host, model) = host()
        compose.waitUntil(timeoutMillis = 5_000) { host.content.badgeNumber == 3 }
        compose.onNodeWithText("Extension stores").performClick()
        compose.waitForIdle()
        host.navigator.lastItem::class.simpleName shouldBe "ExtensionStoresScreen"
        compose.onRoot().performTouchInput { swipeDown() }
        compose.waitUntil(timeoutMillis = 5_000) { model.state.value.isRefreshing }
    }
}
