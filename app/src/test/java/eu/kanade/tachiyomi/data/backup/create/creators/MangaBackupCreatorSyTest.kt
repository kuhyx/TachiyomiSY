package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.libraryManga
import eu.kanade.tachiyomi.data.backup.models.BackupMergedMangaReference
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.sql.models.SearchMetadata
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.source.service.SourceManager

private val mergedReference = BackupMergedMangaReference(
    isInfoManga = true,
    getChapterUpdates = true,
    chapterSortMode = 0,
    chapterPriority = 0,
    downloadChapters = false,
    mergeUrl = "merge://1",
    mangaUrl = "/m",
    mangaSourceId = 3L,
)

private val flatMetadata = FlatMetadata(
    metadata = SearchMetadata(
        mangaId = 1L,
        uploader = "u",
        extra = "{}",
        indexedExtra = null,
        extraVersion = 1,
    ),
    tags = emptyList(),
    titles = emptyList(),
)

internal class MangaBackupCreatorSyTest {

    private val db = FakeBackupDatabase()
    private val sourceManager = mockk<SourceManager>()
    private val getCustomMangaInfo = mockk<GetCustomMangaInfo>()
    private val getFlatMetadataById = mockk<GetFlatMetadataById>()
    private val getCategories = mockk<GetCategories>()
    private val getHistory = mockk<GetHistory>()

    @BeforeEach
    fun setUp() {
        coEvery { getCategories.await(any()) } returns emptyList()
        coEvery { getHistory.await(any()) } returns emptyList()
        every { sourceManager.get(any()) } returns null
        every { getCustomMangaInfo.get(any()) } returns null
        coEvery { getFlatMetadataById.await(any()) } returns null
        startKoin {
            modules(
                module {
                    single { db.database }
                    single { getCategories }
                    single { getHistory }
                    single { sourceManager }
                    single { getCustomMangaInfo }
                    single { getFlatMetadataById }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    private fun creator(): MangaBackupCreator = MangaBackupCreator(
        database = db.database,
        getCategories = getCategories,
        getHistory = getHistory,
        sourceManager = sourceManager,
        getCustomMangaInfo = getCustomMangaInfo,
        getFlatMetadataById = getFlatMetadataById,
    )

    private val options = BackupOptions(
        chapters = false,
        categories = false,
        tracking = false,
        history = false,
    )

    @Test
    fun mergedReferencesForMergedSource() = runTest {
        db.withMergedReferences(listOf(mergedReference))
        val manga = libraryManga(source = MERGED_SOURCE_ID)
        val backup = creator()(listOf(manga), options).single()
        backup.mergedMangaReferences.single() shouldBe mergedReference
    }

    @Test
    fun noMergedReferencesForOtherSources() = runTest {
        creator()(listOf(libraryManga()), options).single().mergedMangaReferences shouldBe emptyList()
    }

    @Test
    fun flatMetadataForMetadataSource() = runTest {
        every { sourceManager.get(any()) } returns mockk<MetadataSource<*, *>>()
        coEvery { getFlatMetadataById.await(any()) } returns flatMetadata
        val backup = creator()(listOf(libraryManga()), options).single()
        backup.flatMetadata?.searchMetadata?.uploader shouldBe "u"
    }

    @Test
    fun noFlatMetadataWhenSourceHasNone() = runTest {
        every { sourceManager.get(any()) } returns mockk<MetadataSource<*, *>>()
        creator()(listOf(libraryManga()), options).single().flatMetadata shouldBe null
    }

    @Test
    fun noFlatMetadataForPlainSource() = runTest {
        creator()(listOf(libraryManga()), options).single().flatMetadata shouldBe null
    }

    @Test
    fun customInfoCopiedWhenPresent() = runTest {
        every { getCustomMangaInfo.get(any()) } returns CustomMangaInfo(
            id = 1L,
            title = "Custom",
            author = "A",
            artist = "R",
            thumbnailUrl = "http://t",
            description = "D",
            genre = listOf("g"),
            status = 2L,
        )
        val backup = creator()(listOf(libraryManga()), options).single()
        backup.customTitle shouldBe "Custom"
        backup.customAuthor shouldBe "A"
        backup.customArtist shouldBe "R"
        backup.customThumbnailUrl shouldBe "http://t"
        backup.customDescription shouldBe "D"
        backup.customGenre shouldBe listOf("g")
        backup.customStatus shouldBe 2
    }

    @Test
    fun customStatusDefaultsToZero() = runTest {
        every { getCustomMangaInfo.get(any()) } returns CustomMangaInfo(id = 1L, title = null)
        creator()(listOf(libraryManga()), options).single().customStatus shouldBe 0
    }

    @Test
    fun toBackupMangaWithoutCustomInfo() {
        val manga = libraryManga().copy(
            ogArtist = "R",
            ogAuthor = "A",
            ogDescription = "D",
            ogGenre = listOf("g"),
            ogStatus = 3L,
            ogThumbnailUrl = "http://t",
            viewerFlags = ReadingMode.MASK + 1L,
        )
        val backup = manga.toBackupManga(customMangaInfo = null)
        backup.artist shouldBe "R"
        backup.author shouldBe "A"
        backup.description shouldBe "D"
        backup.genre shouldBe listOf("g")
        backup.status shouldBe 3
        backup.thumbnailUrl shouldBe "http://t"
        backup.viewer shouldBe (ReadingMode.MASK + 1) and ReadingMode.MASK
        backup.customTitle shouldBe null
    }

    @Test
    fun toBackupMangaWithNullGenre() {
        libraryManga().toBackupManga(customMangaInfo = null).genre shouldBe emptyList()
    }
}
