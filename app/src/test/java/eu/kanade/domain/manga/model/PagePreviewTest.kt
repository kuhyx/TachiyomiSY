package eu.kanade.domain.manga.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class PagePreviewTest {

    @Test
    fun serializesWithoutProgress() {
        val preview = PagePreview(index = 2, imageUrl = "http://img", source = 5)
        val json = """{"index":2,"imageUrl":"http://img","source":5}"""
        Json.encodeToString(PagePreview.serializer(), preview) shouldBe json
        Json.decodeFromString(PagePreview.serializer(), json) shouldBe preview
    }

    @Test
    fun exposesProgressToTheInfo() {
        val preview = PagePreview(index = 2, imageUrl = "http://img", source = 5)
        preview.progress.value shouldBe -1
        val info = preview.toPagePreviewInfo()
        info.index shouldBe 2
        info.imageUrl shouldBe "http://img"
        info.update(bytesRead = 40, contentLength = 100, done = false)
        preview.progress.value shouldBe 40
        preview.copy(index = 3).index shouldBe 3
        preview.toString() shouldBe "PagePreview(index=2, imageUrl=http://img, source=5)"
        preview.hashCode() shouldBe PagePreview(index = 2, imageUrl = "http://img", source = 5).hashCode()
    }
}
