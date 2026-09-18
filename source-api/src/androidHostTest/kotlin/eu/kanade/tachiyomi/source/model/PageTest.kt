package eu.kanade.tachiyomi.source.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

internal class PageTest {
    @Test
    fun indexOnly() {
        val page = Page(3)
        page.index shouldBe 3
        page.url shouldBe ""
        page.imageUrl shouldBe null
        page.uri shouldBe null
        page.number shouldBe 4
    }

    @Test
    fun indexAndUrl() {
        val page = Page(0, "https://example.invalid/p")
        page.url shouldBe "https://example.invalid/p"
        page.imageUrl shouldBe null
        page.number shouldBe 1
    }

    @Test
    fun indexUrlAndImage() {
        val page = Page(0, "u", "i")
        page.url shouldBe "u"
        page.imageUrl shouldBe "i"
    }

    @Test
    fun explicitNullUri() {
        val page = Page(index = 1, url = "u", imageUrl = "i", uri = null)
        page.uri shouldBe null
        page.index shouldBe 1
    }

    @Test
    fun urlAndImageAreMutable() {
        val page = Page(0)
        page.url = "changed"
        page.imageUrl = "img"
        page.url shouldBe "changed"
        page.imageUrl shouldBe "img"
    }

    @Test
    fun statusStartsQueuedAndFlows() {
        val page = Page(0)
        page.status shouldBe Page.State.Queue
        page.statusFlow.value shouldBe Page.State.Queue

        page.status = Page.State.LoadPage
        page.statusFlow.value shouldBe Page.State.LoadPage
        page.status = Page.State.DownloadImage
        page.status shouldBe Page.State.DownloadImage
        page.status = Page.State.Ready
        page.statusFlow.value shouldBe Page.State.Ready

        val failure = IllegalStateException("boom")
        page.status = Page.State.Error(failure)
        page.status shouldBe Page.State.Error(failure)
    }

    @Test
    fun progressStartsAtZeroAndFlows() {
        val page = Page(0)
        page.progress shouldBe 0
        page.progressFlow.value shouldBe 0
        page.progress = 55
        page.progress shouldBe 55
        page.progressFlow.value shouldBe 55
    }

    @Test
    fun updateComputesPercent() {
        val page = Page(0)
        page.update(bytesRead = 50L, contentLength = 200L, done = false)
        page.progress shouldBe 25
        page.update(bytesRead = 200L, contentLength = 200L, done = true)
        page.progress shouldBe 100
        page.update(bytesRead = 0L, contentLength = 1L, done = false)
        page.progress shouldBe 0
    }

    @Test
    fun updateWithUnknownLength() {
        val page = Page(0)
        page.update(bytesRead = 50L, contentLength = -1L, done = false)
        page.progress shouldBe -1
        page.update(bytesRead = 50L, contentLength = 0L, done = false)
        page.progressFlow.value shouldBe -1
    }

    @Test
    fun errorStateIsDataClass() {
        val cause = IllegalArgumentException("bad")
        val error = Page.State.Error(cause)
        error.error shouldBeSameInstanceAs cause
        error.component1() shouldBeSameInstanceAs cause
        error.copy() shouldBe error
        error.copy(error = cause) shouldBe error
        error.hashCode() shouldBe Page.State.Error(cause).hashCode()
        error.toString() shouldBe "Error(error=$cause)"
        error shouldNotBe Page.State.Error(IllegalArgumentException("other"))
        val queued: Page.State = Page.State.Queue
        (error == queued) shouldBe false
    }
}
