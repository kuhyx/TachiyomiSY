package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import exh.eh.EHentaiUpdaterStats
import exh.metadata.metadata.EHentaiSearchMetadata
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.mockk.coEvery
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@RunWith(RobolectricTestRunner::class)
internal class UpdaterStatisticsTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val eh = EhScreenKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        koin.start(eh.module())
        coEvery { eh.getExhFavorites.await() } returns emptyList()
    }

    @After
    fun tearDown() {
        koin.stop()
    }

    private fun manga(id: Long): Manga = Manga.create().copy(id = id)

    private fun meta(id: Long, checkedAgo: Long) = EHentaiSearchMetadata().apply {
        mangaId = id
        lastUpdateCheck = System.currentTimeMillis() - checkedAgo
    }.flatten()

    private fun stats(agoMillis: Long) {
        val stats = EHentaiUpdaterStats(System.currentTimeMillis() - agoMillis, possibleUpdates = 9, updateCount = 4)
        koin.exh.exhAutoUpdateStats.set(Json.encodeToString(EHentaiUpdaterStats.serializer(), stats))
    }

    // The dialog body is the one text that joins the run summary and the counts with a blank line.
    private fun open(): String {
        harness.show(SettingsEhScreen)
        harness.click("Show updater statistics")
        compose.awaitMain(timeoutMillis = 10_000) { harness.count("Gallery updater statistics") == 1 }
        return compose.onAllNodes(hasText("", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }
            .map { it.text }
            .firstOrNull { "\n\n" in it }
            .orEmpty()
    }

    @Test
    fun notRanYet() {
        open() shouldStartWith "The updater has not ran yet."
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        harness.count("Gallery updater statistics") shouldBe 0
    }

    @Test
    fun ranHoursAgoCountsRecent() {
        stats(agoMillis = 2.hours.inWholeMilliseconds + 1_000)
        coEvery { eh.getExhFavorites.await() } returns listOf(manga(1), manga(2), manga(3))
        coEvery { eh.getFlatMetadata.await(1) } returns meta(id = 1, checkedAgo = 1_000)
        coEvery { eh.getFlatMetadata.await(2) } returns meta(id = 2, checkedAgo = 400.days.inWholeMilliseconds)
        coEvery { eh.getFlatMetadata.await(3) } returns null
        val text = open()
        text shouldContain "last ran 2 hours ago"
        text shouldContain "checked 4 out of the 9"
    }

    @Test
    fun ranMomentsAgo() {
        stats(agoMillis = 0)
        open() shouldContain "moments ago"
    }

    @Test
    fun ranYearsAgo() {
        stats(agoMillis = 800.days.inWholeMilliseconds)
        open() shouldContain "2 years ago"
    }

    @Test
    fun failureShowsEmptyDialog() {
        coEvery { eh.getExhFavorites.await() } throws IllegalStateException("db")
        open() shouldBe ""
        harness.count("Gallery updater statistics") shouldBe 1
    }

    @Test
    fun loadingDialogFirst() {
        val gate = CompletableDeferred<List<Manga>>()
        coEvery { eh.getExhFavorites.await() } coAnswers { gate.await() }
        harness.show(SettingsEhScreen)
        harness.click("Show updater statistics")
        harness.count("Collecting statistics…") shouldBe 1
        gate.complete(emptyList())
        compose.awaitMain(timeoutMillis = 10_000) { harness.count("Gallery updater statistics") == 1 }
    }
}
