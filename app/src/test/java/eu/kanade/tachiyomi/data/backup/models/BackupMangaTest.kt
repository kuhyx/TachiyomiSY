package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.core.common.extensions.JsonObjectEmptyBytes
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo

internal class BackupMangaTest {

    @BeforeEach
    fun setUp() {
        val customInfo = mockk<GetCustomMangaInfo>()
        every { customInfo.get(any()) } returns null
        startKoin { modules(module { single { customInfo } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun defaults() {
        val manga = BackupManga(source = 1L, url = "/u")
        manga.title shouldBe ""
        manga.artist shouldBe null
        manga.author shouldBe null
        manga.description shouldBe null
        manga.genre shouldBe emptyList()
        manga.status shouldBe 0
        manga.thumbnailUrl shouldBe null
        manga.dateAdded shouldBe 0L
        manga.viewer shouldBe 0
        manga.chapters shouldBe emptyList()
        manga.categories shouldBe emptyList()
        manga.tracking shouldBe emptyList()
        manga.favorite shouldBe true
    }

    @Test
    fun moreDefaults() {
        val manga = BackupManga(source = 1L, url = "/u")
        manga.chapterFlags shouldBe 0
        manga.viewerFlags shouldBe null
        manga.history shouldBe emptyList()
        manga.updateStrategy shouldBe UpdateStrategy.ALWAYS_UPDATE
        manga.lastModifiedAt shouldBe 0L
        manga.favoriteModifiedAt shouldBe null
        manga.excludedScanlators shouldBe emptyList()
        manga.version shouldBe 0L
        manga.notes shouldBe ""
        manga.initialized shouldBe false
        manga.memo shouldBe JsonObjectEmptyBytes
    }

    @Test
    fun syAndJ2kDefaults() {
        val manga = BackupManga(source = 1L, url = "/u")
        manga.mergedMangaReferences shouldBe emptyList()
        manga.flatMetadata shouldBe null
        manga.customStatus shouldBe 0
        manga.customThumbnailUrl shouldBe null
        manga.customTitle shouldBe null
        manga.customArtist shouldBe null
        manga.customAuthor shouldBe null
        manga.customDescription shouldBe null
        manga.customGenre shouldBe null
    }

    @Test
    fun propertiesAreMutable() {
        val manga = BackupManga(source = 1L, url = "/u")
        manga.source = 2L
        manga.url = "/v"
        manga.title = "T"
        manga.customTitle = "C"
        manga.customGenre = listOf("g")
        manga.flatMetadata = backupFlatMetadata()
        manga.source shouldBe 2L
        manga.url shouldBe "/v"
        manga.title shouldBe "T"
        manga.customTitle shouldBe "C"
        manga.customGenre shouldBe listOf("g")
        manga.flatMetadata shouldBe backupFlatMetadata()
    }

    @Test
    fun getMangaImplFallsBackToViewer() {
        val manga = BackupManga(source = 1L, url = "/u", viewer = 3)
        manga.getMangaImpl().viewerFlags shouldBe 3L
    }

    @Test
    fun getMangaImplPrefersViewerFlags() {
        val memo = JsonObject(mapOf("a" to JsonPrimitive(1)))
        val manga = BackupManga(
            source = 4L,
            url = "/u",
            title = "T",
            artist = "A",
            author = "B",
            description = "D",
            genre = listOf("g"),
            status = 2,
            thumbnailUrl = "http://c",
            dateAdded = 5L,
            viewer = 3,
            viewerFlags = 7,
            chapterFlags = 8,
            updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE,
            lastModifiedAt = 9L,
            favoriteModifiedAt = 10L,
            version = 11L,
            notes = "n",
            initialized = true,
            memo = MemoColumnAdapter.encode(memo),
        )
        val impl = manga.getMangaImpl()
        impl.viewerFlags shouldBe 7L
        impl.url shouldBe "/u"
        impl.ogTitle shouldBe "T"
        impl.ogArtist shouldBe "A"
        impl.ogAuthor shouldBe "B"
        impl.ogDescription shouldBe "D"
        impl.ogGenre shouldBe listOf("g")
        impl.ogStatus shouldBe 2L
        impl.ogThumbnailUrl shouldBe "http://c"
        impl.favorite shouldBe true
        impl.source shouldBe 4L
        impl.dateAdded shouldBe 5L
        impl.chapterFlags shouldBe 8L
        impl.updateStrategy shouldBe UpdateStrategy.ONLY_FETCH_ONCE
        impl.lastModifiedAt shouldBe 9L
        impl.favoriteModifiedAt shouldBe 10L
        impl.version shouldBe 11L
        impl.notes shouldBe "n"
        impl.initialized shouldBe true
        impl.memo shouldBe memo
    }

    @Test
    fun protoRoundTrip() {
        val manga = BackupManga(
            source = 1L,
            url = "/u",
            title = "T",
            chapters = listOf(BackupChapter(url = "/c", name = "C")),
            categories = listOf(2L),
            tracking = listOf(BackupTracking(syncId = 1, libraryId = 2L)),
            history = listOf(BackupHistory(url = "/c", lastRead = 3L)),
            mergedMangaReferences = listOf(backupMergedMangaReference()),
            flatMetadata = backupFlatMetadata(),
        )
        val bytes = ProtoBuf.encodeToByteArray(BackupManga.serializer(), manga)
        val decoded = ProtoBuf.decodeFromByteArray(BackupManga.serializer(), bytes)
        decoded.title shouldBe "T"
        decoded.chapters.single().name shouldBe "C"
        decoded.categories shouldBe listOf(2L)
        decoded.tracking.single().libraryId shouldBe 2L
        decoded.history.single().lastRead shouldBe 3L
        decoded.mergedMangaReferences.single() shouldBe backupMergedMangaReference()
        decoded.flatMetadata shouldBe backupFlatMetadata()
    }

    @Test
    fun protoMinimalPayload() {
        val bytes = ProtoBuf.encodeToByteArray(BackupManga.serializer(), BackupManga(source = 1L, url = "/u"))
        val decoded = ProtoBuf.decodeFromByteArray(BackupManga.serializer(), bytes)
        decoded.source shouldBe 1L
        decoded.url shouldBe "/u"
        decoded.favorite shouldBe true
        decoded.memo shouldBe JsonObjectEmptyBytes
    }
}
