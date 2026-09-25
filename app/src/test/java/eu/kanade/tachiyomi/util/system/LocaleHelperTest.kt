package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.ui.browse.source.SourcesScreenModel
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
internal class LocaleHelperTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun displayNamesNormaliseChinese() {
        LocaleHelper.getDisplayName("zh-CN") shouldBe Locale.forLanguageTag("zh-Hans").displayName
        LocaleHelper.getDisplayName("zh-TW") shouldBe Locale.forLanguageTag("zh-Hant").displayName
        LocaleHelper.getDisplayName("fr") shouldBe Locale.forLanguageTag("fr").displayName
    }

    @Test
    fun shortDisplayNamesAreLowerCased() {
        LocaleHelper.getShortDisplayName(null) shouldBe ""
        LocaleHelper.getShortDisplayName("es-419") shouldBe "es-la"
        LocaleHelper.getShortDisplayName("zh-CN") shouldBe "zh-hans"
        LocaleHelper.getShortDisplayName("zh-TW") shouldBe "zh-hant"
        LocaleHelper.getShortDisplayName("de") shouldBe "de"
        LocaleHelper.getShortDisplayName("de", uppercase = true) shouldBe "DE"
    }

    @Test
    fun localizedDisplayNamesAre() {
        LocaleHelper.getLocalizedDisplayName(null) shouldBe ""
        LocaleHelper.getLocalizedDisplayName("") shouldBe
            LocaleHelper.getLocalizedDisplayName(Locale.getDefault().toLanguageTag())
        LocaleHelper.getLocalizedDisplayName("fr") shouldBe "Français"
        LocaleHelper.getLocalizedDisplayName("zh-CN").isNotEmpty() shouldBe true
        LocaleHelper.getLocalizedDisplayName("zh-TW").isNotEmpty() shouldBe true
        LocaleHelper.getLocalizedDisplayName("!!not a tag") shouldBe ""
    }

    @Test
    fun sourceDisplayNamesCoverEvery() {
        LocaleHelper.getSourceDisplayName("custom|My sources", context) shouldBe "My sources"
        LocaleHelper.getSourceDisplayName(SourcesScreenModel.LAST_USED_KEY, context) shouldBe "Last used"
        LocaleHelper.getSourceDisplayName(SourcesScreenModel.PINNED_KEY, context) shouldBe "Pinned"
        LocaleHelper.getSourceDisplayName("other", context) shouldBe "Other"
        LocaleHelper.getSourceDisplayName("all", context) shouldBe "Multi"
        LocaleHelper.getSourceDisplayName("fr", context) shouldBe "Français"
        LocaleHelper.getSourceDisplayName(null, context) shouldBe ""
    }

    @Test
    fun theComparatorKeepsMultiFirst() {
        listOf("fr", "all", "de").sortedWith(LocaleHelper.comparator) shouldBe listOf("all", "de", "fr")
        LocaleHelper.comparator("all", "all") shouldBe -1
        LocaleHelper.comparator("de", "all") shouldBe 1
    }

    @Test
    fun defaultLanguagesAlwaysHoldAll() {
        LocaleHelper.getDefaultEnabledLanguages() shouldBe setOf("all", "en", Locale.getDefault().language)
    }
}
