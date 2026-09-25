package mihon.feature.upcoming

import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import mihon.domain.upcoming.interactor.GetUpcomingManga
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.model.Manga
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

internal class UpcomingScreenModelTest {
    private val getUpcomingManga = mockk<GetUpcomingManga>()
    private val upcoming = MutableStateFlow<List<Manga>>(emptyList())
    private val day1 = LocalDate.of(2030, 1, 5)
    private val day2 = LocalDate.of(2030, 1, 9)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { getUpcomingManga.subscribe() } returns upcoming
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun manga(id: Long, day: LocalDate, status: Int = SManga.ONGOING): Manga = Manga.create().copy(
        id = id,
        ogStatus = status.toLong(),
        nextUpdate = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    )

    private fun UpcomingScreenModel.settled(): UpcomingScreenModel.State = runBlocking {
        withTimeout(10_000L) { state.first { it.items.isNotEmpty() } }
    }

    @Test
    fun itemsAreGroupedByDay() {
        upcoming.value = listOf(
            manga(1L, day1),
            manga(2L, day1),
            manga(3L, day2),
            manga(4L, day2, status = SManga.COMPLETED),
        )
        val state = UpcomingScreenModel(getUpcomingManga).settled()
        state.items.map { if (it is UpcomingUIModel.Header) "h${it.mangaCount}" else "m" } shouldContainExactly
            listOf("h2", "m", "m", "h2", "m", "m")
        // A manga without a date counts toward the header above it.
        state.events shouldBe mapOf(day1 to 2, day2 to 2)
        state.headerIndexes shouldBe mapOf(day1 to 0, day2 to 3)
    }

    @Test
    fun anEmptyListHasNoHeaders() {
        val model = UpcomingScreenModel(getUpcomingManga)
        upcoming.value = listOf(manga(1L, day1, status = SManga.COMPLETED))
        val state = model.settled()
        state.items.size shouldBe 1
        state.events shouldBe emptyMap()
    }

    @Test
    fun theMonthCanChange() {
        val model = UpcomingScreenModel(getUpcomingManga)
        model.setSelectedYearMonth(YearMonth.of(2031, 2))
        model.state.value.selectedYearMonth shouldBe YearMonth.of(2031, 2)
    }

    @Test
    fun theInteractorComesFromInjekt() {
        startKoin { modules(module { single { getUpcomingManga } }) }
        upcoming.value = listOf(manga(1L, day1))
        UpcomingScreenModel().settled().items.size shouldBe 2
    }
}
