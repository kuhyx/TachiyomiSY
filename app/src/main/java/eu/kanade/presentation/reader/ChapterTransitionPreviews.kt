package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import tachiyomi.domain.chapter.model.Chapter

internal fun previewChapter(name: String, scanlator: String, chapterNumber: Double) = Chapter.create().copy(
    id = 0L,
    mangaId = 0L,
    url = "",
    name = name,
    scanlator = scanlator,
    chapterNumber = chapterNumber,
)

@PreviewLightDark
@Composable
internal fun TransitionTextPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            ChapterTransition(
                transition = ChapterTransition.Next(ReaderChapter(FakeChapter), ReaderChapter(FakeChapter)),
                currChapterDownloaded = false,
                goingToChapterDownloaded = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
internal fun TransitionTextLongTitlePreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            ChapterTransition(
                transition = ChapterTransition.Next(ReaderChapter(FakeChapterLongTitle), ReaderChapter(FakeChapter)),
                currChapterDownloaded = true,
                goingToChapterDownloaded = true,
            )
        }
    }
}

@PreviewLightDark
@Composable
internal fun TransitionTextWithGapPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            ChapterTransition(
                transition = ChapterTransition.Next(ReaderChapter(FakeChapter), ReaderChapter(FakeGapChapter)),
                currChapterDownloaded = true,
                goingToChapterDownloaded = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
internal fun TransitionTextNoNextPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            ChapterTransition(
                transition = ChapterTransition.Next(ReaderChapter(FakeChapter), null),
                currChapterDownloaded = true,
                goingToChapterDownloaded = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
internal fun TransitionNoPreviousPreview() {
    TachiyomiPreviewTheme {
        Surface(modifier = Modifier.padding(48.dp)) {
            ChapterTransition(
                transition = ChapterTransition.Prev(ReaderChapter(FakeChapter), null),
                currChapterDownloaded = true,
                goingToChapterDownloaded = false,
            )
        }
    }
}
