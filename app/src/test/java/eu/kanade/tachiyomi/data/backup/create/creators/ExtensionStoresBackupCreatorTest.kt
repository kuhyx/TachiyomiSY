package eu.kanade.tachiyomi.data.backup.create.creators

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.domain.extension.interactor.GetExtensionStores
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

internal fun testExtensionStore(name: String = "Store"): ExtensionStore = ExtensionStore(
    indexUrl = "https://index/$name",
    name = name,
    badgeLabel = "badge",
    signingKey = "key",
    contact = ExtensionStore.Contact(website = "https://site", discord = null),
    isLegacy = false,
    extensionListUrl = null,
)

internal class ExtensionStoresBackupCreatorTest {

    private val getExtensionStores = mockk<GetExtensionStores>()

    @BeforeEach
    fun setUp() {
        startKoin { modules(module { single { getExtensionStores } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun mapsEveryStore() = runTest {
        coEvery { getExtensionStores.get() } returns listOf(testExtensionStore(), testExtensionStore(name = "Other"))
        val creator = ExtensionStoresBackupCreator(getExtensionStores = getExtensionStores)
        creator().map { it.name } shouldBe listOf("Store", "Other")
    }

    @Test
    fun injectsInteractorByDefault() = runTest {
        coEvery { getExtensionStores.get() } returns listOf(testExtensionStore(name = "Injected"))
        ExtensionStoresBackupCreator()().single().name shouldBe "Injected"
    }

    @Test
    fun emptyWhenNoStores() = runTest {
        coEvery { getExtensionStores.get() } returns emptyList()
        ExtensionStoresBackupCreator(getExtensionStores = getExtensionStores)() shouldBe emptyList()
    }
}
