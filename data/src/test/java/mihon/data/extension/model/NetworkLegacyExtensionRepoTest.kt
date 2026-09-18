package mihon.data.extension.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import mihon.data.extension.assertDataClass
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.Test

internal class NetworkLegacyExtensionRepoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val meta = NetworkLegacyExtensionRepo.Meta(
        name = "Legacy",
        shortName = "LEG",
        website = "https://legacy.example",
        signingKeyFingerprint = "FFFF",
    )

    @Test
    fun storeUsesShortName() {
        NetworkLegacyExtensionRepo(indexV2 = null, meta = meta).toExtensionStore("u") shouldBe ExtensionStore(
            indexUrl = "u",
            name = "Legacy",
            badgeLabel = "LEG",
            signingKey = "FFFF",
            contact = ExtensionStore.Contact(website = "https://legacy.example", discord = null),
            isLegacy = true,
            extensionListUrl = null,
        )
    }

    @Test
    fun storeFallsBackToName() {
        val repo = NetworkLegacyExtensionRepo(indexV2 = "v2", meta = meta.copy(shortName = null))

        repo.toExtensionStore("u").badgeLabel shouldBe "Legacy"
    }

    @Test
    fun jsonRoundTrips() {
        val full = NetworkLegacyExtensionRepo(indexV2 = "https://v2.example", meta = meta)
        val minimal = """{"meta":{"name":"Legacy","website":"https://legacy.example","signingKeyFingerprint":"FFFF"}}"""

        val encoded = json.encodeToString(NetworkLegacyExtensionRepo.serializer(), full)

        encoded.contains("index_v2") shouldBe true
        json.decodeFromString(NetworkLegacyExtensionRepo.serializer(), encoded) shouldBe full
        json.decodeFromString(NetworkLegacyExtensionRepo.serializer(), minimal) shouldBe
            NetworkLegacyExtensionRepo(indexV2 = null, meta = meta.copy(shortName = null))
    }

    @Test
    fun dataClassContracts() {
        val repo = NetworkLegacyExtensionRepo(indexV2 = "v2", meta = meta)

        assertDataClass(
            value = repo,
            equal = repo.copy(),
            different = listOf(repo.copy(indexV2 = null), repo.copy(meta = meta.copy(name = "x"))),
        )
        assertDataClass(
            value = meta,
            equal = meta.copy(),
            different = listOf(
                meta.copy(name = "x"),
                meta.copy(shortName = null),
                meta.copy(website = "x"),
                meta.copy(signingKeyFingerprint = "x"),
            ),
        )
    }
}
