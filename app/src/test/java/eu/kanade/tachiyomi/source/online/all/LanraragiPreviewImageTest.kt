package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.PagePreviewInfo
import eu.kanade.tachiyomi.source.online.FakeDelegateSource
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.serveMetadataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import okhttp3.CacheControl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class LanraragiPreviewImageTest {
    private val harness = SourceTestHarness()
    private lateinit var source: Lanraragi

    @Before
    fun setUp() {
        harness.install()
        harness.serveMetadataSource()
        source = Lanraragi(FakeDelegateSource(harness.baseUrl, lang = "all", name = "LANraragi"), harness.application)
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun previewImageReadyAtOnce() {
        val preview = PagePreviewInfo(1, "${harness.baseUrl}/api/archives/$ARC_ID/thumbnail")
        harness.enqueueBytes(byteArrayOf(1, 2), "image/jpeg")
        runBlocking { source.fetchPreviewImage(preview, CacheControl.FORCE_NETWORK) }.body.bytes().size shouldBe 2
        harness.takeRequest().headers["Cache-Control"] shouldBe "no-cache"
        harness.enqueueBytes(byteArrayOf(1), "image/jpeg")
        runBlocking { source.fetchPreviewImage(preview) }.body.bytes().size shouldBe 1
        harness.takeRequest().headers["Cache-Control"] shouldBe "max-age=600"
    }

    @Test
    fun previewImageWaitsForJob() {
        val preview = PagePreviewInfo(1, "${harness.baseUrl}/api/archives/$ARC_ID/thumbnail")
        harness.enqueue("""{"job":5,"operation":"thumbnail","success":1}""", code = 202)
        harness.enqueue("""{"state":"finished"}""")
        harness.enqueueBytes(byteArrayOf(9), "image/jpeg")
        runBlocking { source.fetchPreviewImage(preview) }.body.bytes().size shouldBe 1
        harness.server.requestCount shouldBe 3
    }

    @Test
    fun previewImageGivesUp() {
        val preview = PagePreviewInfo(1, "${harness.baseUrl}/api/archives/$ARC_ID/thumbnail")
        harness.enqueue("""{"job":5,"operation":"thumbnail","success":1}""", code = 202)
        repeat(4) { harness.enqueue("""{"state":"active"}""") }
        harness.enqueue("""{"job":5,"operation":"thumbnail","success":1}""", code = 202)
        shouldThrow<IOException> { runBlocking { source.fetchPreviewImage(preview) } }.message shouldBe
            "Thumbnail not ready"
        harness.server.requestCount shouldBe 6
    }
}
