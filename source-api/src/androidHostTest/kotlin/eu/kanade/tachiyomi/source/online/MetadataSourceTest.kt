package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.RankedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.sql.models.SearchMetadata
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/** A [MetadataSource] over [RankedSearchMetadata] whose input is the rank to store. */
internal class RankedMetadataSource : StubSource(id = 11L), MetadataSource<RankedSearchMetadata, Int> {
    var created: Int = 0

    override val metaClass: KClass<RankedSearchMetadata> = RankedSearchMetadata::class

    override suspend fun parseIntoMetadata(metadata: RankedSearchMetadata, input: Int) {
        metadata.rank = input
    }

    override fun newMetaInstance(): RankedSearchMetadata = RankedSearchMetadata().also { created++ }
}

/** Metadata row 5 as stored in the database: uploader "up" and [rank] in the extra json. */
internal fun storedRank(rank: Int): FlatMetadata = FlatMetadata(
    metadata = SearchMetadata(
        mangaId = 5L,
        uploader = "up",
        extra = """{"rank":$rank}""",
        indexedExtra = null,
        extraVersion = 0,
    ),
    tags = emptyList(),
    titles = emptyList(),
)

/** The database round trip of [MetadataSource.parseToManga] and [MetadataSource.fetchOrLoadMetadata]. */
internal class MetadataSourceTest {
    private val harness = SourceHarness()
    private val source = RankedMetadataSource()
    private val manga = SManga(url = "/gallery/1", title = "t")
    private val getMangaId = mockk<MetadataSource.GetMangaId>()
    private val insert = mockk<MetadataSource.InsertFlatMetadata>()
    private val getFlat = mockk<MetadataSource.GetFlatMetadataById>()
    private val saved = mutableListOf<RaisedSearchMetadata>()
    private val stored = storedRank(9)

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
    fun injectedCollaboratorsResolve() {
        (source.getMangaId === getMangaId) shouldBe true
        (source.insertFlatMetadata === insert) shouldBe true
        (source.getFlatMetadataById === getFlat) shouldBe true
    }

    @Test
    fun mangaIdLooksUpUrlAndSource() = runTest {
        coEvery { getMangaId.awaitId("/gallery/1", 11L) } returns 5L
        with(source) { manga.mangaId() } shouldBe 5L
    }

    @Test
    fun parseToMangaWithoutIdSkipsDb() = runTest {
        coEvery { getMangaId.awaitId(any(), any()) } returns null
        (source.parseToManga(manga, 3) === manga) shouldBe true
        source.created shouldBe 1
        saved shouldBe emptyList()
        coVerify(exactly = 0) { getFlat.await(any()) }
    }

    @Test
    fun parseToMangaRaisesStored() = runTest {
        coEvery { getMangaId.awaitId(any(), any()) } returns 5L
        coEvery { getFlat.await(5L) } returns stored
        source.parseToManga(manga, 3)
        source.created shouldBe 0
        val meta = saved.single() as RankedSearchMetadata
        meta.rank shouldBe 3
        meta.mangaId shouldBe 5L
        meta.uploader shouldBe "up"
    }

    @Test
    fun parseToMangaCreatesIfUnstored() = runTest {
        coEvery { getMangaId.awaitId(any(), any()) } returns 5L
        coEvery { getFlat.await(5L) } returns null
        source.parseToManga(manga, 4)
        source.created shouldBe 1
        (saved.single() as RankedSearchMetadata).rank shouldBe 4
        saved.single().mangaId shouldBe 5L
    }

    @Test
    fun fetchOrLoadPrefersStored() = runTest {
        coEvery { getFlat.await(5L) } returns stored
        val meta = source.fetchOrLoadMetadata(5L) { error("input must not be produced") }
        meta.rank shouldBe 9
        meta.mangaId shouldBe 5L
        saved shouldBe emptyList()
    }

    @Test
    fun fetchOrLoadParsesWhenMissing() = runTest {
        coEvery { getFlat.await(5L) } returns null
        val meta = source.fetchOrLoadMetadata(5L) { 6 }
        meta.rank shouldBe 6
        meta.mangaId shouldBe 5L
        (saved.single() === meta) shouldBe true
    }

    @Test
    fun fetchOrLoadWithoutIdSkipsDb() = runTest {
        val meta = source.fetchOrLoadMetadata(null) { 7 }
        meta.rank shouldBe 7
        meta.mangaId shouldBe -1L
        saved shouldBe emptyList()
        coVerify(exactly = 0) { getFlat.await(any()) }
    }
}
