package eu.kanade.tachiyomi.source.online.all

import android.net.Uri
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import exh.util.UriFilter
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class EHentaiFiltersTest {
    private val harness = SourceTestHarness()

    @Before
    fun setUp() = harness.install()

    @After
    fun tearDown() = harness.uninstall()

    private fun uriOf(vararg filters: UriFilter): String =
        Uri.parse("https://e-hentai.org").buildUpon().apply { filters.forEach { it.addToUri(this) } }.build().toString()

    @Test
    fun watchedAppendsPath() {
        uriOf(Watched(isEnabled = true)) shouldBe "https://e-hentai.org/watched"
        uriOf(Watched(isEnabled = false)) shouldBe "https://e-hentai.org"
        Watched(isEnabled = true).isEnabled shouldBe true
    }

    @Test
    fun toplistOptions() {
        ToplistOption.entries.map { it.toString() } shouldContainExactly
            listOf("None", "All time", "Past year", "Past month", "Yesterday")
        ToplistOption.PAST_MONTH.index shouldBe 13
        ToplistOptions().values.size shouldBe 5
    }

    @Test
    fun genreGroupBits() {
        val group = GenreGroup()
        uriOf(group) shouldBe "https://e-hentai.org?f_cats=1023"
        group.state[0].state = true
        group.state[9].state = true
        uriOf(group) shouldBe "https://e-hentai.org?f_cats=1020"
        group.state[0].genreId shouldBe 2
    }

    @Test
    fun advancedOptions() {
        val off = AdvancedOption("Off", "f_off")
        off.state shouldBe false
        uriOf(off) shouldBe "https://e-hentai.org"
        val on = AdvancedOption("On", "f_on", defValue = true)
        uriOf(on) shouldBe "https://e-hentai.org?f_on=on"
        on.param shouldBe "f_on"
    }

    @Test
    fun pageOptionsShareSearchFlag() {
        val min = MinPagesOption()
        val max = MaxPagesOption()
        uriOf(min, max) shouldBe "https://e-hentai.org"
        min.state = " 5 "
        uriOf(min) shouldBe "https://e-hentai.org?f_sp=on&f_spf=5"
        max.state = "9"
        uriOf(min, max) shouldBe "https://e-hentai.org?f_sp=on&f_spf=5&f_spt=9"
    }

    @Test
    fun ratingOption() {
        val rating = RatingOption()
        uriOf(rating) shouldBe "https://e-hentai.org"
        rating.state = 2
        uriOf(rating) shouldBe "https://e-hentai.org?f_srdd=3&f_sr=on"
        rating.values.size shouldBe 5
    }

    @Test
    fun advancedGroupDelegates() {
        val group = AdvancedGroup()
        group.state.size shouldBe 8
        (group.state[0] as AdvancedOption).state = true
        (group.state[2] as RatingOption).state = 1
        uriOf(group) shouldBe "https://e-hentai.org?f_sh=on&f_srdd=2&f_sr=on"
    }

    @Test
    fun queryCombinesTags() {
        val tags = AutoCompleteTags()
        tags.values.isNotEmpty() shouldBe true
        tags.skipAutoFillTags.all { it.endsWith(":") } shouldBe true
        tags.state = listOf(
            " female:tag ",
            "-male:x",
            "~parody:some series",
            "solo",
            "-tank",
            "~loose",
            "::",
            " ",
            "a:b:c",
        )
        // A bare prefixed tag keeps its prefix in the item, so the marker doubles (current behaviour).
        EHentaiQuery.combine(FilterList(tags, ReverseFilter())) shouldBe
            "female:tag$ -male:x$ ~parody:\"some series$\" solo$ --tank$ ~~loose$ a:b$"
        EHentaiQuery.combine(FilterList()) shouldBe ""
        AdvSearchEntry("a" to "b", exclude = false, or = true).or shouldBe true
    }

    @Test
    fun filterListFromPreferences() {
        val source = harness.ehentai()
        harness.exhPreferences.exhWatchedListDefaultState.set(true)
        val filters = source.filterList()
        filters.size shouldBe 9
        (filters[4] as Watched).state shouldBe true
        filters[0].javaClass shouldBe Filter.Header::class.java
        filters[2].javaClass shouldBe Filter.Separator::class.java
        (filters[7] as ReverseFilter).state shouldBe false
        (filters[8] as JumpSeekFilter).state shouldBe ""
        harness.exhPreferences.exhWatchedListDefaultState.set(false)
        (source.filterList()[4] as Watched).state shouldBe false
    }
}
