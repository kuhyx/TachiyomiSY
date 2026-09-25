package eu.kanade.tachiyomi.source.online.all

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class MangaDexPreferenceKeysTest {
    @Test
    fun keysAreSuffixedWithLanguage() {
        MangaDex.getDataSaverPreferenceKey("en") shouldBe "dataSaverV5_en"
        MangaDex.getStandardHttpsPreferenceKey("en") shouldBe "usePort443_en"
        MangaDex.getBlockedGroupsPrefKey("ja") shouldBe "blockedGroups_ja"
        MangaDex.getBlockedUploaderPrefKey("ja") shouldBe "blockedUploader_ja"
        MangaDex.getCoverQualityPrefKey("fr") shouldBe "thumbnailQuality_fr"
        MangaDex.getTryUsingFirstVolumeCoverKey("fr") shouldBe "tryUsingFirstVolumeCover_fr"
        MangaDex.getAltTitlesInDescKey("de") shouldBe "altTitlesInDesc_de"
        MangaDex.getFinalChapterInDescPrefKey("de") shouldBe "finalChapterInDesc_de"
        MangaDex.preferExtensionLangTitleKey("pt-BR") shouldBe "preferExtensionLangTitle_pt-BR"
    }
}
