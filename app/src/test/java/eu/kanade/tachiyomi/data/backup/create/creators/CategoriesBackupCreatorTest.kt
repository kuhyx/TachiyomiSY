package eu.kanade.tachiyomi.data.backup.create.creators

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.category.interactor.GetCategories

internal class CategoriesBackupCreatorTest {

    private val getCategories = mockk<GetCategories>()

    @BeforeEach
    fun setUp() {
        startKoin { modules(module { single { getCategories } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun skipsSystemCategory() = runTest {
        coEvery { getCategories.await() } returns listOf(testCategory(id = 0L), testCategory(id = 2L))
        val creator = CategoriesBackupCreator(getCategories = getCategories)
        creator().map { it.name } shouldContainExactly listOf("C2")
    }

    @Test
    fun injectsGetCategoriesByDefault() = runTest {
        coEvery { getCategories.await() } returns listOf(testCategory(id = 5L, name = "Reading"))
        CategoriesBackupCreator()().single().name shouldBe "Reading"
    }

    @Test
    fun emptyWhenNoCategories() = runTest {
        coEvery { getCategories.await() } returns emptyList()
        CategoriesBackupCreator(getCategories = getCategories)() shouldBe emptyList()
    }
}
