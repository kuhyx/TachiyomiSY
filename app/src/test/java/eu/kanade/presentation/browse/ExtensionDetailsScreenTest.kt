package eu.kanade.presentation.browse

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import eu.kanade.domain.extension.interactor.ExtensionSourceItem
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.extension.anInstalledExtension
import eu.kanade.tachiyomi.extension.fixtureStore
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreenModel
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h3000dp")
internal class ExtensionDetailsScreenTest {
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val uriHandler = object : UriHandler {
        override fun openUri(uri: String) {
            events += "open $uri"
        }
    }

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private fun show(state: ExtensionDetailsScreenModel.State) {
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides uriHandler) {
                MaterialTheme {
                    ExtensionDetailsScreen(
                        navigateUp = { events += "up" },
                        state = state,
                        onClickSourcePreferences = { events += "prefs $it" },
                        onClickEnableAll = { events += "enable" },
                        onClickDisableAll = { events += "disable" },
                        onClickClearCookies = { events += "cookies" },
                        onClickUninstall = { events += "uninstall" },
                        onClickSource = { events += "source $it" },
                        onClickIncognito = { events += "incognito $it" },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun sources(): List<ExtensionSourceItem> {
        val plain = mockk<Source>(relaxed = true) {
            every { id } returns 1L
            every { lang } returns "en"
        }
        val configurable = mockk<ConfigurableSource>(relaxed = true) {
            every { id } returns 2L
            every { lang } returns "ja"
        }
        every { configurable.toString() } returns "Conf (JA)"
        return listOf(
            ExtensionSourceItem(plain, enabled = true, labelAsName = false),
            ExtensionSourceItem(configurable, enabled = false, labelAsName = true),
        )
    }

    @Test
    fun emptyWithoutExtension() {
        show(ExtensionDetailsScreenModel.State())
        compose.onNodeWithText("Well, this is awkward", substring = true).assertExists()
        compose.onNodeWithContentDescription("Open source repo").assertDoesNotExist()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly listOf("up")
    }

    @Test
    fun githubRepoAndOverflow() {
        val store = fixtureStore.copy(indexUrl = "https://raw.githubusercontent.com/me/repo/main/index.json")
        val extension = anInstalledExtension(sources = emptyList()).copy(store = store, isNsfw = true)
        show(ExtensionDetailsScreenModel.State(extension = extension, isIncognito = true))
        compose.onNodeWithContentDescription("Open source repo").performClick()
        listOf("Enable all", "Disable all", "Clear cookies").forEach {
            compose.onNodeWithContentDescription("More options").performClick()
            compose.onNodeWithText(it).performClick()
        }
        compose.onNodeWithText("18+").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Ext pkg.one").performClick()
        events shouldContainExactly listOf("open https://github.com/me/repo", "enable", "disable", "cookies")
    }

    @Test
    fun sourcesAndActions() {
        val extension = anInstalledExtension(isObsolete = true, isRedundant = true).copy(store = fixtureStore)
        show(ExtensionDetailsScreenModel.State(extension = extension, loadedSources = sources()))
        compose.onNodeWithContentDescription("Open source repo").performClick()
        compose.onNodeWithText("Uninstall").performClick()
        compose.onNodeWithText("App info").performClick()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Conf (JA)").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onAllNodesWithText("English")[1].performScrollTo().performClick()
        shadowOf(compose.activity).nextStartedActivity.data.toString() shouldBe "package:pkg.one"
        events shouldContainExactly listOf(
            "open https://store/index.min.json",
            "uninstall",
            "incognito true",
            "source 2",
            "prefs 2",
            "source 1",
        )
    }

    @Test
    fun unsharedWithoutAppInfo() {
        val extension = anInstalledExtension().copy(isShared = false)
        show(ExtensionDetailsScreenModel.State(extension = extension, loadedSources = emptyList()))
        compose.onNodeWithText("App info").assertDoesNotExist()
        compose.onNodeWithText("Ext pkg.one").performClick()
        compose.onNodeWithText("Version").assertExists()
    }

    @Test
    fun untrustedNameCopies() {
        val details = Extension.Untrusted(
            name = "U",
            pkgName = "eu.kanade.tachiyomi.extension.u",
            versionName = "1",
            versionCode = 1L,
            libVersion = 1.6,
            signatureHash = "h",
        )
        compose.setContent {
            MaterialTheme {
                DetailsHeader(
                    extension = details,
                    extIncognitoMode = false,
                    onClickAgeRating = {},
                    onClickUninstall = {},
                    onClickAppInfo = null,
                    onExtIncognitoChange = {},
                )
            }
        }
        compose.onNodeWithText("u").performClick()
        compose.onNodeWithText("App info").assertDoesNotExist()
    }
}
