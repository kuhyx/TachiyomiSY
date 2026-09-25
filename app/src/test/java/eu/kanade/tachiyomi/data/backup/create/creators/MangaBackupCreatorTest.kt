package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.chapterRow
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.libraryManga
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import io.kotest.matchers.collections.shouldContainExactly
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
import tachiyomi.domain.history.model.History
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.source.service.SourceManager
import java.util.Date

internal class MangaBackupCreatorTest {

    private val db = FakeBackupDatabase()
    private val getCategories = mockk<GetCategories>()
    private val getHistory = mockk<GetHistory>()
    private val sourceManager = mockk<SourceManager>()
    private val getCustomMangaInfo = mockk<GetCustomMangaInfo>()
    private val getFlatMetadataById = mockk<GetFlatMetadataById>()

    private val allOff = BackupOptions(
        chapters = false,
        categories = false,
        tracking = false,
        history = false,
        customInfo = false,
    )

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

    @Test
    fun everySectionDisabled() = runTest {
        db.withExcludedScanlators(listOf("Scan"))
        val backup = creator()(listOf(libraryManga()), allOff).single()
        backup.excludedScanlators shouldContainExactly listOf("Scan")
        backup.chapters shouldBe emptyList()
        backup.categories shouldBe emptyList()
        backup.tracking shouldBe emptyList()
        backup.history shouldBe emptyList()
        backup.customTitle shouldBe null
    }

    @Test
    fun injectsCollaboratorsByDefault() = runTest {
        MangaBackupCreator()(listOf(libraryManga()), allOff).single().url shouldBe "/m"
    }

    @Test
    fun chaptersWhenNotEmpty() = runTest {
        db.withBackupChapters(listOf(BackupChapter(url = "/c", name = "C")))
        val backup = creator()(listOf(libraryManga()), BackupOptions(customInfo = false)).single()
        backup.chapters.single().name shouldBe "C"
    }

    @Test
    fun chaptersKeptEmptyWhenNoRows() = runTest {
        db.withBackupChapters(emptyList())
        creator()(listOf(libraryManga()), allOff.copy(chapters = true)).single().chapters shouldBe emptyList()
    }

    @Test
    fun categoriesWhenNotEmpty() = runTest {
        coEvery { getCategories.await(any()) } returns listOf(testCategory(id = 2L, order = 5L))
        val backup = creator()(listOf(libraryManga()), allOff.copy(categories = true)).single()
        backup.categories shouldContainExactly listOf(5L)
    }

    @Test
    fun categoriesEmptyWhenNone() = runTest {
        creator()(listOf(libraryManga()), allOff.copy(categories = true)).single().categories shouldBe emptyList()
    }

    @Test
    fun trackingWhenNotEmpty() = runTest {
        db.withTracks(listOf(BackupTracking(syncId = 1, libraryId = 2L)))
        val backup = creator()(listOf(libraryManga()), allOff.copy(tracking = true)).single()
        backup.tracking.single().syncId shouldBe 1
    }

    @Test
    fun trackingEmptyWhenNone() = runTest {
        creator()(listOf(libraryManga()), allOff.copy(tracking = true)).single().tracking shouldBe emptyList()
    }

    @Test
    fun historyWithAndWithoutReadAt() = runTest {
        db.withChapterRow(chapterRow(url = "/c1"))
        coEvery { getHistory.await(any()) } returns listOf(
            History(id = 1L, chapterId = 1L, readAt = Date(50L), readDuration = 7L),
            History(id = 2L, chapterId = 1L, readAt = null, readDuration = 0L),
        )
        val history = creator()(listOf(libraryManga()), allOff.copy(history = true)).single().history
        history.map { it.lastRead } shouldContainExactly listOf(50L, 0L)
        history.first().readDuration shouldBe 7L
    }

    @Test
    fun historyEmptyWhenNone() = runTest {
        creator()(listOf(libraryManga()), allOff.copy(history = true)).single().history shouldBe emptyList()
    }
}
