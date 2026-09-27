package eu.kanade.presentation.browse

import android.app.Application
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.base.BasePreferences
import eu.kanade.presentation.more.settings.widget.pressDialogBack
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.extension.anInstalledExtension
import eu.kanade.tachiyomi.extension.anUntrustedExtension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsScreenModel
import eu.kanade.tachiyomi.ui.library.ComposableScreen
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class ExtensionScreenTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val untrusted = anUntrustedExtension(pkgName = "odd")

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun show(state: ExtensionsScreenModel.State, query: String?) {
        compose.setContent {
            MaterialTheme {
                Navigator(
                    ComposableScreen {
                        ExtensionScreen(
                            state = state,
                            contentPadding = PaddingValues(),
                            searchQuery = query,
                            onLongClickItem = { events += "long" },
                            onClickItemCancel = { events += "cancel" },
                            onOpenWebView = { events += "webview" },
                            onInstallExtension = { events += "install" },
                            onUninstallExtension = { events += "uninstall ${it.pkgName}" },
                            onUpdateExtension = { events += "update" },
                            onTrustExtension = { events += "trust ${it.pkgName}" },
                            onOpenExtension = { events += "open" },
                            onClickUpdateAll = { events += "all" },
                            onRefresh = { events += "refresh" },
                        )
                    },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun items(): Map<ExtensionUiModel.Header, List<ExtensionUiModel.Item>> = mapOf(
        ExtensionUiModel.Header.Resource(MR.strings.ext_untrusted) to
            listOf(ExtensionUiModel.Item(untrusted, InstallStep.Idle)),
    )

    @Test
    fun emptySearchHasNoResults() {
        show(ExtensionsScreenModel.State(isLoading = false), query = "zzz")
        compose.onNodeWithText("No results found").assertExists()
    }

    @Test
    fun emptyListOffersStores() {
        show(ExtensionsScreenModel.State(isLoading = false), query = "")
        compose.onNodeWithText("Extension stores").performClick()
    }

    @Test
    fun permissionWarningRequests() {
        val state = ExtensionsScreenModel.State(
            isLoading = false,
            items = items(),
            installer = BasePreferences.ExtensionInstaller.PACKAGEINSTALLER,
        )
        show(state, query = null)
        compose.onNodeWithText("Permissions are needed", substring = true).performClick()
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).nextStartedActivity.shouldNotBeNull()
    }

    @Test
    fun trustDialogChoices() {
        show(ExtensionsScreenModel.State(isLoading = false, items = items()), query = null)
        compose.onAllNodesWithContentDescription("Trust")[0].performClick()
        compose.onNodeWithText("Uninstall").performClick()
        compose.onAllNodesWithContentDescription("Trust")[0].performClick()
        compose.onNodeWithText("Trust").performClick()
        events shouldContainExactly listOf("uninstall odd", "trust odd")
    }

    @Test
    fun trustDialogBackDismisses() {
        show(ExtensionsScreenModel.State(isLoading = false, items = items()), query = null)
        compose.onAllNodesWithContentDescription("Trust")[0].performClick()
        compose.waitForIdle()
        pressDialogBack()
        compose.waitForIdle()
        compose.onNodeWithText("Untrusted extension").assertDoesNotExist()
        events shouldContainExactly emptyList()
    }

    @Test
    fun installersAndTextHeaders() {
        val installed = anInstalledExtension(pkgName = "inst")
        val items: Map<ExtensionUiModel.Header, List<ExtensionUiModel.Item>> = mapOf(
            ExtensionUiModel.Header.Text("Plain header") to listOf(ExtensionUiModel.Item(installed, InstallStep.Error)),
        )
        show(ExtensionsScreenModel.State(isLoading = false, items = items, installer = null), query = null)
        compose.onNodeWithText("Plain header").assertExists()
        compose.onAllNodesWithContentDescription("Retry")[0].performClick()
        compose.onNodeWithText("Permissions are needed", substring = true).assertDoesNotExist()
        events shouldContainExactly listOf("open")
    }

    @Test
    fun shizukuNeedsNoPermission() {
        val state = ExtensionsScreenModel.State(
            isLoading = false,
            items = items(),
            installer = BasePreferences.ExtensionInstaller.SHIZUKU,
        )
        show(state, query = null)
        compose.onNodeWithText("Permissions are needed", substring = true).assertDoesNotExist()
    }

    @Test
    fun emptyWithoutQuery() {
        show(ExtensionsScreenModel.State(isLoading = false), query = null)
        compose.onNodeWithText("Extension stores").assertExists()
    }

    @Test
    fun idleUpdateUpdates() {
        val stale = anInstalledExtension(pkgName = "stale", hasUpdate = true)
        val items: Map<ExtensionUiModel.Header, List<ExtensionUiModel.Item>> = mapOf(
            ExtensionUiModel.Header.Text("Updates") to listOf(ExtensionUiModel.Item(stale, InstallStep.Idle)),
        )
        show(ExtensionsScreenModel.State(isLoading = false, items = items), query = null)
        compose.onAllNodesWithContentDescription("Update")[0].performClick()
        events shouldContainExactly listOf("update")
    }

    @Test
    fun trustDialogSeesNewCallbacks() {
        var round by mutableIntStateOf(0)
        compose.setContent {
            MaterialTheme {
                Navigator(
                    ComposableScreen {
                        val tag = round
                        ExtensionScreen(
                            state = ExtensionsScreenModel.State(isLoading = false, items = items()),
                            contentPadding = PaddingValues(),
                            searchQuery = null,
                            onLongClickItem = {},
                            onClickItemCancel = {},
                            onOpenWebView = {},
                            onInstallExtension = {},
                            onUninstallExtension = { events += "uninstall $tag" },
                            onUpdateExtension = {},
                            onTrustExtension = { events += "trust $tag" },
                            onOpenExtension = {},
                            onClickUpdateAll = {},
                            onRefresh = {},
                        )
                    },
                )
            }
        }
        compose.onAllNodesWithContentDescription("Trust")[0].performClick()
        compose.runOnIdle { round++ }
        compose.onNodeWithText("Trust").performClick()
        compose.onAllNodesWithContentDescription("Trust")[0].performClick()
        compose.runOnIdle { round++ }
        compose.onNodeWithText("Uninstall").performClick()
        events shouldContainExactly listOf("trust 1", "uninstall 2")
    }
}
