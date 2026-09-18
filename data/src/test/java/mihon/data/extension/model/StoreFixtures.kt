package mihon.data.extension.model

import mihon.data.extension.model.NetworkExtensionStore.ContentWarning
import mihon.domain.extension.model.ExtensionStore

internal const val INDEX_URL: String = "https://store.example/index.json"

/** A network source; only the required fields, so the defaults of the rest are exercised. */
internal fun networkSource(id: Long = 1L, language: String = "en"): NetworkExtensionStore.Source =
    NetworkExtensionStore.Source(id = id, name = "Source $id", language = language)

/** A network extension carrying [sources]. */
internal fun networkExtension(
    sources: List<NetworkExtensionStore.Source> = listOf(networkSource()),
    contentWarning: ContentWarning = ContentWarning.SAFE,
    packageName: String = "eu.kanade.tachiyomi.extension.en.demo",
): NetworkExtensionStore.Extension = NetworkExtensionStore.Extension(
    name = "Demo",
    packageName = packageName,
    resources = NetworkExtensionStore.Resources(
        apkUrl = "https://store.example/apk/demo.apk",
        iconUrl = "https://store.example/icon/demo.png",
    ),
    extensionLib = "1.5",
    versionCode = 7L,
    versionName = "1.4.7",
    contentWarning = contentWarning,
    sources = sources,
)

/** A current-format store index with an embedded [extensionList]. */
internal fun networkStore(
    extensionList: NetworkExtensionStore.ExtensionList? = NetworkExtensionStore.ExtensionList(
        listOf(networkExtension()),
    ),
    extensionListUrl: String? = null,
    discord: String? = "https://discord.gg/demo",
): NetworkExtensionStore = NetworkExtensionStore(
    name = "Demo Store",
    badgeLabel = "DEMO",
    signingKey = "ABCDEF",
    contact = NetworkExtensionStore.Contact(website = "https://store.example", discord = discord),
    extensionList = extensionList,
    extensionListUrl = extensionListUrl,
)

/** The domain store for [indexUrl]. */
internal fun domainStore(
    indexUrl: String = INDEX_URL,
    isLegacy: Boolean = false,
    extensionListUrl: String? = null,
): ExtensionStore = ExtensionStore(
    indexUrl = indexUrl,
    name = "Demo Store",
    badgeLabel = "DEMO",
    signingKey = "ABCDEF",
    contact = ExtensionStore.Contact(website = "https://store.example", discord = null),
    isLegacy = isLegacy,
    extensionListUrl = extensionListUrl,
)
