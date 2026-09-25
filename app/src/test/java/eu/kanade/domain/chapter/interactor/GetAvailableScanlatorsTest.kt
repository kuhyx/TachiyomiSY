package eu.kanade.domain.chapter.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.repository.ChapterRepository

internal class GetAvailableScanlatorsTest {

    private val repository = mockk<ChapterRepository>()
    private val interactor = GetAvailableScanlators(repository)
    private val raw = listOf("Group", "", " ", "Group", "Other")

    @Test
    fun dropsBlanksAndDuplicates() = runTest {
        coEvery { repository.getScanlatorsByMangaId(4) } returns raw
        every { repository.getScanlatorsByMangaIdAsFlow(4) } returns flowOf(raw)
        interactor.await(4) shouldBe setOf("Group", "Other")
        interactor.subscribe(4).first() shouldBe setOf("Group", "Other")
    }

    @Test
    fun mergedLookupsUseTheMergeId() = runTest {
        coEvery { repository.getScanlatorsByMergeId(4) } returns raw
        every { repository.getScanlatorsByMergeIdAsFlow(4) } returns flowOf(raw)
        interactor.awaitMerge(4) shouldBe setOf("Group", "Other")
        interactor.subscribeMerge(4).first() shouldBe setOf("Group", "Other")
    }
}
