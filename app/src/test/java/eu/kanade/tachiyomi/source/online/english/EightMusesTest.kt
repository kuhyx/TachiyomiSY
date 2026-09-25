package eu.kanade.tachiyomi.source.online.english

import android.net.Uri
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.FakeDelegateSource
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.invokeDeclared
import eu.kanade.tachiyomi.source.online.jsoup
import eu.kanade.tachiyomi.source.online.rawTag
import eu.kanade.tachiyomi.source.online.sChapter
import eu.kanade.tachiyomi.source.online.sManga
import eu.kanade.tachiyomi.source.online.serveMetadataSource
import exh.metadata.metadata.EightMusesSearchMetadata
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val FULL = """
<html><body>
<div class="top-menu-breadcrumb"><ol><li><a>Home</a></li><li><a>Artist Name</a></li><li><a>Album Title</a></li></ol></div>
<div class="gallery">
<a class="c-tile" href="/comics/picture/a/b/1"><img class="lazyload" data-src="/image/1.jpg"></a>
<a class="c-tile" href="/comics/album/a/b/c"><img class="lazyload" data-src="/image/c.jpg"></a>
<a class="c-tile" href="/other"></a>
</div>
<div class="album-tags"><a>tag1</a><a>tag2</a></div>
</body></html>
"""

@RunWith(RobolectricTestRunner::class)
internal class EightMusesTest {
    private val harness = SourceTestHarness()
    private lateinit var source: EightMuses

    @Before
    fun setUp() {
        harness.install()
        harness.serveMetadataSource()
        source = EightMuses(FakeDelegateSource(harness.baseUrl, name = "8Muses"), harness.application)
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun identity() {
        source.lang shouldBe "en"
        source.metaClass shouldBe EightMusesSearchMetadata::class
        source.newMetaInstance().javaClass shouldBe EightMusesSearchMetadata::class.java
        source.matchingHosts shouldContainExactly listOf("www.8muses.com", "comics.8muses.com", "8muses.com")
    }

    @Test
    fun fullPage() {
        val meta = EightMusesSearchMetadata()
        runBlocking { source.parseIntoMetadata(meta, jsoup(FULL, "https://comics.8muses.com/comics/album/a/b")) }
        meta.path shouldContainExactly listOf("comics", "album", "a", "b")
        meta.title shouldBe "Album Title"
        meta.thumbnailUrl shouldBe "${harness.baseUrl}/image/c.jpg"
        meta.tags shouldContainExactly listOf(
            rawTag("artist", "Artist Name", EightMusesSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("tags", "tag1", EightMusesSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("tags", "tag2", EightMusesSearchMetadata.TAG_TYPE_DEFAULT),
        )
    }

    @Test
    fun pageWithoutThumbnails() {
        val meta = EightMusesSearchMetadata()
        val html = FULL.substringBefore("""<div class="gallery">""") + "</body></html>"
        runBlocking { source.parseIntoMetadata(meta, jsoup(html, "https://comics.8muses.com/comics/album/a")) }
        meta.thumbnailUrl.shouldBeNull()
        meta.tags.size shouldBe 1
        val noSrc = FULL.replace("""<img class="lazyload" data-src="/image/c.jpg">""", "")
            .replace("""<img class="lazyload" data-src="/image/1.jpg">""", "")
        val meta2 = EightMusesSearchMetadata()
        runBlocking { source.parseIntoMetadata(meta2, jsoup(noSrc, "https://comics.8muses.com/comics/album/a")) }
        meta2.thumbnailUrl.shouldBeNull()
    }

    @Test
    fun mapUrlToMangaUrl() {
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://comics.8muses.com/comics/album/a/b")) } shouldBe
            "/comics/album/a/b"
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://comics.8muses.com/comics/Picture/a/b/3")) } shouldBe
            "/comics/album/a/b"
    }

    @Test
    fun updateSearchAndDetailsDelegate() {
        val manga = sManga("/comics/album/a")
        val chapters = listOf(sChapter("/comics/album/a/1"))
        val update = runBlocking { source.getMangaUpdate(manga, chapters, fetchDetails = false, fetchChapters = false) }
        update.manga shouldBe manga
        update.chapters shouldBe chapters
        runBlocking { source.getSearchManga(1, "q", FilterList()) }.mangas.single().url shouldBe "/search/1/q"
        val rx = source.invokeDeclared(EightMuses::class, "fetchSearchManga", listOf(2, "r", FilterList()))
        ((rx as rx.Observable<*>).toBlocking().first() as MangasPage).mangas.single().url shouldBe "/search/2/r"
        harness.enqueue(FULL)
        val details = source.invokeDeclared(EightMuses::class, "fetchMangaDetails", listOf(sManga("/comics/album/a/b")))
        ((details as rx.Observable<*>).toBlocking().first() as SManga).title shouldBe "Album Title"
        harness.takeRequest().target shouldBe "/comics/album/a/b"
        EightMuses.SelfContents(emptyList(), emptyList()).copy(albums = emptyList()).albums.isEmpty() shouldBe true
    }
}
