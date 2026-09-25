package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Test
import java.util.Date

internal class BackupHistoryTest {

    @Test
    fun defaultReadDuration() {
        val history = BackupHistory(url = "/a", lastRead = 5L)
        history.readDuration shouldBe 0L
        history.url shouldBe "/a"
        history.lastRead shouldBe 5L
    }

    @Test
    fun dataClassMembers() {
        val history = BackupHistory(url = "/a", lastRead = 5L, readDuration = 3L)
        history shouldBe BackupHistory(url = "/a", lastRead = 5L, readDuration = 3L)
        history shouldNotBe BackupHistory(url = "/b", lastRead = 5L, readDuration = 3L)
        history.hashCode() shouldBe BackupHistory(url = "/a", lastRead = 5L, readDuration = 3L).hashCode()
        history.toString() shouldNotBe ""
        history.copy(readDuration = 9L).readDuration shouldBe 9L
    }

    @Test
    fun getHistoryImplMapsFields() {
        val impl = BackupHistory(url = "/a", lastRead = 1_000L, readDuration = 42L).getHistoryImpl()
        impl.readAt shouldBe Date(1_000L)
        impl.readDuration shouldBe 42L
        impl.id shouldBe -1L
        impl.chapterId shouldBe -1L
    }

    @Test
    fun protoRoundTrip() {
        val history = BackupHistory(url = "/a", lastRead = 7L, readDuration = 8L)
        val bytes = ProtoBuf.encodeToByteArray(BackupHistory.serializer(), history)
        ProtoBuf.decodeFromByteArray(BackupHistory.serializer(), bytes) shouldBe history
    }

    @Test
    fun protoMinimalPayload() {
        val bytes = ProtoBuf.encodeToByteArray(BackupHistory.serializer(), BackupHistory(url = "/x", lastRead = 2L))
        val decoded = ProtoBuf.decodeFromByteArray(BackupHistory.serializer(), bytes)
        decoded.readDuration shouldBe 0L
        decoded.url shouldBe "/x"
    }
}
