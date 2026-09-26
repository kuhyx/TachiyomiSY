package eu.kanade.tachiyomi.ui.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.tachiyomi.ui.base.libraryManga
import exh.source.EH_SOURCE_ID
import exh.source.mangaDexSourceIds
import io.mockk.every
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo

/** The fork's overflow actions of the selection bar: clean titles, MangaDex follows and reset info. */
@RunWith(RobolectricTestRunner::class)
internal class LibraryTabSyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = LibraryTabRig(compose)
    private val savedMangaDex = mangaDexSourceIds

    @Before
    fun setUp() {
        mangaDexSourceIds = listOf(MANGADEX)
        rig.start(module { single { GetCustomMangaInfo(EditedInfo) } })
    }

    @After
    fun tearDown() {
        mangaDexSourceIds = savedMangaDex
        rig.stop()
    }

    private fun selectOnly(title: String, source: Long, id: Long = 1, shown: String = title) {
        val entry = manga(id, title).copy(source = source, favorite = true)
        rig.harness.library.value = listOf(libraryManga(id, entry, listOf(1L)))
        rig.show(shown)
        rig.select(shown)
        rig.click("More")
    }

    @Test
    fun cleaningTitles() {
        selectOnly("[G] Alpha", EH_SOURCE_ID)
        rig.click("Clean titles")
        verify(timeout = LIBRARY_WAIT) { rig.harness.setCustomMangaInfo.set(CustomMangaInfo(id = 1, title = "Alpha")) }
    }

    @Test
    fun followingOnMangadex() {
        every { rig.harness.sourceManager.getVisibleOnlineSources() } returns emptyList()
        selectOnly("Alpha", MANGADEX)
        rig.click("Add to MangaDex follows")
        rig.waitUntilGone("Mark as read")
    }

    @Test
    fun resettingInfo() {
        // Entry 11 has an edited title in [EditedInfo].
        selectOnly("Original", 1, id = 11, shown = "Edited")
        rig.click("Reset Info")
        verify(timeout = LIBRARY_WAIT) { rig.harness.setCustomMangaInfo.set(CustomMangaInfo(id = 11, title = null)) }
    }

    private companion object {
        const val MANGADEX = 2_499L
    }
}
