package mihon.domain.extension.interactor

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.domain.extension.repository.ExtensionStoreRepository
import org.junit.jupiter.api.Test

internal class GetExtensionStoreCountAsFlowTest {

    private val repository = mockk<ExtensionStoreRepository>()

    @Test
    fun emitsTheRepositoryCount() = runTest {
        every { repository.getCountAsFlow() } returns flowOf(3L)

        GetExtensionStoreCountAsFlow(repository)().first() shouldBe 3L
    }
}
