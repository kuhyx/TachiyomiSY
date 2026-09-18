package tachiyomi.domain.chapter.repository

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.reflect.KClass

/**
 * The interfaces' only code is the `applyScanlatorFilter = false` defaults, which every
 * interactor bypasses by passing the flag: reached here by omitting it on a mock. The
 * compatibility `DefaultImpls` bridges exist for Java callers only, so they are driven directly.
 */
internal class ChapterRepositoryDefaultsTest {

    private val chapter = Chapter.create().copy(id = 1L, mangaId = 7L)
    private val repository = mockk<ChapterRepository>()

    @Test
    fun chaptersByMangaIdUnfiltered() = runTest {
        coEvery { repository.getChapterByMangaId(7L, false) } returns listOf(chapter)

        repository.getChapterByMangaId(7L) shouldBe listOf(chapter)
    }

    @Test
    fun chapterFlowUnfiltered() = runTest {
        coEvery { repository.getChapterByMangaIdAsFlow(7L, false) } returns flowOf(listOf(chapter))

        repository.getChapterByMangaIdAsFlow(7L).first() shouldBe listOf(chapter)
    }

    @Test
    fun mergedChaptersUnfiltered() = runTest {
        coEvery { repository.getMergedChapterByMangaId(7L, false) } returns listOf(chapter)

        repository.getMergedChapterByMangaId(7L) shouldBe listOf(chapter)
    }

    @Test
    fun mergedChapterFlowUnfiltered() = runTest {
        coEvery { repository.getMergedChapterByMangaIdFlow(7L, false) } returns flowOf(listOf(chapter))

        repository.getMergedChapterByMangaIdFlow(7L).first() shouldBe listOf(chapter)
    }

    @Test
    fun queryBridgesForJava() = runTest {
        coEvery { repository.getChapterByMangaId(7L, false) } returns listOf(chapter)
        coEvery { repository.getChapterByMangaIdAsFlow(7L, false) } returns flowOf(listOf(chapter))

        val owner = ChapterQueryRepository::class
        bridge<List<Chapter>>(owner, "getChapterByMangaId") shouldBe listOf(chapter)
        bridge<Flow<List<Chapter>>>(owner, "getChapterByMangaIdAsFlow").first() shouldBe listOf(chapter)
    }

    @Test
    fun mergedBridgesForJava() = runTest {
        coEvery { repository.getMergedChapterByMangaId(7L, false) } returns listOf(chapter)
        coEvery { repository.getMergedChapterByMangaIdFlow(7L, false) } returns flowOf(listOf(chapter))

        val owner = ChapterMergedRepository::class
        bridge<List<Chapter>>(owner, "getMergedChapterByMangaId") shouldBe listOf(chapter)
        bridge<Flow<List<Chapter>>>(owner, "getMergedChapterByMangaIdFlow").first() shouldBe listOf(chapter)
    }

    // Calls `<owner>$DefaultImpls.<name>$default(repository, 7, false, continuation, mask = 2, null)`,
    // the static bridge the compiler emits for Java callers that leave `applyScanlatorFilter` out.
    private suspend fun <T> bridge(owner: KClass<*>, name: String): T = suspendCoroutineUninterceptedOrReturn {
        val parameterTypes = arrayOf(
            owner.java,
            Long::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            Continuation::class.java,
            Int::class.javaPrimitiveType,
            Any::class.java,
        )
        Class.forName("${owner.java.name}\$DefaultImpls")
            .getMethod("$name\$default", *parameterTypes)
            .invoke(null, repository, 7L, false, it, 2, null)
    }
}
