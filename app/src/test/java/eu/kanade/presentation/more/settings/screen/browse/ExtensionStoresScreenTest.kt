package eu.kanade.presentation.more.settings.screen.browse

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.presentation.more.settings.screen.awaitMain
import eu.kanade.tachiyomi.extension.ExtensionManager
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableSharedFlow
import mihon.domain.extension.interactor.AddExtensionStore
import mihon.domain.extension.interactor.GetExtensionStores
import mihon.domain.extension.interactor.RemoveExtensionStore
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.model.ExtensionStore
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExtensionStoresScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val stores = MutableSharedFlow<List<ExtensionStore>>(replay = 1)
    private val remove = mockk<RemoveExtensionStore>().also { coEvery { it(any()) } just runs }
    private val update = mockk<UpdateExtensionStores>().also { coEvery { it() } just runs }

    @Before
    fun setUp() {
        val get = mockk<GetExtensionStores> { every { subscribe() } returns stores }
        val add = mockk<AddExtensionStore>().also {
            coEvery { it(any()) } returns Result.failure(IllegalStateException())
        }
        stopKoin()
        startKoin {
            modules(
                module {
                    single { get }
                    single { add }
                    single { remove }
                    single { update }
                    single { mockk<ExtensionManager>(relaxed = true) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun show(url: String? = null) {
        compose.setContent { MaterialTheme { Navigator(ExtensionStoresScreen(url)) } }
        compose.waitForIdle()
    }

    private fun count(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun loadingThenEmpty() {
        show()
        count("Extension stores") shouldBe 0
        stores.tryEmit(emptyList())
        compose.awaitMain(timeoutMillis = 5_000) { count("You haven't added an extension store yet") == 1 }
        compose.onNodeWithContentDescription("Refresh").performClick()
        coVerify(timeout = 5_000) { update() }
    }

    @Test
    fun storeActions() {
        stores.tryEmit(listOf(store("a", discord = "https://discord.example")))
        show()
        compose.awaitMain(timeoutMillis = 5_000) { count("Store a") == 1 }
        compose.onNodeWithContentDescription("Open in browser").performClick()
        compose.onNodeWithContentDescription("Copy to clipboard").performClick()
        compose.onNodeWithContentDescription("Discord").performClick()
        compose.onNodeWithContentDescription("Delete").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("OK").performClick()
        coVerify(timeout = 5_000) { remove("a") }
    }

    @Test
    fun createDialogOpens() {
        stores.tryEmit(listOf(store("a")))
        show()
        compose.awaitMain(timeoutMillis = 5_000) { count("Store a") == 1 }
        compose.onNodeWithText("Add").performClick()
        compose.waitForIdle()
        count("Add extension store") shouldBe 1
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        count("Add extension store") shouldBe 0
    }

    @Test
    fun deeplinkAsksToConfirm() {
        stores.tryEmit(emptyList())
        show(url = "https://deep.example")
        compose.awaitMain(timeoutMillis = 5_000) { count("Do you wish to add the extension store below?") == 1 }
        compose.onAllNodesWithText("Add").fetchSemanticsNodes().size shouldBe 2
    }
}
