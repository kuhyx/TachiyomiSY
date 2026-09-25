package eu.kanade.presentation.more.settings.screen.browse

import mihon.domain.extension.model.ExtensionStore

internal fun store(url: String, discord: String? = null): ExtensionStore = ExtensionStore(
    indexUrl = url,
    name = "Store $url",
    badgeLabel = "S",
    signingKey = "key",
    contact = ExtensionStore.Contact(website = "https://site.example", discord = discord),
    isLegacy = false,
    extensionListUrl = null,
)
