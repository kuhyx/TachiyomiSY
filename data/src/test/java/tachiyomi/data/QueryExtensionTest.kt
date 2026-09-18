package tachiyomi.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

internal class QueryExtensionTest {
    private val database = inMemoryDatabase()
    private val categories = database.categoriesQueries
    private val duration = database.historyQueries.getReadDuration()

    @Test
    fun subscribeToListEmitsRows() = runTest {
        categories.getCategories().subscribeToList().first().map { it.name } shouldBe listOf("")
        categories.getCategories().subscribeToList(EmptyCoroutineContext).first().size shouldBe 1
    }

    @Test
    fun subscribeToListMapsRows() = runTest {
        categories.getCategories().subscribeToList { it.name }.first() shouldBe listOf("")
    }

    @Test
    fun subscribeToOneEmitsRow() = runTest {
        duration.subscribeToOne().first() shouldBe 0L
        duration.subscribeToOne(EmptyCoroutineContext).first() shouldBe 0L
        duration.subscribeToOne { it + 1L }.first() shouldBe 1L
    }

    @Test
    fun subscribeToOneThrowsOnNone() = runTest {
        shouldThrow<NullPointerException> { categories.getCategory(99L).subscribeToOne().first() }
    }

    @Test
    fun subscribeToOneOrNullRow() = runTest {
        categories.getCategory(0L).subscribeToOneOrNull().first()?.name shouldBe ""
        categories.getCategory(99L).subscribeToOneOrNull(EmptyCoroutineContext).first() shouldBe null
    }

    @Test
    fun subscribeToOneOrNullMapped() = runTest {
        categories.getCategory(0L).subscribeToOneOrNull { it.name }.first() shouldBe ""
        categories.getCategory(99L).subscribeToOneOrNull { it.name }.first() shouldBe null
    }

    @Test
    fun awaitListMapsRows() = runTest {
        categories.getCategories().awaitList { it.name } shouldBe listOf("")
    }

    @Test
    fun awaitOneMapsRow() = runTest {
        duration.awaitOne { it + 2L } shouldBe 2L
        shouldThrow<NullPointerException> { categories.getCategory(99L).awaitOne { it.name } }
    }

    @Test
    fun awaitOneOrNullMapsRow() = runTest {
        categories.getCategory(0L).awaitOneOrNull { it.name } shouldBe ""
        categories.getCategory(99L).awaitOneOrNull { it.name } shouldBe null
    }
}
