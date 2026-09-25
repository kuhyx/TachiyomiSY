package eu.kanade.presentation.more.settings.screen.browse

import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.findAvailableExtensions
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mihon.domain.extension.interactor.AddExtensionStore
import mihon.domain.extension.interactor.GetExtensionStores
import mihon.domain.extension.interactor.RemoveExtensionStore
import mihon.domain.extension.interactor.UpdateExtensionStores
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ExtensionStoresScreenModelTest {
    private val stores = MutableSharedFlow<List<ExtensionStore>>(replay = 1)
    private val add = mockk<AddExtensionStore>()
    private val remove = mockk<RemoveExtensionStore>().also { coEvery { it(any()) } just runs }
    private val update = mockk<UpdateExtensionStores>().also { coEvery { it() } just runs }
    private val extensions = mockk<ExtensionManager>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic("eu.kanade.tachiyomi.extension.ExtensionManagerAvailableKt")
        coEvery { extensions.findAvailableExtensions() } just runs
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun model() = ExtensionStoresScreenModel(
        getExtensionStores = mockk<GetExtensionStores> { every { subscribe() } returns stores },
        addExtensionStore = add,
        removeExtensionStore = remove,
        updateExtensionStores = update,
        extensionManager = extensions,
    )

    private fun success(model: ExtensionStoresScreenModel): ExtensionStoreScreenState.Success {
        val deadline = System.currentTimeMillis() + 5_000
        while (model.state.value !is ExtensionStoreScreenState.Success && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
        return model.state.value as ExtensionStoreScreenState.Success
    }

    private fun eventually(check: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!check() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10)
        }
        check() shouldBe true
    }

    @Test
    fun loadingIgnoresDialogs() {
        val model = model()
        model.showDialog(ExtensionStoreDialog.Create())
        model.addFromDeeplink("u")
        model.dismissDialog()
        model.refreshRepos()
        model.state.value shouldBe ExtensionStoreScreenState.Loading
    }

    @Test
    fun storesArriveAndRefresh() {
        val model = model()
        stores.tryEmit(emptyList())
        success(model).isEmpty shouldBe true
        stores.tryEmit(listOf(store("a")))
        eventually { success(model).stores.size == 1 }
        model.refreshRepos()
        coVerify(timeout = 5_000) { update() }
    }

    @Test
    fun createFromDialogSucceeds() {
        coEvery { add("new") } returns Result.success(Unit)
        val model = model()
        stores.tryEmit(listOf(store("a")))
        success(model)
        model.showDialog(ExtensionStoreDialog.Create())
        model.createRepo("new")
        eventually { success(model).dialog == null }
        coVerify { extensions.findAvailableExtensions() }
    }

    @Test
    fun createFailuresKeepDialog() {
        coEvery { add(any()) } returns Result.failure(IllegalStateException("bad")) andThen
            Result.failure(IllegalStateException())
        val model = model()
        stores.tryEmit(listOf(store("a")))
        success(model)
        model.showDialog(ExtensionStoreDialog.Create())
        model.createRepo("x")
        eventually { success(model).dialog == ExtensionStoreDialog.Create(errorMessage = "bad") }
        model.addFromDeeplink("a")
        success(model).dialog shouldBe ExtensionStoreDialog.Confirm(url = "a", alreadyExists = true)
        model.createRepo("a")
        eventually { success(model).dialog == ExtensionStoreDialog.Confirm("a", true, errorMessage = "unknown error") }
    }

    @Test
    fun otherDialogsUntouched() {
        coEvery { add(any()) } returns Result.failure(IllegalStateException("bad"))
        val model = model()
        stores.tryEmit(listOf(store("a")))
        success(model)
        val delete = ExtensionStoreDialog.Delete(store("a"))
        model.showDialog(delete)
        model.createRepo("x")
        eventually { success(model).dialog == delete }
        model.dismissDialog()
        model.createRepo("y")
        model.deleteRepo("a")
        coVerify(timeout = 5_000) { remove("a") }
        success(model).dialog shouldBe null
    }
}
