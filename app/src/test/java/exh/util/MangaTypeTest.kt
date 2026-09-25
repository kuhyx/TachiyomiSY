package exh.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.sy.SYMR
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
internal class MangaTypeTest {
    private val sourceManager = mockk<SourceManager>()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single<SourceManager> { sourceManager } }) }
        every { sourceManager.get(NAMED_SOURCE) } returns mockk<Source> { every { name } returns "Toonily" }
        every { sourceManager.get(UNKNOWN_SOURCE) } returns null
    }

    @After
    fun tearDown() = stopKoin()

    private fun manga(genre: List<String>?, source: Long = UNKNOWN_SOURCE) =
        Manga.create().copy(ogGenre = genre, source = source)

    @Test
    fun explicitMangaTagWins() {
        manga(listOf("webtoon", "manga")).mangaType() shouldBe MangaType.TYPE_MANGA
    }

    @Test
    fun tagsDecideTheType() {
        manga(listOf("Webtoon")).mangaType() shouldBe MangaType.TYPE_WEBTOON
        manga(listOf("Comic")).mangaType() shouldBe MangaType.TYPE_COMIC
        manga(listOf("Manhua")).mangaType() shouldBe MangaType.TYPE_MANHUA
        manga(listOf("Manhwa")).mangaType() shouldBe MangaType.TYPE_MANHWA
    }

    @Test
    fun sourceNameDecidesTheType() {
        manga(emptyList(), source = NAMED_SOURCE).mangaType() shouldBe MangaType.TYPE_MANHWA
        manga(null).mangaType("webtoons") shouldBe MangaType.TYPE_WEBTOON
        manga(null).mangaType("xkcd") shouldBe MangaType.TYPE_COMIC
        manga(null).mangaType("Manhuaus") shouldBe MangaType.TYPE_MANHUA
    }

    @Test
    fun nothingMatchedIsManga() {
        manga(listOf("action"), source = UNKNOWN_SOURCE).mangaType() shouldBe MangaType.TYPE_MANGA
        manga(null).mangaType("Some Source") shouldBe MangaType.TYPE_MANGA
    }

    @Test
    fun readerTypeIsWebtoonForStrips() {
        val flag = ReadingMode.WEBTOON.flagValue
        manga(listOf("manhwa")).defaultReaderType() shouldBe flag
        manga(null).defaultReaderType(MangaType.TYPE_WEBTOON) shouldBe flag
        manga(null).defaultReaderType(MangaType.TYPE_COMIC).shouldBeNull()
        manga(null).defaultReaderType(MangaType.TYPE_MANGA).shouldBeNull()
    }

    @Test
    fun localisedNamesPerType() {
        fun expected(res: dev.icerock.moko.resources.StringResource) =
            context.stringResource(res).lowercase(Locale.getDefault())
        manga(listOf("webtoon")).mangaType(context) shouldBe expected(SYMR.strings.entry_type_webtoon)
        manga(listOf("manhwa")).mangaType(context) shouldBe expected(SYMR.strings.entry_type_manhwa)
        manga(listOf("manhua")).mangaType(context) shouldBe expected(SYMR.strings.entry_type_manhua)
        manga(listOf("comic")).mangaType(context) shouldBe expected(SYMR.strings.entry_type_comic)
        manga(listOf("manga")).mangaType(context) shouldBe expected(SYMR.strings.entry_type_manga)
    }

    private companion object {
        const val NAMED_SOURCE = 10L
        const val UNKNOWN_SOURCE = 11L
    }
}
