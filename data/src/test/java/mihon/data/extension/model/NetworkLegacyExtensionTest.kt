package mihon.data.extension.model

import eu.kanade.tachiyomi.extension.model.Extension
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import mihon.data.extension.assertDataClass
import org.junit.jupiter.api.Test

internal class NetworkLegacyExtensionTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val store = domainStore(indexUrl = "https://legacy.example/repo.json", isLegacy = true)
    private val source = NetworkLegacyExtension.Source(id = 5L, lang = "en", name = "Five", baseUrl = "https://five")

    private fun legacy(
        sources: List<NetworkLegacyExtension.Source>?,
        nsfw: Int = 0,
        name: String = "Tachiyomi: Demo",
    ): NetworkLegacyExtension = NetworkLegacyExtension(
        name = name,
        pkg = "eu.kanade.tachiyomi.extension.en.demo",
        apk = "demo.apk",
        lang = "en",
        code = 3L,
        version = "1.4.3",
        nsfw = nsfw,
        sources = sources,
    )

    @Test
    fun mapsSourcesAndUrls() {
        legacy(sources = listOf(source)).toAvailableExtension(store, "https://legacy.example") shouldBe
            Extension.Available(
                name = "Demo",
                pkgName = "eu.kanade.tachiyomi.extension.en.demo",
                versionName = "1.4.3",
                versionCode = 3L,
                libVersion = 1.4,
                lang = "en",
                isNsfw = false,
                sources = listOf(
                    Extension.Available.Source(id = 5L, lang = "en", name = "Five", baseUrl = "https://five"),
                ),
                apkUrl = "https://legacy.example/apk/demo.apk",
                iconUrl = "https://legacy.example/icon/eu.kanade.tachiyomi.extension.en.demo.png",
                store = store,
            )
    }

    @Test
    fun nullSourcesGetPlaceholder() {
        val available = legacy(sources = null, name = "Plain").toAvailableExtension(store, "https://legacy.example")

        available.name shouldBe "Plain"
        available.sources shouldBe listOf(
            Extension.Available.Source(id = 0L, lang = "en", name = "Plain", baseUrl = ""),
        )
    }

    @Test
    fun emptySourcesGetPlaceholder() {
        val available = legacy(sources = emptyList()).toAvailableExtension(store, "https://legacy.example")

        available.sources.single().id shouldBe 0L
        available.sources.single().name shouldBe "Tachiyomi: Demo"
    }

    @Test
    fun nsfwFlagIsOneOnly() {
        legacy(sources = null, nsfw = 1).toAvailableExtension(store, "b").isNsfw shouldBe true
        legacy(sources = null, nsfw = 2).toAvailableExtension(store, "b").isNsfw shouldBe false
    }

    @Test
    fun jsonRoundTrips() {
        val full = legacy(sources = listOf(source), nsfw = 1)
        val minimal = """{"name":"Tachiyomi: Demo","pkg":"eu.kanade.tachiyomi.extension.en.demo","apk":"demo.apk",""" +
            """"lang":"en","code":3,"version":"1.4.3","nsfw":0}"""

        val encoded = json.encodeToString(NetworkLegacyExtension.serializer(), full)

        json.decodeFromString(NetworkLegacyExtension.serializer(), encoded) shouldBe full
        json.decodeFromString(NetworkLegacyExtension.serializer(), minimal) shouldBe legacy(sources = null)
    }

    @Test
    fun dataClassContracts() {
        val extension = legacy(sources = listOf(source))

        assertDataClass(
            value = extension,
            equal = extension.copy(),
            different = listOf(
                extension.copy(name = "x"),
                extension.copy(pkg = "x"),
                extension.copy(apk = "x"),
                extension.copy(lang = "x"),
                extension.copy(code = 4L),
                extension.copy(version = "x"),
                extension.copy(nsfw = 1),
                extension.copy(sources = null),
            ),
        )
        assertDataClass(
            value = source,
            equal = source.copy(),
            different = listOf(
                source.copy(id = 6L),
                source.copy(lang = "x"),
                source.copy(name = "x"),
                source.copy(baseUrl = "x"),
            ),
        )
    }
}
