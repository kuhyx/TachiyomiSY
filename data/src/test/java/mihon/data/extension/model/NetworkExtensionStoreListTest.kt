package mihon.data.extension.model

import eu.kanade.tachiyomi.extension.model.Extension
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import mihon.data.extension.model.NetworkExtensionStore.ContentWarning
import org.junit.jupiter.api.Test

internal class NetworkExtensionStoreListTest {
    private val store = domainStore()

    private fun available(extension: NetworkExtensionStore.Extension): Extension.Available =
        NetworkExtensionStore.ExtensionList(listOf(extension)).toAvailableExtensions(store).single()

    @Test
    fun mapsEveryField() {
        val extension = networkExtension(
            sources = listOf(
                NetworkExtensionStore.Source(id = 9L, name = "Nine", language = "en", homeUrl = "https://nine"),
            ),
        )

        available(extension) shouldBe Extension.Available(
            name = "Demo",
            pkgName = "eu.kanade.tachiyomi.extension.en.demo",
            versionName = "1.4.7",
            versionCode = 7L,
            libVersion = 1.5,
            lang = "en",
            isNsfw = false,
            sources = listOf(
                Extension.Available.Source(id = 9L, lang = "en", name = "Nine", baseUrl = "https://nine"),
            ),
            apkUrl = "https://store.example/apk/demo.apk",
            iconUrl = "https://store.example/icon/demo.png",
            store = store,
        )
    }

    @Test
    fun multiLanguageFilesUnderAll() {
        val extension = networkExtension(
            sources = listOf(networkSource(id = 1L, language = "en"), networkSource(id = 2L, language = "fr")),
        )

        available(extension).lang shouldBe "all"
    }

    @Test
    fun noSourcesFileUnderAll() {
        available(networkExtension(sources = emptyList())).lang shouldBe "all"
    }

    @Test
    fun mixedAndAboveAreNsfw() {
        available(networkExtension(contentWarning = ContentWarning.UNSPECIFIED)).isNsfw shouldBe false
        available(networkExtension(contentWarning = ContentWarning.SAFE)).isNsfw shouldBe false
        available(networkExtension(contentWarning = ContentWarning.MIXED)).isNsfw shouldBe true
        available(networkExtension(contentWarning = ContentWarning.NSFW)).isNsfw shouldBe true
    }

    @Test
    fun emptyListMapsToNothing() {
        NetworkExtensionStore.ExtensionList(emptyList()).toAvailableExtensions(store).shouldBeEmpty()
    }

    @Test
    fun everyExtensionIsMapped() {
        val list = NetworkExtensionStore.ExtensionList(
            listOf(networkExtension(packageName = "a"), networkExtension(packageName = "b")),
        )

        list.toAvailableExtensions(store).map { it.pkgName } shouldBe listOf("a", "b")
    }
}
