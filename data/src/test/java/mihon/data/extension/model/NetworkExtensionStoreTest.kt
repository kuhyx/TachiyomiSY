package mihon.data.extension.model

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.data.extension.assertDataClass
import mihon.data.extension.model.NetworkExtensionStore.ContentWarning
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.Test

internal class NetworkExtensionStoreTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun toExtensionStoreMapsFields() {
        val store = networkStore(extensionListUrl = "https://store.example/list.json")

        store.toExtensionStore(INDEX_URL) shouldBe ExtensionStore(
            indexUrl = INDEX_URL,
            name = "Demo Store",
            badgeLabel = "DEMO",
            signingKey = "ABCDEF",
            contact = ExtensionStore.Contact(website = "https://store.example", discord = "https://discord.gg/demo"),
            isLegacy = false,
            extensionListUrl = "https://store.example/list.json",
        )
    }

    @Test
    fun toExtensionStoreKeepsNulls() {
        val store = networkStore(extensionList = null, discord = null).toExtensionStore(INDEX_URL)

        store.contact.discord shouldBe null
        store.extensionListUrl shouldBe null
        store.isLegacy shouldBe false
    }

    @Test
    fun jsonFullRoundTrip() {
        val store = networkStore(extensionListUrl = "https://store.example/list.json")

        val encoded = json.encodeToString(NetworkExtensionStore.serializer(), store)

        json.decodeFromString(NetworkExtensionStore.serializer(), encoded) shouldBe store
    }

    @Test
    fun jsonMinimalDecodes() {
        val minimal = """{"name":"Demo Store","badgeLabel":"DEMO","signingKey":"ABCDEF",""" +
            """"contact":{"website":"https://store.example"},"unknown":1}"""

        json.decodeFromString(NetworkExtensionStore.serializer(), minimal) shouldBe
            networkStore(extensionList = null, discord = null)
    }

    @Test
    fun protobufRoundTrip() {
        val store = networkStore(extensionListUrl = "https://store.example/list.json")

        val bytes = ProtoBuf.encodeToByteArray(NetworkExtensionStore.serializer(), store)

        ProtoBuf.decodeFromByteArray(NetworkExtensionStore.serializer(), bytes) shouldBe store
    }

    @Test
    fun contentWarningJsonNames() {
        val long = """{"name":"Demo","packageName":"p","resources":{"apkUrl":"a","iconUrl":"i"},""" +
            """"extensionLib":"1.5","versionCode":7,"versionName":"1.4.7",""" +
            """"contentWarning":"CONTENT_WARNING_NSFW","sources":[]}"""
        val short = long.replace("CONTENT_WARNING_NSFW", "MIXED")

        json.decodeFromString(NetworkExtensionStore.Extension.serializer(), long).contentWarning shouldBe
            ContentWarning.NSFW
        json.decodeFromString(NetworkExtensionStore.Extension.serializer(), short).contentWarning shouldBe
            ContentWarning.MIXED
    }

    @Test
    fun sourceDefaultsAreEmpty() {
        val source = networkSource()

        source.homeUrl shouldBe ""
        source.mirrorUrls.shouldBeEmpty()
        source.message shouldBe null
        json.decodeFromString(
            NetworkExtensionStore.Source.serializer(),
            """{"id":1,"name":"Source 1","language":"en"}""",
        ) shouldBe source
    }

    @Test
    fun sourceFullFieldsRoundTrip() {
        val source = NetworkExtensionStore.Source(
            id = 2L,
            name = "Full",
            language = "fr",
            homeUrl = "https://full.example",
            mirrorUrls = listOf("https://mirror.example"),
            message = "notice",
        )

        val encoded = json.encodeToString(NetworkExtensionStore.Source.serializer(), source)

        json.decodeFromString(NetworkExtensionStore.Source.serializer(), encoded) shouldBe source
        source.copy(message = null).message shouldBe null
    }

    @Test
    fun storeDataClassContract() {
        val store = networkStore()

        assertDataClass(
            value = store,
            equal = store.copy(),
            different = listOf(
                store.copy(name = "Other"),
                store.copy(badgeLabel = "OTHER"),
                store.copy(signingKey = "000000"),
                store.copy(contact = NetworkExtensionStore.Contact(website = "https://other.example", discord = null)),
                store.copy(extensionList = null),
                store.copy(extensionListUrl = "https://store.example/list.json"),
            ),
        )
        store.contact.hashCode() shouldBe store.contact.copy().hashCode()
    }

    @Test
    fun nestedDataClassContracts() {
        val contact = NetworkExtensionStore.Contact(website = "https://store.example", discord = null)
        val resources = NetworkExtensionStore.Resources(apkUrl = "a", iconUrl = "i")
        val extension = networkExtension()
        val list = NetworkExtensionStore.ExtensionList(listOf(extension))

        assertDataClass(
            value = contact,
            equal = contact.copy(),
            different = listOf(contact.copy(website = "x"), contact.copy(discord = "d")),
        )
        assertDataClass(
            value = resources,
            equal = resources.copy(),
            different = listOf(resources.copy(apkUrl = "b"), resources.copy(iconUrl = "j")),
        )
        assertDataClass(
            value = list,
            equal = list.copy(),
            different = listOf(list.copy(extensions = emptyList())),
        )
        assertDataClass(
            value = extension,
            equal = extension.copy(),
            different = listOf(
                extension.copy(name = "x"),
                extension.copy(packageName = "x"),
                extension.copy(resources = resources),
                extension.copy(extensionLib = "1.4"),
                extension.copy(versionCode = 8L),
                extension.copy(versionName = "x"),
                extension.copy(contentWarning = ContentWarning.NSFW),
                extension.copy(sources = emptyList()),
            ),
        )
    }
}
