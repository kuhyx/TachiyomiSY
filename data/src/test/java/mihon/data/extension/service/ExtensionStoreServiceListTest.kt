package mihon.data.extension.service

import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.data.extension.LEGACY_INDEX_JSON
import mihon.data.extension.StoreServer
import mihon.data.extension.cancellingService
import mihon.data.extension.gzipped
import mihon.data.extension.model.NetworkExtensionStore
import mihon.data.extension.model.domainStore
import mihon.data.extension.model.networkExtension
import mihon.data.extension.model.networkStore
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.cancellation.CancellationException

internal class ExtensionStoreServiceListTest {
    private val server = StoreServer()
    private val service = server.service
    private val list = NetworkExtensionStore.ExtensionList(listOf(networkExtension(packageName = "listed")))

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun listUrlJson() = runTest {
        val store = domainStore(extensionListUrl = server.url("/list.json"))
        server.enqueue(server.json.encodeToString(NetworkExtensionStore.ExtensionList.serializer(), list))

        val extensions = service.getExtensions(store).getOrThrow()

        extensions.map { it.pkgName } shouldBe listOf("listed")
        extensions.single().store shouldBe store
        server.takePath() shouldBe "/list.json"
    }

    @Test
    fun listUrlProtobuf() = runTest {
        val store = domainStore(extensionListUrl = server.url("/list.pb"))
        val bytes = ProtoBuf.encodeToByteArray(NetworkExtensionStore.ExtensionList.serializer(), list)
        server.enqueue(Buffer().write(bytes))

        service.getExtensions(store).getOrThrow().map { it.pkgName } shouldBe listOf("listed")
    }

    @Test
    fun listUrlGzipped() = runTest {
        val store = domainStore(extensionListUrl = server.url("/list.json"))
        server.enqueue(gzipped(server.json.encodeToString(NetworkExtensionStore.ExtensionList.serializer(), list)))

        service.getExtensions(store).getOrThrow().map { it.pkgName } shouldBe listOf("listed")
    }

    @Test
    fun embeddedJsonStore() = runTest {
        val store = domainStore(indexUrl = server.url("/index.json"))
        server.enqueueJson(networkStore(extensionList = list))

        service.getExtensions(store).getOrThrow().map { it.pkgName } shouldBe listOf("listed")
        server.takePath() shouldBe "/index.json"
    }

    @Test
    fun embeddedProtobufStore() = runTest {
        val store = domainStore(indexUrl = server.url("/index.pb"))
        server.enqueueProto(networkStore(extensionList = list))

        service.getExtensions(store).getOrThrow().map { it.pkgName } shouldBe listOf("listed")
    }

    @Test
    fun embeddedListMissingFails() = runTest {
        val store = domainStore(indexUrl = server.url("/index.json"))
        server.enqueueJson(networkStore(extensionList = null))

        service.getExtensions(store).exceptionOrNull().shouldBeInstanceOf<NullPointerException>()
    }

    @Test
    fun legacyStoreReadsIndex() = runTest {
        val store = domainStore(indexUrl = server.url("/legacy/repo.json"), isLegacy = true)
        server.enqueue(LEGACY_INDEX_JSON)

        val extension = service.getExtensions(store).getOrThrow().single()

        extension.name shouldBe "Demo"
        extension.isNsfw shouldBe true
        extension.apkUrl shouldBe server.url("/legacy/apk/demo.apk")
        extension.store shouldBe store
        server.takePath() shouldBe "/legacy/index.min.json"
    }

    @Test
    fun httpErrorFails() = runTest {
        server.enqueue("nope", code = 404)

        val result = service.getExtensions(domainStore(extensionListUrl = server.url("/list.json")))

        result.exceptionOrNull().shouldBeInstanceOf<HttpException>()
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        shouldThrow<CancellationException> { cancellingService(server.json).getExtensions(domainStore()) }
    }
}
