package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.extension.anAvailableExtension
import eu.kanade.tachiyomi.extension.anAvailableSource
import eu.kanade.tachiyomi.extension.anInstalledExtension
import eu.kanade.tachiyomi.extension.anUntrustedExtension
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h3000dp")
internal class ExtensionItemTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun show(vararg items: Pair<Extension, InstallStep>) {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    items.forEachIndexed { index, (extension, step) ->
                        val item = ExtensionUiModel.Item(extension, step)
                        val onClick = { ext: Extension -> events += "click ${ext.pkgName}" }
                        if (index == 0) {
                            ExtensionItem(
                                item = item,
                                onClickItem = onClick,
                                onLongClickItem = { events += "long ${it.pkgName}" },
                                onClickItemCancel = { events += "cancel ${it.pkgName}" },
                                onClickItemAction = { events += "action ${it.pkgName}" },
                                onClickItemSecondaryAction = { events += "second ${it.pkgName}" },
                                modifier = Modifier,
                            )
                        } else {
                            ExtensionItem(
                                item = item,
                                onClickItem = onClick,
                                onLongClickItem = { events += "long ${it.pkgName}" },
                                onClickItemCancel = { events += "cancel ${it.pkgName}" },
                                onClickItemAction = { events += "action ${it.pkgName}" },
                                onClickItemSecondaryAction = { events += "second ${it.pkgName}" },
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun tap(label: String, index: Int = 0) {
        compose.onAllNodesWithContentDescription(label)[index].performScrollTo().performClick()
    }

    @Test
    fun installedVariants() {
        show(
            anInstalledExtension(pkgName = "a", hasUpdate = true).copy(isShared = false) to InstallStep.Idle,
            anInstalledExtension(pkgName = "b", isObsolete = true)
                .copy(lang = "", versionName = "") to InstallStep.Idle,
            anInstalledExtension(pkgName = "c", isRedundant = true) to InstallStep.Installed,
            anInstalledExtension(pkgName = "d").copy(isNsfw = true) to InstallStep.Error,
        )
        listOf("Private", "ORPHANED", "REDUNDANT", "18+").forEach { compose.onNodeWithText(it).assertExists() }
        tap("Settings")
        tap("Update")
        tap("Retry")
        compose.onNodeWithText("Ext a").performClick()
        compose.onNodeWithText("Ext a").performTouchInput { longClick() }
        events shouldContainExactly listOf("second a", "action a", "action d", "click a", "long a")
    }

    @Test
    fun inProgressSteps() {
        show(
            anAvailableExtension(pkgName = "p") to InstallStep.Pending,
            anAvailableExtension(pkgName = "q") to InstallStep.Downloading,
            anAvailableExtension(pkgName = "r") to InstallStep.Installing,
        )
        listOf("Pending", "Downloading", "Installing").forEach { compose.onNodeWithText(it).assertExists() }
        tap("Cancel", index = 2)
        events shouldContainExactly listOf("cancel r")
    }

    @Test
    fun availableAndUntrusted() {
        show(
            anAvailableExtension(pkgName = "s", sources = listOf(anAvailableSource(1L))) to InstallStep.Idle,
            anAvailableExtension(pkgName = "t") to InstallStep.Idle,
            anUntrustedExtension(pkgName = "u") to InstallStep.Idle,
        )
        compose.onNodeWithText("UNTRUSTED").assertExists()
        tap("Open in WebView")
        tap("Install", index = 1)
        tap("Trust")
        compose.onAllNodesWithText("Ext t")[0].assertExists()
        events shouldContainExactly listOf("second s", "action t", "action u")
    }
}
