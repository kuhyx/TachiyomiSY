package mihon.data.extension.repository

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.data.extension.StoreServer
import mihon.data.extension.TestLogcat
import mihon.data.extension.model.INDEX_URL
import mihon.data.extension.model.NetworkExtensionStore
import mihon.data.extension.model.networkExtension
import mihon.data.extension.model.networkStore
import mihon.data.extension.service.ExtensionStoreService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase

internal class ExtensionStoreRefreshTest {
    private val server = StoreServer()
    private val database = inMemoryDatabase()
    private val repository = ExtensionStoreRepositoryImpl(server.service, database)

    @BeforeEach
    fun setUp() {
        TestLogcat.start()
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun refreshAllUpdatesInPlace() = runTest {
        val url = server.url("/index.json")
        repository.insertFromPreference(url, "Old")
        server.enqueueJson(networkStore())

        repository.refreshAll()

        repository.getAll().map { it.name } shouldBe listOf("Demo Store")
        TestLogcat.messages.shouldBeEmpty()
    }

    @Test
    fun refreshAllFollowsRedirect() = runTest {
        val legacyUrl = server.url("/legacy/index.json")
        val v2Url = server.url("/v2/index.json")
        repository.insertFromPreference(legacyUrl, "Legacy")
        server.enqueue("""{"index_v2":"$v2Url","meta":{"name":"L","website":"w","signingKeyFingerprint":"f"}}""")
        server.enqueueJson(networkStore())

        repository.refreshAll()

        repository.getAll().map { it.indexUrl } shouldBe listOf(v2Url)
    }

    @Test
    fun refreshAllLogsFetchFailure() = runTest {
        val url = server.url("/index.json")
        repository.insertFromPreference(url, "Old")
        server.enqueue("nope", code = 404)

        repository.refreshAll()

        repository.getAll().map { it.name } shouldBe listOf("Old")
        TestLogcat.messages.last() shouldContain "Failed to refresh extension store 'Old ($url)'"
    }

    @Test
    fun refreshAllLogsThrownError() = runTest {
        val service = mockk<ExtensionStoreService>()
        coEvery { service.fetch(any()) } throws IllegalStateException("boom")
        val throwing = ExtensionStoreRepositoryImpl(service, database)
        throwing.insertFromPreference(INDEX_URL, "Old")

        throwing.refreshAll()

        throwing.getAll().map { it.name } shouldBe listOf("Old")
        TestLogcat.messages.single() shouldContain "boom"
    }

    @Test
    fun refreshAllWithoutStores() = runTest {
        repository.refreshAll()

        repository.getAll().shouldBeEmpty()
        TestLogcat.messages.shouldBeEmpty()
    }

    @Test
    fun fetchExtensionsFlattens() = runTest {
        repository.insertFromPreference(server.url("/a/index.json"), "A")
        repository.insertFromPreference(server.url("/b/index.json"), "B")
        server.enqueueJson(networkStore())
        server.enqueueJson(
            networkStore(
                extensionList = NetworkExtensionStore.ExtensionList(listOf(networkExtension(packageName = "other"))),
            ),
        )

        val extensions = repository.fetchExtensions()

        extensions.map { it.pkgName }.sorted() shouldBe listOf("eu.kanade.tachiyomi.extension.en.demo", "other")
        extensions.map { it.store.name }.toSet() shouldBe setOf("A", "B")
    }

    @Test
    fun fetchExtensionsSkipsFailure() = runTest {
        val url = server.url("/index.json")
        repository.insertFromPreference(url, "A")
        server.enqueue("nope", code = 500)

        repository.fetchExtensions().shouldBeEmpty()

        TestLogcat.messages.single() shouldContain "Failed to fetch extensions for store 'A ($url)'"
    }

    @Test
    fun fetchExtensionsSwallows() = runTest {
        val service = mockk<ExtensionStoreService>()
        coEvery { service.getExtensions(any()) } throws IllegalStateException("boom")
        val throwing = ExtensionStoreRepositoryImpl(service, database)
        throwing.insertFromPreference(INDEX_URL, "A")

        throwing.fetchExtensions().shouldBeEmpty()

        TestLogcat.messages.single() shouldContain "boom"
    }

    @Test
    fun fetchExtensionsWithoutStores() = runTest {
        repository.fetchExtensions().shouldBeEmpty()
    }
}
