package eu.kanade.tachiyomi.ui.manga

import exh.debug.DebugToggles
import exh.eh.ChapterChain
import exh.source.EH_SOURCE_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Accepted roots that do not redirect, and the debug toggle that turns the check off. The check runs
 * after the accepted root comes back, with nothing observable, so the root's getters say when it ran.
 */
@RunWith(RobolectricTestRunner::class)
internal class MangaRedirectEdgesTest {
    private val harness = MangaHarness()
    private val read = AtomicBoolean()

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga(source = EH_SOURCE_ID, favorite = true) to listOf(chapter(1L))
    }

    @After
    fun tearDown() {
        unmockkObject(DebugToggles.ENABLE_EXH_ROOT_REDIRECT)
        harness.stop()
    }

    private fun accept(root: Manga) {
        coEvery { harness.updateHelper.acceptRootAndDiscardOthers(any(), any()) } returns
            Triple(ChapterChain(root, emptyList(), emptyList()), emptyList(), emptyList())
    }

    @Test
    fun sameRootIsNotRedirected() {
        val root = manga(favorite = true)
        accept(
            spyk(root) {
                every { id } answers {
                    read.set(true)
                    root.id
                }
            },
        )
        harness.loaded()
        eventually { read.get() }
    }

    @Test
    fun unfavouredRootIsIgnored() {
        val root = manga().copy(id = 4L)
        accept(
            spyk(root) {
                every { favorite } answers {
                    read.set(true)
                    false
                }
            },
        )
        harness.loaded()
        eventually { read.get() }
    }

    @Test
    fun disabledToggleSkipsTheCheck() {
        // Mocked: the toggle's store is resolved once per JVM, by whichever test read a toggle first.
        mockkObject(DebugToggles.ENABLE_EXH_ROOT_REDIRECT)
        every { DebugToggles.ENABLE_EXH_ROOT_REDIRECT.enabled } returns false
        harness.loaded().awaitSuccess { it.chapters.size == 1 }
        coVerify(exactly = 0) { harness.updateHelper.acceptRootAndDiscardOthers(any(), any()) }
    }
}
