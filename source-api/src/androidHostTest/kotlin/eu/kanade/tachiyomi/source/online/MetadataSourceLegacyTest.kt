package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.RankedSearchMetadata
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import rx.Completable
import rx.Single

/** The deprecated Rx wrappers of [MetadataSource], reached reflectively so the module's warning gate stays green. */
internal class MetadataSourceLegacyTest {
    private val harness = SourceHarness()
    private val source = RankedMetadataSource()
    private val manga = SManga(url = "/gallery/1", title = "t")
    private val getMangaId = mockk<MetadataSource.GetMangaId>()
    private val insert = mockk<MetadataSource.InsertFlatMetadata>()
    private val getFlat = mockk<MetadataSource.GetFlatMetadataById>()
    private val saved = mutableListOf<RaisedSearchMetadata>()

    @BeforeEach
    fun setUp() {
        harness.services[MetadataSource.GetMangaId::class.java] = getMangaId
        harness.services[MetadataSource.InsertFlatMetadata::class.java] = insert
        harness.services[MetadataSource.GetFlatMetadataById::class.java] = getFlat
        harness.install()
        coEvery { insert.await(any()) } answers { saved += firstArg<RaisedSearchMetadata>() }
    }

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun completableParsesIntoManga() {
        coEvery { getMangaId.awaitId(any(), any()) } returns 5L
        coEvery { getFlat.await(5L) } returns null
        val completable = source.invokeDeclared(MetadataSource::class, "parseToMangaCompletable", listOf(manga, 3))
        (completable as Completable).await()
        (saved.single() as RankedSearchMetadata).rank shouldBe 3
        saved.single().mangaId shouldBe 5L
    }

    @Test
    fun singleLoadsStoredMetadata() {
        coEvery { getFlat.await(5L) } returns storedRank(8)
        val producer: () -> Single<Int> = { Single.just(1) }
        val single = source.invokeDeclared(MetadataSource::class, "getOrLoadMetadata", listOf(5L, producer))
        val meta = (single as Single<*>).toBlocking().value() as RankedSearchMetadata
        meta.rank shouldBe 8
        saved shouldBe emptyList()
    }

    @Test
    fun singleParsesProducedInput() {
        coEvery { getFlat.await(5L) } returns null
        val producer: () -> Single<Int> = { Single.just(2) }
        val single = source.invokeDeclared(MetadataSource::class, "getOrLoadMetadata", listOf(5L, producer))
        val meta = (single as Single<*>).toBlocking().value() as RankedSearchMetadata
        meta.rank shouldBe 2
        (saved.single() === meta) shouldBe true
    }
}
