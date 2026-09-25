package exh.ui.metadata

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.MetadataSource
import exh.metadata.metadata.EHentaiSearchMetadata
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

private const val WAIT_MS = 10_000L

internal class MetadataViewScreenModelTest {
    private val getFlatMetadataById = mockk<GetFlatMetadataById>()
    private val getManga = mockk<GetManga>()
    private val sourceManager = mockk<SourceManager>()
    private val manga = Manga.create().copy(id = 5L, source = 8L)
    private val metadataSource = mockk<MetadataSource<EHentaiSearchMetadata, *>> {
        every { metaClass } returns EHentaiSearchMetadata::class
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        coEvery { getManga.await(5L) } returns manga
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun model() = MetadataViewScreenModel(
        mangaId = 5L,
        sourceId = 8L,
        getFlatMetadataById = getFlatMetadataById,
        sourceManager = sourceManager,
        getManga = getManga,
    )

    private fun MetadataViewScreenModel.settled(): MetadataViewState = runBlocking {
        withTimeout(WAIT_MS) {
            manga.first { it != null }
            state.first { it != MetadataViewState.Loading }
        }
    }

    @Test
    fun anUnknownSource() {
        every { sourceManager.get(8L) } returns null
        val model = model()
        model.settled() shouldBe MetadataViewState.SourceNotFound
        model.mangaId shouldBe 5L
        model.sourceId shouldBe 8L
        model.manga.value shouldBe manga
    }

    @Test
    fun aSourceWithoutMetadata() {
        every { sourceManager.get(8L) } returns mockk<Source>()
        model().settled() shouldBe MetadataViewState.SourceNotFound
    }

    @Test
    fun noStoredMetadata() {
        every { sourceManager.get(8L) } returns metadataSource
        coEvery { getFlatMetadataById.await(5L) } returns null
        model().settled() shouldBe MetadataViewState.MetadataNotFound
    }

    @Test
    fun storedMetadataIsRaised() {
        every { sourceManager.get(8L) } returns metadataSource
        val flat = EHentaiSearchMetadata().apply {
            mangaId = 5L
            gId = "123"
            gToken = "tok"
            title = "Main"
        }.flatten()
        coEvery { getFlatMetadataById.await(5L) } returns flat
        val success = model().settled().shouldBeInstanceOf<MetadataViewState.Success>()
        val raised = success.meta.shouldBeInstanceOf<EHentaiSearchMetadata>()
        raised.gId shouldBe "123"
        raised.title shouldBe "Main"
    }

    @Test
    fun collaboratorsComeFromInjekt() {
        every { sourceManager.get(8L) } returns null
        startKoin {
            modules(
                module {
                    single { getFlatMetadataById }
                    single { sourceManager }
                    single { getManga }
                },
            )
        }
        MetadataViewScreenModel(mangaId = 5L, sourceId = 8L).settled() shouldBe MetadataViewState.SourceNotFound
    }
}
