package mihon.domain.extension.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

internal class ExtensionStoreTest {

    @Test
    fun fieldsAreKept() {
        val store = extensionStore().copy(isLegacy = true, extensionListUrl = "https://example.test/list.json")
        store.indexUrl shouldBe "https://example.test/index.min.json"
        store.name shouldBe "Example"
        store.badgeLabel shouldBe "EX"
        store.signingKey shouldBe "abc123"
        store.contact shouldBe ExtensionStore.Contact(website = "https://example.test", discord = "dc")
        store.isLegacy shouldBe true
        store.extensionListUrl shouldBe "https://example.test/list.json"
    }

    @Test
    fun storeDataClassContract() {
        val store = extensionStore()
        val same = extensionStore()
        store shouldBe same
        store.hashCode() shouldBe same.hashCode()
        store.toString() shouldBe same.toString()
        store.toString() shouldContain "Example"
        store.copy() shouldBe store
        store shouldNotBe extensionStore(indexUrl = "https://other.test/index.min.json")
        store shouldNotBe "not a store"
    }

    @Test
    fun storeComponents() {
        val store = extensionStore()
        store.component1() shouldBe "https://example.test/index.min.json"
        store.component2() shouldBe "Example"
        store.component3() shouldBe "EX"
        store.component4() shouldBe "abc123"
        store.component5() shouldBe store.contact
        store.component6() shouldBe false
        store.component7() shouldBe null
    }

    @Test
    fun contactDataClassContract() {
        val contact = ExtensionStore.Contact(website = "https://example.test", discord = null)
        val same = ExtensionStore.Contact(website = "https://example.test", discord = null)
        contact shouldBe same
        contact.hashCode() shouldBe same.hashCode()
        contact.toString() shouldBe same.toString()
        contact.toString() shouldContain "example.test"
        contact.copy() shouldBe contact
        contact shouldNotBe contact.copy(discord = "dc")
        contact shouldNotBe "not a contact"
        contact.component1() shouldBe "https://example.test"
        contact.component2() shouldBe null
        contact.copy(discord = "dc").component2() shouldBe "dc"
    }
}
