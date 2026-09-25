package eu.kanade.domain.extension.interactor

import eu.kanade.tachiyomi.extension.model.Extension
import mihon.domain.extension.model.ExtensionStore

/** The store every fixture extension comes from. */
internal val testStore: ExtensionStore = ExtensionStore(
    indexUrl = "https://store/index.json",
    name = "Store",
    badgeLabel = "S",
    signingKey = "key",
    contact = ExtensionStore.Contact(website = "https://store", discord = null),
    isLegacy = false,
    extensionListUrl = null,
)

/** An installed extension named [name]. */
internal fun installed(
    name: String,
    pkgName: String = "pkg.$name",
    isNsfw: Boolean = false,
    hasUpdate: Boolean = false,
    isObsolete: Boolean = false,
    isRedundant: Boolean = false,
): Extension.Installed = Extension.Installed(
    name = name,
    pkgName = pkgName,
    versionName = "1.0",
    versionCode = 1,
    libVersion = 1.5,
    lang = "en",
    isNsfw = isNsfw,
    pkgFactory = null,
    sources = emptyList(),
    icon = null,
    hasUpdate = hasUpdate,
    isObsolete = isObsolete,
    isShared = false,
    isRedundant = isRedundant,
)

/** An available extension named [name] exposing [sources] as `id to lang`. */
internal fun available(
    name: String,
    sources: List<Pair<Long, String>>,
    pkgName: String = "pkg.$name",
    isNsfw: Boolean = false,
): Extension.Available = Extension.Available(
    name = name,
    pkgName = pkgName,
    versionName = "1.0",
    versionCode = 1,
    libVersion = 1.5,
    lang = "all",
    isNsfw = isNsfw,
    sources = sources.map { (id, lang) ->
        Extension.Available.Source(id = id, lang = lang, name = "$name $lang", baseUrl = "https://$name")
    },
    apkUrl = "https://store/$name.apk",
    iconUrl = "https://store/$name.png",
    store = testStore,
)

/** An untrusted extension named [name]. */
internal fun untrusted(name: String, pkgName: String = "pkg.$name"): Extension.Untrusted = Extension.Untrusted(
    name = name,
    pkgName = pkgName,
    versionName = "1.0",
    versionCode = 1,
    libVersion = 1.5,
    signatureHash = "hash",
)
