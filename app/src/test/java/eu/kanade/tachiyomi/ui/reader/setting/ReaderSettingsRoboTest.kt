package eu.kanade.tachiyomi.ui.reader.setting

import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class ReaderSettingsRoboTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun colorFiltersFromPie() {
        ReaderPreferences.ColorFilterMode.size shouldBe 6
    }

    @Test
    fun screenModelMapsState() {
        val prefs = ReaderPreferences(InMemoryPreferenceStore())
        startKoin { modules(module { single { prefs } }) }
        val state = MutableStateFlow(ReaderViewModel.State())
        val model = ReaderSettingsScreenModel(
            readerState = state,
            onChangeReadingMode = {},
            onChangeOrientation = {},
        )
        model.preferences shouldBe prefs
        val manga = Manga.create().copy(id = 5L)
        state.value = ReaderViewModel.State(manga = manga)
        runBlocking {
            withTimeout(5_000) {
                model.mangaFlow.first { it == manga } shouldBe manga
                model.viewerFlow.first { it == null } shouldBe null
            }
        }
        model.onChangeReadingMode(ReadingMode.WEBTOON)
        model.onChangeOrientation(ReaderOrientation.FREE)
        model.onDispose()
    }
}
