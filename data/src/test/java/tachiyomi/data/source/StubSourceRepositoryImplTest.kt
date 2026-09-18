package tachiyomi.data.source

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase

internal class StubSourceRepositoryImplTest {
    private val repository = StubSourceRepositoryImpl(inMemoryDatabase())

    @Test
    fun getStubSourceMissingIsNull() = runTest {
        repository.getStubSource(1L) shouldBe null
    }

    @Test
    fun upsertInsertsThenReads() = runTest {
        repository.upsertStubSource(id = 1L, lang = "en", name = "One")

        val stub = repository.getStubSource(1L)

        stub?.id shouldBe 1L
        stub?.lang shouldBe "en"
        stub?.name shouldBe "One"
    }

    @Test
    fun upsertUpdatesExistingRow() = runTest {
        repository.upsertStubSource(id = 1L, lang = "en", name = "One")

        repository.upsertStubSource(id = 1L, lang = "fr", name = "Un")

        repository.subscribeAll().first().map { it.lang to it.name } shouldBe listOf("fr" to "Un")
    }

    @Test
    fun subscribeAllEmitsEveryRow() = runTest {
        repository.subscribeAll().first().shouldBeEmpty()
        repository.upsertStubSource(id = 1L, lang = "en", name = "One")
        repository.upsertStubSource(id = 2L, lang = "ja", name = "Two")

        repository.subscribeAll().first().map { it.toString() } shouldBe listOf("One (EN)", "Two (JA)")
    }
}
