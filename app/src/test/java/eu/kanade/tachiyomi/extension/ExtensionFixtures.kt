package eu.kanade.tachiyomi.extension

import eu.kanade.tachiyomi.extension.model.Extension
import mihon.domain.extension.model.ExtensionStore

/** The store every fixture extension belongs to. */
internal val fixtureStore: ExtensionStore = ExtensionStore(
    indexUrl = "https://store/index.min.json",
    name = "Store",
    badgeLabel = "S",
    signingKey = "key",
    contact = ExtensionStore.Contact(website = "https://store", discord = null),
    isLegacy = false,
    extensionListUrl = null,
)

/** An extension a store lists. */
internal fun anAvailableExtension(
    pkgName: String = "pkg.one",
    versionCode: Long = 2L,
    libVersion: Double = 1.6,
    sources: List<Extension.Available.Source> = emptyList(),
): Extension.Available = Extension.Available(
    name = "Ext $pkgName",
    pkgName = pkgName,
    versionName = "1.0",
    versionCode = versionCode,
    libVersion = libVersion,
    lang = "en",
    isNsfw = false,
    sources = sources,
    apkUrl = "https://store/$pkgName.apk",
    iconUrl = "https://store/$pkgName.png",
    store = fixtureStore,
)

/** A source as a store's index describes it. */
internal fun anAvailableSource(id: Long, lang: String = "en"): Extension.Available.Source =
    Extension.Available.Source(id = id, lang = lang, name = "Source $id", baseUrl = "https://source/$id")

/** An extension installed on the device. */
internal fun anInstalledExtension(
    pkgName: String = "pkg.one",
    versionCode: Long = 1L,
    libVersion: Double = 1.6,
    hasUpdate: Boolean = false,
    isObsolete: Boolean = false,
    isRedundant: Boolean = false,
    sources: List<eu.kanade.tachiyomi.source.Source> = emptyList(),
): Extension.Installed = Extension.Installed(
    name = "Ext $pkgName",
    pkgName = pkgName,
    versionName = "1.0",
    versionCode = versionCode,
    libVersion = libVersion,
    lang = "en",
    isNsfw = false,
    pkgFactory = null,
    sources = sources,
    icon = null,
    hasUpdate = hasUpdate,
    isObsolete = isObsolete,
    isShared = true,
    isRedundant = isRedundant,
)

/** An installed extension whose signature the user has not trusted. */
internal fun anUntrustedExtension(pkgName: String = "pkg.one"): Extension.Untrusted = Extension.Untrusted(
    name = "Ext $pkgName",
    pkgName = pkgName,
    versionName = "1.0",
    versionCode = 1L,
    libVersion = 1.6,
    signatureHash = "hash",
)
