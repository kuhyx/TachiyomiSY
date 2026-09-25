package eu.kanade.tachiyomi.data.sync.service

import eu.kanade.tachiyomi.data.backup.models.Backup
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.verify
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import mockwebserver3.MockResponse
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class SyncYomiSyncServiceTest {

    private val harness = SyncYomiHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    @Test
    fun firstSyncOverwritesRemote() = runTest {
        harness.enqueue(code = 404)
        harness.enqueue(code = 200, etag = "e1")
        val data = syncData("a", "b")
        harness.service.doSync(data) shouldBeSameInstanceAs data.backup
        harness.preferences.lastSyncEtag.get() shouldBe "e1"
        harness.preferences.lastSyncEntryCount.get() shouldBe 2
        harness.eventNames() shouldBe listOf("SYNC_STARTED", "SYNC_SUCCESS")
        harness.logged shouldContain "Try overwrite remote data with ETag()"
        val push = harness.requests[1]
        push.method shouldBe "PUT"
        push.headers["If-Match"].shouldBeNull()
        push.headers[X_API_TOKEN] shouldBe "key"
    }

    @Test
    fun remoteDataIsMerged() = runTest {
        harness.enqueueBackup(Backup(backupManga = listOf(manga("remote"))), etag = "e1")
        harness.enqueue(code = 200, etag = "e2")
        val merged = harness.service.doSync(syncData("local"))
        merged?.backupManga?.map { it.url } shouldBe listOf("local", "remote")
        harness.requests[1].headers["If-Match"] shouldBe "e1"
        harness.preferences.lastSyncEtag.get() shouldBe "e2"
        harness.logged shouldContain "Try update remote data with ETag(e1)"
    }

    @Test
    fun conflictReportsFailure() = runTest {
        harness.enqueue(code = 404)
        harness.enqueue(code = 412)
        harness.service.doSync(syncData("a"))?.backupManga?.size shouldBe 1
        harness.preferences.lastSyncEntryCount.get() shouldBe 0
        harness.eventNames() shouldBe listOf("SYNC_STARTED", "SYNC_FAILED")
        harness.events.last() shouldContain "Failed to push sync data"
    }

    @Test
    fun nothingToPushStillSucceeds() = runTest {
        harness.enqueue(code = 404)
        harness.service.doSync(SyncData()).shouldBeNull()
        harness.preferences.lastSyncEntryCount.get() shouldBe 0
        harness.eventNames() shouldBe listOf("SYNC_STARTED", "SYNC_SUCCESS")
    }

    @Test
    fun errorsAreReportedAndSwallowed() = runTest {
        harness.enqueue(code = 500, body = Buffer().writeUtf8("boom"))
        harness.service.doSync(syncData("a")).shouldBeNull()
        verify { harness.notifier.showSyncError("Failed to download sync data: boom") }
        harness.eventNames() shouldBe listOf("SYNC_STARTED", "SYNC_ERROR")
        harness.logged shouldContain "Error syncing: Failed to download sync data: boom"
    }

    @Test
    fun collapseIsAnError() = runTest {
        harness.preferences.lastSyncEntryCount.set(10)
        harness.enqueue(code = 404)
        harness.service.doSync(syncData("a")).shouldBeNull()
        harness.requests.size shouldBe 1
        harness.eventNames().last() shouldBe "SYNC_ERROR"
    }

    @Test
    fun cancellationIsReported() = runTest {
        harness.content.addLast(MockResponse.Builder().headersDelay(5, TimeUnit.SECONDS).build())
        shouldThrow<TimeoutCancellationException> {
            withTimeout(1_000) { harness.service.doSync(syncData("a")) }
        }
        harness.eventNames() shouldBe listOf("SYNC_STARTED", "SYNC_CANCELLED")
    }

    @Test
    fun eventFieldsDefaultToNull() {
        val type = Class.forName("eu.kanade.tachiyomi.data.sync.service.SyncYomiSyncService\$SyncEvent")
        val event = Json.decodeFromString(serializer(type), """{"event":"SYNC_STARTED"}""")
        event.toString() shouldBe "SyncEvent(event=SYNC_STARTED, deviceName=null, message=null)"
    }

    @Test
    fun unreachableEventsAreLogged() = runTest {
        harness.preferences.clientHost.set("not a url")
        harness.service.doSync(syncData("a")).shouldBeNull()
        harness.logged.count { it.startsWith("Failed to report sync event") } shouldBe 2
    }
}
