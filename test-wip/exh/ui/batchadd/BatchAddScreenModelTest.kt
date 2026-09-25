package exh.ui.batchadd

import android.content.Context
import exh.GalleryAdderHarness
import exh.source.ExhPreferences
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore

private const val WAIT_MS = 20_000L

@RunWith(RobolectricTestRunner::class)
internal class BatchAddScreenModelTest {
    private val harness = GalleryAdderHarness()
    private val preferences = ExhPreferences(InMemoryPreferenceStore())

    @Before
    fun setUp() {
        harness.start()
    }

    @After
    fun tearDown() = harness.stop()

    private fun BatchAddScreenModel.settled(): BatchAddState = runBlocking {
        withTimeout(WAIT_MS) {
            state.first { it.progressTotal > 0 && it.events.size == it.progressTotal + 1 }
        }
    }

    @Test
    fun blankInputOpensTheDialog() {
        val model = BatchAddScreenModel(preferences)
        model.updateGalleries("  \n ")
        model.addGalleries(harness.context)
        model.state.value.dialog shouldBe BatchAddScreenModel.Dialog.NoGalleriesSpecified
        model.dismissDialog()
        model.state.value.dialog shouldBe null
        model.state.value.state shouldBe BatchAddScreenModel.State.INPUT
    }

    @Test
    fun oneUrlPerLine() {
        val uris = mutableListOf<String>()
        val uri = slot<android.net.Uri>()
        every { harness.source.matchesUri(capture(uri)) } answers {
            uris += uri.captured.toString()
            "bad" !in uri.captured.toString()
        }
        val model = BatchAddScreenModel(preferences)
        model.updateGalleries(" https://gallery.test/g/1/abc \n\nhttps://bad.test/x")
        model.addGalleries(harness.context)
        model.state.value.state shouldBe BatchAddScreenModel.State.PROGRESS
        val done = model.settled()
        done.progress shouldBe 2
        done.events.size shouldBe 3
        done.events[0].startsWith("[OK] ") shouldBe true
        done.events[1].startsWith("[ERROR] ") shouldBe true
        done.events[2] shouldBe "\nSummary:\nAdded: 1 gallerie(s)\nFailed: 1 gallerie(s)"
        uris.first() shouldBe "https://gallery.test/g/1/abc"
    }

    @Test
    fun visitedKeysBecomeEhUrls() {
        val model = BatchAddScreenModel(preferences)
        model.updateGalleries("123.abc: 456.def:")
        model.addGalleries(harness.context)
        model.settled().progressTotal shouldBe 2
        verify { harness.source.matchesUri(match { it.toString() == "https://e-hentai.org/g/123/abc" }) }
        verify { harness.source.matchesUri(match { it.toString() == "https://e-hentai.org/g/456/def" }) }
    }

    @Test
    fun visitedKeysOnExhentai() {
        preferences.enableExhentai.set(true)
        val model = BatchAddScreenModel(preferences)
        model.updateGalleries("7.ff:")
        model.addGalleries(harness.context)
        model.settled()
        verify { harness.source.matchesUri(match { it.toString() == "https://exhentai.org/g/7/ff" }) }
    }

    @Test
    fun finishResetsTheInput() {
        val model = BatchAddScreenModel(preferences)
        model.updateGalleries("https://gallery.test/g/1/abc")
        model.addGalleries(harness.context)
        model.settled()
        model.finish()
        val state = model.state.value
        state.galleries shouldBe ""
        state.progress shouldBe 0
        state.progressTotal shouldBe 0
        state.events shouldContainExactly emptyList()
        state.state shouldBe BatchAddScreenModel.State.INPUT
    }

    @Test
    fun aCrashIsLoggedNotThrown() {
        // A bare mock context fails the adder's first string lookup, outside its own error handling.
        val model = BatchAddScreenModel(preferences)
        model.updateGalleries("https://gallery.test/g/1/abc")
        model.addGalleries(mockk<Context>())
        Thread.sleep(500)
        model.state.value.events shouldContainExactly emptyList()
        model.state.value.progress shouldBe 0
    }

    @Test
    fun preferencesComeFromInjekt() {
        loadKoinModules(module { single { preferences } })
        val model = BatchAddScreenModel()
        model.updateGalleries("9.aa:")
        model.addGalleries(harness.context)
        model.settled().progressTotal shouldBe 1
    }
}
