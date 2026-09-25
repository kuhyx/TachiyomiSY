package eu.kanade.tachiyomi.data.coil

import android.content.Context
import coil3.request.Options
import eu.kanade.domain.manga.model.PagePreview
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class PagePreviewKeyerTest {

    @Test
    fun keyIsTheImageUrl() {
        val preview = PagePreview(index = 3, imageUrl = "https://example.org/p3.png", source = 5L)
        val options = Options(context = mockk<Context>(relaxed = true))
        PagePreviewKeyer().key(data = preview, options = options) shouldBe "https://example.org/p3.png"
    }
}
