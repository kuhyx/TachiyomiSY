package exh.ui.metadata.adapters

import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.R
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.HBrowseSearchMetadata
import exh.metadata.metadata.LanraragiSearchMetadata
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HBrowseLanraragiDescriptionTest {
    @get:Rule
    val compose = createComposeRule()

    private val host = DescriptionHost(compose)

    @Test
    fun hBrowsePages() {
        host.show { HBrowseDescription(successState(HBrowseSearchMetadata().apply { length = 1 }), it) }
        host.text(R.id.pages) shouldBe "1 page"
        host.pressAll(R.id.pages)
        host.viewerOpened shouldBe 1
    }

    @Test
    fun hBrowseWithoutLength() {
        host.show { HBrowseDescription(successState(HBrowseSearchMetadata()), it) }
        host.text(R.id.pages) shouldBe "0 pages"
    }

    @Test
    fun hBrowseIgnoresOthers() {
        host.show { HBrowseDescription(successState(EHentaiSearchMetadata()), it) }
        host.text(R.id.pages) shouldBe ""
    }

    @Test
    fun hBrowseIgnoresNull() {
        host.show { HBrowseDescription(successState(null), it) }
        host.text(R.id.pages) shouldBe ""
    }

    @Test
    fun lanraragiFields() {
        val meta = LanraragiSearchMetadata().apply {
            extension = "zip"
            pageCount = 4
        }
        host.show { LanraragiDescription(successState(meta), it) }
        host.text(R.id.ext) shouldBe "ZIP"
        host.text(R.id.pages) shouldBe "4 pages"
        host.pressAll(R.id.pages, R.id.ext)
        host.viewerOpened shouldBe 1
    }

    @Test
    fun lanraragiDefaults() {
        host.show { LanraragiDescription(successState(LanraragiSearchMetadata()), it) }
        host.text(R.id.ext) shouldBe ""
        host.text(R.id.pages) shouldBe "1 page"
    }

    @Test
    fun lanraragiIgnoresOthers() {
        host.show { LanraragiDescription(successState(EHentaiSearchMetadata()), it) }
        host.text(R.id.pages) shouldBe ""
    }

    @Test
    fun lanraragiIgnoresNull() {
        host.show { LanraragiDescription(successState(null), it) }
        host.text(R.id.pages) shouldBe ""
    }
}
