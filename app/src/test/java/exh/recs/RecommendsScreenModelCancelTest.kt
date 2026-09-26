package exh.recs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import cafe.adriel.voyager.core.model.ScreenModelStore
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.readMember
import exh.recs.sources.rankedResults
import exh.recs.sources.sourceManga
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

private const val WAIT_MS = 10_000L

/** A recommendation whose own coroutine is cancelled while it resolves its titles keeps its Loading state. */
@RunWith(RobolectricTestRunner::class)
internal class RecommendsScreenModelCancelTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = SourceTestHarness()
    private val getManga = mockk<GetManga>()
    private val networkToLocal = mockk<NetworkToLocalManga>()
    private val resolved = CompletableDeferred<Unit>()

    @Before
    fun setUp() {
        harness.install()
        harness.serve(getManga)
        harness.serve(networkToLocal)
        harness.serve<SourceManager>(mockk())
    }

    @After
    fun tearDown() {
        harness.uninstall()
        // Voyager caches a screen model's IO scope globally; see RecommendsScreenModelTest.
        val dependencies = checkNotNull(ScreenModelStore.readMember(ScreenModelStore::class, "dependencies"))
        val remove = dependencies::class.java.methods.first { it.name == "remove" && it.parameterCount == 1 }
        (dependencies as Map<*, *>).keys.filter { "IoCoroutineScope" in it.toString() }.forEach { key ->
            remove.invoke(dependencies, key)
        }
    }

    private fun model(): RecommendsScreenModel =
        RecommendsScreenModel(RecommendsScreen.Args.MergedSourceMangas(listOf(rankedResults(1))))

    private fun stillLoading(model: RecommendsScreenModel): Boolean = runBlocking {
        withTimeout(WAIT_MS) { resolved.await() }
        model.state.value.items.values.single() is RecommendationItemResult.Loading
    }

    @Test
    fun cancelledSuccessIsDropped() {
        coEvery { networkToLocal(any<Manga>()) } coAnswers {
            currentCoroutineContext().job.cancel()
            resolved.complete(Unit)
            firstArg<Manga>()
        }
        stillLoading(model()) shouldBe true
    }

    @Test
    fun cancelledFailureIsDropped() {
        coEvery { networkToLocal(any<Manga>()) } coAnswers {
            currentCoroutineContext().job.cancel()
            resolved.complete(Unit)
            error("resolution failed after cancellation")
        }
        stillLoading(model()) shouldBe true
    }

    @Test
    fun getMangaFollowsTheDatabase() {
        val initial = sourceManga(id = 4L, title = "Before")
        val other = sourceManga(id = 5L, title = "Other").copy(url = "/other")
        // Finite, so the producer runs to its end: a null row is skipped, the stored row shown.
        every { getManga.subscribe(initial.url, initial.source) } returns flowOf(null, initial.copy(ogTitle = "After"))
        every { getManga.subscribe(other.url, other.source) } returns flowOf(other)
        coEvery { networkToLocal(any<Manga>()) } answers { firstArg<Manga>() }
        val model = model()
        var param by mutableStateOf(initial)
        var tick by mutableIntStateOf(0)
        var shown: Manga? = null
        var forwarded: Manga? = null
        compose.setContent {
            tick.hashCode()
            shown = model.getManga(param).value
            // Called from a composable that forwards its own parameter, getManga sees it as same/different.
            Forwarding(model, param, tick) { forwarded = it }
        }
        compose.waitForIdle()
        shown?.title shouldBe "After"
        tick++
        compose.waitForIdle()
        shown?.title shouldBe "After"
        // produceState has no keys: a new argument recomposes but keeps the running producer.
        param = other
        compose.waitForIdle()
        shown?.title shouldBe "After"
        forwarded?.title shouldBe "After"
    }
}

@Composable
private fun Forwarding(model: RecommendsScreenModel, manga: Manga, tick: Int, onShown: (Manga) -> Unit) {
    tick.hashCode()
    onShown(model.getManga(manga).value)
}
