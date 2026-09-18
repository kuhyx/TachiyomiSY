package mihon.domain.extension.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.domain.extension.repository.ExtensionStoreRepository
import org.junit.jupiter.api.Test
import java.io.IOException

internal class AddExtensionStoreTest {

    private val repository = mockk<ExtensionStoreRepository>()
    private val addStore = AddExtensionStore(repository)

    @Test
    fun returnsTheRepositoryResult() = runTest {
        coEvery { repository.insert("https://a.test/index.min.json") } returns Result.success(Unit)

        addStore("https://a.test/index.min.json") shouldBe Result.success(Unit)
    }

    @Test
    fun failureIsPassedThrough() = runTest {
        val failure = Result.failure<Unit>(IOException("offline"))
        coEvery { repository.insert("https://a.test/index.min.json") } returns failure

        addStore("https://a.test/index.min.json") shouldBe failure
    }
}
