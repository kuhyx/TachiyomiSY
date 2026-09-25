package eu.kanade.tachiyomi.data.sync.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.sync.SyncNotifier
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.protobuf.ProtoBuf
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okio.Buffer
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.Collections

/**
 * A SyncYomi server on [MockWebServer]: `/api/sync/event` records the reported events and answers
 * 200; `/api/sync/content` answers the queued [content] responses in order.
 */
internal class SyncYomiHarness {
    val server = MockWebServer()
    val preferences = SyncPreferences(FlowPreferenceStore())
    val notifier: SyncNotifier = mockk(relaxed = true)
    val context: Context = ApplicationProvider.getApplicationContext()
    val events: MutableList<String> = Collections.synchronizedList(mutableListOf())
    val requests: MutableList<RecordedRequest> = Collections.synchronizedList(mutableListOf())
    val content = ArrayDeque<MockResponse>()
    var logged = mutableListOf<String>()

    /** Built with the [ProtoBuf] from Koin, which the service defaults to. */
    lateinit var service: SyncYomiSyncService

    private val dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse =
            if (request.url.encodedPath == "/api/sync/event") {
                events += request.body?.utf8().orEmpty()
                MockResponse(code = 200)
            } else {
                requests += request
                synchronized(content) { content.removeFirst() }
            }
    }

    fun start() {
        logged = captureLogcat()
        startKoin { modules(module { single<ProtoBuf> { ProtoBuf } }) }
        server.dispatcher = dispatcher
        server.start()
        preferences.clientHost.set(server.url("/").toString().removeSuffix("/"))
        preferences.clientAPIKey.set("key")
        service = SyncYomiSyncService(context, Json, preferences, notifier)
    }

    fun stop() {
        server.close()
        stopKoin()
        releaseLogcat()
    }

    fun enqueue(code: Int, etag: String? = null, body: Buffer = Buffer()) {
        val builder = MockResponse.Builder().code(code).body(body)
        if (etag != null) builder.addHeader("ETag", etag)
        synchronized(content) { content.addLast(builder.build()) }
    }

    fun enqueueBackup(backup: Backup, etag: String) {
        enqueue(code = 200, etag = etag, body = Buffer().write(ProtoBuf.encodeToByteArray(Backup.serializer(), backup)))
    }

    /** The `event` field of every reported event, in order. */
    fun eventNames(): List<String> =
        events.map { Json.parseToJsonElement(it).jsonObject.getValue("event").jsonPrimitive.content }
}

internal fun syncData(vararg urls: String): SyncData =
    SyncData(deviceId = "device", backup = Backup(backupManga = urls.map { manga(it) }))
