package eu.kanade.tachiyomi.data.backup.models

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.Test

internal class BackupExtensionStoreTest {

    @Test
    fun holdsEveryField() {
        val store = backupExtensionStore()
        store.indexUrl shouldBe "https://index"
        store.name shouldBe "Store"
        store.badgeLabel shouldBe "badge"
        store.signingKey shouldBe "key"
        store.contactWebsite shouldBe "https://site"
        store.contactDiscord shouldBe "discord"
        store.isLegacy shouldBe false
        store.extensionListUrl shouldBe "https://list"
    }

    @Test
    fun nullableFieldsAcceptNull() {
        val store = backupExtensionStore(
            badgeLabel = null,
            contactDiscord = null,
            isLegacy = null,
            extensionListUrl = null,
        )
        store.badgeLabel shouldBe null
        store.contactDiscord shouldBe null
        store.isLegacy shouldBe null
        store.extensionListUrl shouldBe null
    }

    @Test
    fun dataClassMembers() {
        val store = backupExtensionStore()
        store shouldBe backupExtensionStore()
        store shouldNotBe backupExtensionStore(badgeLabel = "other")
        store.hashCode() shouldBe backupExtensionStore().hashCode()
        store.toString() shouldNotBe ""
        store.copy(name = "N").name shouldBe "N"
    }

    @Test
    fun mapperCopiesContact() {
        val domain = ExtensionStore(
            indexUrl = "https://index",
            name = "Store",
            badgeLabel = "badge",
            signingKey = "key",
            contact = ExtensionStore.Contact(website = "https://site", discord = "discord"),
            isLegacy = true,
            extensionListUrl = "https://list",
        )
        backupExtensionStoreMapper(domain) shouldBe backupExtensionStore(isLegacy = true)
    }

    @Test
    fun protoRoundTrip() {
        val store = backupExtensionStore()
        val bytes = ProtoBuf.encodeToByteArray(BackupExtensionStore.serializer(), store)
        ProtoBuf.decodeFromByteArray(BackupExtensionStore.serializer(), bytes) shouldBe store
    }
}
