package mihon.data.extension.service

import eu.kanade.tachiyomi.network.HttpException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import mihon.data.extension.LEGACY_INDEX_JSON
import mihon.data.extension.StoreServer
import mihon.data.extension.TestLogcat
import mihon.data.extension.cancellingService
import mihon.data.extension.gzipped
import mihon.data.extension.legacyRepoJson
import mihon.data.extension.model.NetworkExtensionStore
import mihon.data.extension.model.domainStore
import mihon.data.extension.model.networkStore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.cancellation.CancellationException

internal class ExtensionStoreServiceFetchTest {
    private val server = StoreServer()
    private val service = server.service

    @BeforeEach
    fun setUp() {
        TestLogcat.start()
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun protobufStore() = runTest {
        val url = server.url("/index.pb")
        server.enqueueProto(networkStore(discord = null))

        service.fetch(url).getOrThrow() shouldBe domainStore(indexUrl = url)
    }

    @Test
    fun jsonStore() = runTest {
        val url = server.url("/index.json")
        server.enqueueJson(networkStore(discord = null, extensionListUrl = "l"))

        service.fetch(url).getOrThrow() shouldBe domainStore(indexUrl = url, extensionListUrl = "l")
    }

    @Test
    fun gzippedJsonStore() = runTest {
        val url = server.url("/index.json")
        val body = server.json.encodeToString(NetworkExtensionStore.serializer(), networkStore(discord = null))
        server.enqueue(gzipped(body))

        service.fetch(url).getOrThrow() shouldBe domainStore(indexUrl = url)
    }

    @Test
    fun legacyRepoWithoutPointer() = runTest {
        val url = server.url("/repo.json")
        server.enqueue(legacyRepoJson())

        val store = service.fetch(url).getOrThrow()

        store.indexUrl shouldBe url
        store.isLegacy shouldBe true
        store.badgeLabel shouldBe "LEG"
    }

    @Test
    fun legacyRepoFollowsPointer() = runTest {
        val v2Url = server.url("/v2/index.json")
        server.enqueue(legacyRepoJson(indexV2 = v2Url))
        server.enqueueJson(networkStore(discord = null))

        service.fetch(server.url("/repo.json")).getOrThrow() shouldBe domainStore(indexUrl = v2Url)

        server.takePath() shouldBe "/repo.json"
        server.takePath() shouldBe "/v2/index.json"
    }

    @Test
    fun legacyArrayReadsSiblingRepo() = runTest {
        server.enqueue(LEGACY_INDEX_JSON)
        server.enqueue(legacyRepoJson())

        val store = service.fetch(server.url("/legacy/index.min.json")).getOrThrow()

        store.indexUrl shouldBe server.url("/legacy/repo.json")
        store.isLegacy shouldBe true
        server.takePath() shouldBe "/legacy/index.min.json"
        server.takePath() shouldBe "/legacy/repo.json"
    }

    @Test
    fun legacyArrayAtOtherUrlFails() = runTest {
        val url = server.url("/legacy/index.json")
        server.enqueue(LEGACY_INDEX_JSON)

        val error = service.fetch(url).exceptionOrNull()

        error.shouldBeInstanceOf<IllegalArgumentException>()
        error.message shouldBe "Provided legacy store url is not valid"
        TestLogcat.messages.single() shouldContain "Failed to add extension store '$url'"
    }

    @Test
    fun httpErrorFails() = runTest {
        server.enqueue("nope", code = 500)

        service.fetch(server.url("/index.json")).exceptionOrNull().shouldBeInstanceOf<HttpException>()
    }

    @Test
    fun tinyBodyFails() = runTest {
        val url = server.url("/index.json")
        server.enqueue("x")

        service.fetch(url).isFailure shouldBe true

        TestLogcat.messages.single() shouldContain "Failed to add extension store '$url'"
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        shouldThrow<CancellationException> { cancellingService(server.json).fetch("https://x.example/index.json") }
    }
}
