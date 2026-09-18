package mihon.data.extension.repository

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mihon.data.extension.StoreServer
import mihon.data.extension.TestLogcat
import mihon.data.extension.model.domainStore
import mihon.data.extension.model.networkStore
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase

internal class ExtensionStoreRepositoryImplTest {
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
    fun insertFromPreferenceBareRow() = runTest {
        repository.insertFromPreference("https://pref.example/index.json", "Pref")

        repository.getAll() shouldBe listOf(
            ExtensionStore(
                indexUrl = "https://pref.example/index.json",
                name = "Pref",
                badgeLabel = "Pref",
                signingKey = "NO_SIGNING_KEY",
                contact = ExtensionStore.Contact(website = "https://pref.example/index.json", discord = null),
                isLegacy = false,
                extensionListUrl = null,
            ),
        )
    }

    @Test
    fun insertFetchesAndUpserts() = runTest {
        val url = server.url("/index.json")
        server.enqueueJson(networkStore(discord = null))

        val result = repository.insert(url)

        result.isSuccess shouldBe true
        repository.getAll() shouldBe listOf(domainStore(indexUrl = url))
    }

    @Test
    fun insertTwiceUpdatesTheRow() = runTest {
        val url = server.url("/index.json")
        server.enqueueJson(networkStore(discord = null))
        server.enqueueJson(networkStore(discord = "https://discord.gg/new"))

        repository.insert(url)
        repository.insert(url)

        repository.getAll().single().contact.discord shouldBe "https://discord.gg/new"
    }

    @Test
    fun insertReportsFetchFailure() = runTest {
        server.enqueue("nope", code = 500)

        val result = repository.insert(server.url("/index.json"))

        result.isFailure shouldBe true
        result.exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
        repository.getAll().shouldBeEmpty()
    }

    @Test
    fun getAllAsFlowEmitsRows() = runTest {
        repository.insertFromPreference("a", "A")
        repository.insertFromPreference("b", "B")

        repository.getAllAsFlow().first().map { it.name } shouldBe listOf("A", "B")
    }

    @Test
    fun getCountAsFlowCountsRows() = runTest {
        repository.getCountAsFlow().first() shouldBe 0L

        repository.insertFromPreference("a", "A")

        repository.getCountAsFlow().first() shouldBe 1L
    }

    @Test
    fun removeDeletesByUrl() = runTest {
        repository.insertFromPreference("a", "A")
        repository.insertFromPreference("b", "B")

        repository.remove("a")

        repository.getAll().map { it.indexUrl } shouldBe listOf("b")
    }

    @Test
    fun mapperKeepsEveryColumn() = runTest {
        val store = domainStore(extensionListUrl = "https://store.example/list.json", isLegacy = true)
            .copy(contact = ExtensionStore.Contact(website = "w", discord = "d"))
        database.extension_storeQueries.upsert(
            indexUrl = store.indexUrl,
            name = store.name,
            badgeLabel = store.badgeLabel,
            signingKey = store.signingKey,
            contactWebsite = "w",
            contactDiscord = "d",
            isLegacy = true,
            extensionListUrl = store.extensionListUrl,
        )

        repository.getAll() shouldBe listOf(store)
    }
}
