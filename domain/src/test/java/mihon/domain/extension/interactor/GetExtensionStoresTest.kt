package mihon.domain.extension.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.domain.extension.model.extensionStore
import mihon.domain.extension.repository.ExtensionStoreRepository
import org.junit.jupiter.api.Test

internal class GetExtensionStoresTest {

    private val stores = listOf(extensionStore())
    private val repository = mockk<ExtensionStoreRepository>()
    private val getStores = GetExtensionStores(repository)

    @Test
    fun getReturnsEveryStore() = runTest {
        coEvery { repository.getAll() } returns stores

        getStores.get() shouldBe stores
    }

    @Test
    fun subscribeEmitsEveryStore() = runTest {
        every { repository.getAllAsFlow() } returns flowOf(stores)

        getStores.subscribe().first() shouldBe stores
    }
}
