package exh.ui.metadata.adapters

import android.view.View
import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.R
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.EightMusesSearchMetadata
import exh.metadata.metadata.MangaDexSearchMetadata
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MangaDexEightMusesDescriptionTest {
    @get:Rule
    val compose = createComposeRule()

    private val host = DescriptionHost(compose)

    @Test
    fun mangaDexRating() {
        host.show { MangaDexDescription(successState(MangaDexSearchMetadata().apply { rating = 7.456F }), it) }
        host.text(R.id.rating) shouldBe "7.46 - Good"
        host.view(R.id.rating).visibility shouldBe View.VISIBLE
        host.pressAll(R.id.rating)
        host.viewerOpened shouldBe 1
    }

    @Test
    fun mangaDexWithoutRating() {
        host.show { MangaDexDescription(successState(MangaDexSearchMetadata()), it) }
        host.text(R.id.rating) shouldBe "0.0 - No rating"
        host.view(R.id.rating).visibility shouldBe View.GONE
        host.view(R.id.rating_bar).visibility shouldBe View.GONE
    }

    @Test
    fun mangaDexIgnoresOthers() {
        host.show { MangaDexDescription(successState(EHentaiSearchMetadata()), it) }
        host.text(R.id.rating) shouldBe ""
    }

    @Test
    fun mangaDexIgnoresNull() {
        host.show { MangaDexDescription(successState(null), it) }
        host.text(R.id.rating) shouldBe ""
    }

    @Test
    fun eightMusesTitle() {
        host.show {
            EightMusesDescription(successState(EightMusesSearchMetadata().apply { title = "Album" }), it)
        }
        host.text(R.id.title) shouldBe "Album"
        host.pressAll(R.id.title)
        host.viewerOpened shouldBe 1
    }

    @Test
    fun eightMusesWithoutTitle() {
        host.show { EightMusesDescription(successState(EightMusesSearchMetadata()), it) }
        host.text(R.id.title) shouldBe "Unknown"
    }

    @Test
    fun eightMusesIgnoresOthers() {
        host.show { EightMusesDescription(successState(EHentaiSearchMetadata()), it) }
        host.text(R.id.title) shouldBe ""
    }

    @Test
    fun eightMusesIgnoresNull() {
        host.show { EightMusesDescription(successState(null), it) }
        host.text(R.id.title) shouldBe ""
    }
}
