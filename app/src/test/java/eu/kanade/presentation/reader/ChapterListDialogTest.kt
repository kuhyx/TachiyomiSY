package eu.kanade.presentation.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalSource
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
internal class ChapterListDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val events = mutableListOf<String>()
    private val downloading = Chapter.create().copy(id = 2L, name = "Downloading", dateUpload = 86_400_000L)
    private val download = mockk<Download> {
        every { chapter } returns downloading
        every { status } returns Download.State.DOWNLOADING
        every { progress } returns 40
    }
    private val downloadManager = mockk<DownloadManager> {
        every { queueState } returns MutableStateFlow(listOf(download))
        every { progressFlow() } returns flowOf(download)
        every { isChapterDownloaded(any(), any(), any(), any(), any(), any()) } answers { firstArg<String>() == "Saved" }
    }

    @Before
    fun setUp() = koin.start(module { single { downloadManager } })

    @After
    fun tearDown() = koin.stop()

    private fun item(chapter: Chapter, manga: Manga, current: Boolean = false) =
        ReaderChapterItem(chapter, manga, current, DateTimeFormatter.ISO_LOCAL_DATE)

    private fun show(manga: Manga, relative: Boolean = false) {
        val harness = ReaderSettingsHarness(manga = manga)
        val chapters = listOf(
            item(Chapter.create().copy(id = 1L, name = "Saved"), manga),
            item(downloading, manga, current = true),
            item(Chapter.create().copy(id = 3L, name = "Plain", dateUpload = 1L), manga),
        )
        compose.setContent {
            MaterialTheme {
                ChapterListDialog(
                    onDismissRequest = {},
                    screenModel = harness.model,
                    chapters = chapters,
                    onClickChapter = { events += "open ${it.id}" },
                    onBookmark = { events += "bookmark ${it.id}" },
                    dateRelativeTime = relative,
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun rowsOpenTheirChapter() {
        show(Manga.create().copy(id = 1L, source = 2L))
        compose.onNodeWithText("Saved").performClick()
        compose.onNodeWithText("Downloading").performClick()
        compose.onNodeWithText("1970-01-02", substring = true).assertExists()
        events shouldContainExactly listOf("open 1", "open 2")
    }

    @Test
    fun ehGalleriesShowTheExactTime() {
        show(Manga.create().copy(id = 1L, source = EH_SOURCE_ID), relative = true)
        compose.onNodeWithText("Plain").assertExists()
    }

    @Test
    fun localMangaIsAlwaysDownloaded() {
        show(Manga.create().copy(id = 1L, source = LocalSource.ID))
        compose.onNodeWithText("Plain").performClick()
        events shouldContainExactly listOf("open 3")
    }
}
