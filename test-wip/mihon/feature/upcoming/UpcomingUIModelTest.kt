package mihon.feature.upcoming

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import java.time.LocalDate

internal class UpcomingUIModelTest {
    private val date = LocalDate.of(2024, 5, 4)
    private val manga = Manga.create().copy(id = 9L, ogTitle = "Upcoming")

    @Test
    fun headerHoldsDateAndCount() {
        val header = UpcomingUIModel.Header(date = date, mangaCount = 2)
        header.date shouldBe date
        header.mangaCount shouldBe 2
        header.copy(mangaCount = 3).mangaCount shouldBe 3
        (header == UpcomingUIModel.Header(date = date, mangaCount = 2)) shouldBe true
        header.hashCode() shouldBe UpcomingUIModel.Header(date = date, mangaCount = 2).hashCode()
        header.toString().contains("Header") shouldBe true
    }

    @Test
    fun itemHoldsTheManga() {
        val item = UpcomingUIModel.Item(manga)
        item.manga.id shouldBe 9L
        item.copy(manga = manga.copy(id = 10L)).manga.id shouldBe 10L
        (item == UpcomingUIModel.Item(manga)) shouldBe true
        item.hashCode() shouldBe UpcomingUIModel.Item(manga).hashCode()
        item.toString().contains("Item") shouldBe true
    }
}
