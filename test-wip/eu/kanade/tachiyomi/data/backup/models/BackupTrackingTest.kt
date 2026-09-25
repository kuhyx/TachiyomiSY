package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test

private val syncId1LibraryId2LegacyMediaId5 = byteArrayOf(0x08, 0x01, 0x10, 0x02, 0x18, 0x05)

internal class BackupTrackingTest {

    @Test
    fun defaults() {
        val tracking = BackupTracking(syncId = 1, libraryId = 2L)
        tracking.trackingUrl shouldBe ""
        tracking.title shouldBe ""
        tracking.lastChapterRead shouldBe 0F
        tracking.totalChapters shouldBe 0
        tracking.score shouldBe 0F
        tracking.status shouldBe 0
        tracking.startedReadingDate shouldBe 0L
        tracking.finishedReadingDate shouldBe 0L
        tracking.private shouldBe false
        tracking.mediaId shouldBe 0L
    }

    @Test
    fun dataClassMembers() {
        val tracking = BackupTracking(syncId = 1, libraryId = 2L, title = "T")
        tracking shouldBe BackupTracking(syncId = 1, libraryId = 2L, title = "T")
        tracking shouldNotBe BackupTracking(syncId = 1, libraryId = 2L, title = "U")
        tracking.hashCode() shouldBe BackupTracking(syncId = 1, libraryId = 2L, title = "T").hashCode()
        tracking.toString() shouldNotBe ""
        tracking.copy(title = "U").title shouldBe "U"
    }

    @Test
    fun getTrackImplUsesMediaId() {
        val tracking = BackupTracking(
            syncId = 3,
            libraryId = 4L,
            trackingUrl = "http://u",
            title = "T",
            lastChapterRead = 5.5F,
            totalChapters = 6,
            score = 7.5F,
            status = 8,
            startedReadingDate = 9L,
            finishedReadingDate = 10L,
            private = true,
            mediaId = 11L,
        )
        val track = tracking.getTrackImpl()
        track.id shouldBe -1L
        track.mangaId shouldBe -1L
        track.trackerId shouldBe 3L
        track.remoteId shouldBe 11L
        track.libraryId shouldBe 4L
        track.title shouldBe "T"
        track.lastChapterRead shouldBe 5.5
        track.totalChapters shouldBe 6L
        track.score shouldBe 7.5
        track.status shouldBe 8L
        track.startDate shouldBe 9L
        track.finishDate shouldBe 10L
        track.remoteUrl shouldBe "http://u"
        track.private shouldBe true
    }

    @Test
    fun getTrackImplPrefersLegacyId() {
        val tracking = ProtoBuf.decodeFromByteArray(BackupTracking.serializer(), syncId1LibraryId2LegacyMediaId5)
        tracking.getTrackImpl().remoteId shouldBe 5L
        tracking.getTrackImpl().trackerId shouldBe 1L
        tracking.getTrackImpl().libraryId shouldBe 2L
    }

    @Test
    fun mapperNarrowsNumbers() {
        val tracking = backupTrackMapper(
            1L,
            2L,
            3L,
            4L,
            5L,
            "T",
            6.5,
            7L,
            8L,
            9.5,
            "http://u",
            10L,
            11L,
            true,
        )
        tracking.syncId shouldBe 3
        tracking.mediaId shouldBe 4L
        tracking.libraryId shouldBe 5L
        tracking.title shouldBe "T"
        tracking.lastChapterRead shouldBe 6.5F
        tracking.totalChapters shouldBe 7
        tracking.status shouldBe 8
        tracking.score shouldBe 9.5F
        tracking.trackingUrl shouldBe "http://u"
        tracking.startedReadingDate shouldBe 10L
        tracking.finishedReadingDate shouldBe 11L
        tracking.private shouldBe true
    }

    @Test
    fun mapperDefaultsNullLibraryId() {
        val tracking = backupTrackMapper(
            1L,
            2L,
            3L,
            4L,
            null,
            "T",
            0.0,
            0L,
            0L,
            0.0,
            "",
            0L,
            0L,
            false,
        )
        tracking.libraryId shouldBe 0L
    }

    @Test
    fun protoRoundTrip() {
        val tracking = BackupTracking(syncId = 1, libraryId = 2L, title = "T", mediaId = 3L)
        val bytes = ProtoBuf.encodeToByteArray(BackupTracking.serializer(), tracking)
        ProtoBuf.decodeFromByteArray(BackupTracking.serializer(), bytes) shouldBe tracking
    }
}
