package mihon.domain.extension.interactor

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.domain.extension.repository.ExtensionStoreRepository
import org.junit.jupiter.api.Test

internal class RemoveExtensionStoreTest {

    private val repository = mockk<ExtensionStoreRepository>()

    @Test
    fun removesByIndexUrl() = runTest {
        coJustRun { repository.remove("https://a.test/index.min.json") }

        RemoveExtensionStore(repository)("https://a.test/index.min.json")

        coVerify(exactly = 1) { repository.remove("https://a.test/index.min.json") }
    }
}
