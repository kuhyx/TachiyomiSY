package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.sync.service.SyncYomiSyncService.SyncYomiException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SyncYomiTransferTest {

    private val harness = SyncYomiHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun notModifiedKeepsTheEtag() = runTest {
        harness.preferences.lastSyncEtag.set("e0")
        harness.enqueue(code = 304)
        harness.service.pullSyncData() shouldBe Pair(null, "e0")
        harness.requests.single().headers["If-None-Match"] shouldBe "e0"
        harness.logged shouldContain "Remote server not modified"
    }

    @Test
    fun pullDecodesTheBackup() = runTest {
        harness.enqueueBackup(Backup(backupManga = listOf(manga("a"))), etag = "e1")
        val (data, etag) = harness.service.pullSyncData()
        etag shouldBe "e1"
        data?.backup?.backupManga?.single()?.url shouldBe "a"
        harness.requests.single().headers["If-None-Match"].shouldBeNull()
    }

    @Test
    fun pullNeedsAnEtag() = runTest {
        harness.enqueue(code = 200)
        shouldThrow<SyncYomiException> { harness.service.pullSyncData() }.message shouldBe "Missing ETag"
        harness.enqueue(code = 200, etag = "")
        shouldThrow<SyncYomiException> { harness.service.pullSyncData() }.message shouldBe "Missing ETag"
    }

    @Test
    fun badContentCountsAsNone() = runTest {
        harness.enqueue(code = 200, etag = "e1", body = Buffer().write(byteArrayOf(0x0A, 0x7F)))
        harness.service.pullSyncData() shouldBe Pair(null, "")
        harness.logged shouldContain "Bad content responsed from server"
    }

    @Test
    fun pushWithoutBackupIsANoOp() = runTest {
        harness.service.pushSyncData(SyncData(), eTag = "e1") shouldBe true
        harness.requests.size shouldBe 0
    }

    @Test
    fun pushOfAnEmptyBackupFails() = runTest {
        val empty = SyncData(backup = Backup(backupManga = emptyList()))
        shouldThrow<IllegalStateException> { harness.service.pushSyncData(empty, eTag = "") }.message shouldBe
            "No library entries to back up"
    }

    @Test
    fun pushNeedsAnEtagBack() = runTest {
        harness.enqueue(code = 200)
        shouldThrow<SyncYomiException> { harness.service.pushSyncData(syncData("a"), eTag = "") }
        harness.enqueue(code = 200, etag = "")
        shouldThrow<SyncYomiException> { harness.service.pushSyncData(syncData("a"), eTag = "") }
        harness.preferences.lastSyncEtag.get() shouldBe ""
    }

    @Test
    fun pushServerErrorIsShown() = runTest {
        harness.enqueue(code = 500, body = Buffer().writeUtf8("nope"))
        harness.service.pushSyncData(syncData("a"), eTag = "e1") shouldBe false
        verify { harness.notifier.showSyncError("Failed to upload sync data: nope") }
        harness.logged shouldContain "SyncError: nope"
        harness.requests.single().headers["Content-Type"] shouldBe "application/octet-stream"
    }

    @Test
    fun explicitProtoBufIsUsed() = runTest {
        val service = SyncYomiSyncService(
            context = harness.context,
            json = harness.service.json,
            syncPreferences = harness.preferences,
            notifier = harness.notifier,
            protoBuf = harness.service.protoBuf,
        )
        service.protoBuf shouldBe harness.service.protoBuf
        SyncYomiException("m").message shouldBe "m"
    }
}
