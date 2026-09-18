package exh.metadata.metadata

import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.base.RaisedTag
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope

internal class EHentaiSearchMetadataMangaTest {
    private lateinit var previousScope: InjektScope

    @BeforeEach
    fun setUp() {
        previousScope = injectDelegatePrefs(preferJapanese = false)
    }

    @AfterEach
    fun tearDown() {
        Injekt = previousScope
        unmockkAll()
    }

    private fun manga(): SManga = SManga(
        url = "/manga/url",
        title = "Manga title",
        artist = "Manga artist",
        author = "Manga author",
        description = "Manga description",
        genre = "manga: genre",
        status = SManga.LICENSED,
        thumbnailUrl = "https://manga/thumb.jpg",
        initialized = true,
    )

    @Test
    fun keyNeedsBothIdAndToken() {
        val metadata = EHentaiSearchMetadata()
        metadata.createMangaInfo(manga()).url shouldBe "/manga/url"
        metadata.gId = "9"
        metadata.createMangaInfo(manga()).url shouldBe "/manga/url"
        metadata.gToken = "tok"
        metadata.createMangaInfo(manga()).url shouldBe "/g/9/tok/?nw=always"
    }

    @Test
    fun titleFallsBackToManga() {
        val metadata = EHentaiSearchMetadata()
        metadata.createMangaInfo(manga()).title shouldBe "Manga title"
        metadata.title = "Scraped"
        metadata.createMangaInfo(manga()).title shouldBe "Scraped"
    }

    @Test
    fun altTitleIgnoredWhenDisabled() {
        val metadata = EHentaiSearchMetadata().apply {
            title = "Romaji"
            altTitle = "日本語"
        }
        metadata.createMangaInfo(manga()).title shouldBe "Romaji"
    }

    @Test
    fun altTitlePreferredWhenEnabled() {
        injectDelegatePrefs(preferJapanese = true)
        val metadata = EHentaiSearchMetadata().apply {
            title = "Romaji"
            altTitle = "日本語"
        }
        metadata.createMangaInfo(manga()).title shouldBe "日本語"
        metadata.altTitle = null
        metadata.createMangaInfo(manga()).title shouldBe "Romaji"
    }

    @Test
    fun artistAndGroupFallBackToManga() {
        val info = EHentaiSearchMetadata().createMangaInfo(manga())
        info.author shouldBe "Manga artist"
        info.artist shouldBe "Manga artist"
    }

    @Test
    fun artistAndGroupComeFromTags() {
        val metadata = EHentaiSearchMetadata().apply {
            tags += RaisedTag(namespace = "artist", name = "ann", type = 0)
            tags += RaisedTag(namespace = "artist", name = "bob", type = 0)
            tags += RaisedTag(namespace = "group", name = "circle", type = 0)
        }
        val info = metadata.createMangaInfo(manga())
        info.author shouldBe "ann, bob"
        info.artist shouldBe "circle"
        info.genre shouldBe "artist: ann, artist: bob, group: circle"
    }

    @Test
    fun statusDefaultsToCompleted() {
        EHentaiSearchMetadata().createMangaInfo(manga()).status shouldBe SManga.COMPLETED
        val titled = EHentaiSearchMetadata().apply { title = "Finished work" }
        titled.createMangaInfo(manga()).status shouldBe SManga.COMPLETED
    }

    @Test
    fun statusOngoingFromTitleSuffix() {
        val metadata = EHentaiSearchMetadata().apply { title = "Serial [ONGOING]" }
        metadata.createMangaInfo(manga()).status shouldBe SManga.ONGOING
        metadata.title = "Serial wip"
        metadata.createMangaInfo(manga()).status shouldBe SManga.ONGOING
    }

    @Test
    fun coverFallsBackToManga() {
        val metadata = EHentaiSearchMetadata()
        metadata.createMangaInfo(manga()).thumbnail_url shouldBe "https://manga/thumb.jpg"
        metadata.thumbnailUrl = "https://eh/cover.jpg"
        metadata.createMangaInfo(manga()).thumbnail_url shouldBe "https://eh/cover.jpg"
    }

    @Test
    fun descriptionClearedGenreEmpty() {
        val info = EHentaiSearchMetadata().createMangaInfo(manga())
        info.description shouldBe null
        info.genre shouldBe ""
        info.initialized shouldBe true
    }
}
