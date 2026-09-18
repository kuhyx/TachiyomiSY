package mihon.domain.extension.interactor

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.domain.extension.repository.ExtensionStoreRepository
import org.junit.jupiter.api.Test

internal class UpdateExtensionStoresTest {

    private val repository = mockk<ExtensionStoreRepository>()

    @Test
    fun refreshesEveryStore() = runTest {
        coJustRun { repository.refreshAll() }

        UpdateExtensionStores(repository)()

        coVerify(exactly = 1) { repository.refreshAll() }
    }
}
