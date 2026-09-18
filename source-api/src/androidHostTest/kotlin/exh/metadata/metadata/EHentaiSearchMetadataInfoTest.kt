package exh.metadata.metadata

import dev.icerock.moko.resources.StringResource
import exh.metadata.MetadataUtil
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

internal class EHentaiSearchMetadataInfoTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun label(resource: StringResource): String = LABEL_PREFIX + resource.resourceId

    private fun formatted(epochMillis: Long): String = MetadataUtil.EX_DATE_FORMAT
        .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()))

    @Test
    fun onlyNonNullFieldsWithDefaults() {
        EHentaiSearchMetadata().getExtraInfoPairs(stubbedContext()) shouldContainExactly listOf(
            label(SYMR.strings.aged) to "false",
            label(SYMR.strings.last_update_check) to formatted(0),
        )
    }

    @Test
    fun everyFieldIsListedWhenSet() {
        val metadata = EHentaiSearchMetadata().apply {
            gId = "123"
            gToken = "tok"
            exh = true
            thumbnailUrl = "https://t"
            title = "Romaji"
            altTitle = "日本語"
            genre = "Doujinshi"
            datePosted = 1_700_000_000_000
            parent = "/g/1/a/"
            visible = "Yes"
            language = "English"
            translated = false
            size = 1_500_000
            length = 20
            favorites = 3
            ratingCount = 4
            averageRating = 4.5
            aged = true
            lastUpdateCheck = 1_600_000_000_000
        }
        metadata.getExtraInfoPairs(stubbedContext()) shouldContainExactly listOf(
            label(SYMR.strings.id) to "123",
            label(SYMR.strings.token) to "tok",
            label(SYMR.strings.is_exhentai_gallery) to "true",
            label(SYMR.strings.thumbnail_url) to "https://t",
            label(MR.strings.title) to "Romaji",
            label(SYMR.strings.alt_title) to "日本語",
            label(SYMR.strings.genre) to "Doujinshi",
            label(SYMR.strings.date_posted) to formatted(1_700_000_000_000),
            label(SYMR.strings.parent) to "/g/1/a/",
            label(SYMR.strings.visible) to "Yes",
            label(SYMR.strings.language) to "English",
            label(SYMR.strings.translated) to "false",
            label(SYMR.strings.gallery_size) to "1.5 MB",
            label(SYMR.strings.page_count) to "20",
            label(SYMR.strings.total_favorites) to "3",
            label(SYMR.strings.total_ratings) to "4",
            label(SYMR.strings.average_rating) to "4.5",
            label(SYMR.strings.aged) to "true",
            label(SYMR.strings.last_update_check) to formatted(1_600_000_000_000),
        )
    }

    @Test
    fun labelsResolveThroughResources() {
        val pairs = EHentaiSearchMetadata().apply { gId = "1" }.getExtraInfoPairs(stubbedContext())
        pairs.first().first shouldBe LABEL_PREFIX + SYMR.strings.id.resourceId
        pairs.size shouldBe 3
    }
}
