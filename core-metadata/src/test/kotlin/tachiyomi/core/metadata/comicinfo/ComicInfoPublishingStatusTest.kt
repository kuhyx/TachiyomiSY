package tachiyomi.core.metadata.comicinfo

import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ComicInfoPublishingStatusTest {
    @Test
    fun knownStatusMapsBothWays() {
        ComicInfoPublishingStatus.toComicInfoValue(SManga.COMPLETED.toLong()) shouldBe "Completed"
        ComicInfoPublishingStatus.toSMangaValue("On hiatus") shouldBe SManga.ON_HIATUS
        ComicInfoPublishingStatus.entries.forEach { status ->
            ComicInfoPublishingStatus.toComicInfoValue(status.sMangaModelValue.toLong()) shouldBe status.comicInfoValue
            ComicInfoPublishingStatus.toSMangaValue(status.comicInfoValue) shouldBe status.sMangaModelValue
        }
    }

    @Test
    fun unknownStatusFallsBack() {
        ComicInfoPublishingStatus.toComicInfoValue(-1L) shouldBe "Unknown"
        ComicInfoPublishingStatus.toSMangaValue(null) shouldBe SManga.UNKNOWN
        ComicInfoPublishingStatus.toSMangaValue("Nope") shouldBe SManga.UNKNOWN
    }
}
