package mihon.domain.extension.model

/** A store with every field set, [discord] optionally left out. */
internal fun extensionStore(
    indexUrl: String = "https://example.test/index.min.json",
    discord: String? = "dc",
): ExtensionStore = ExtensionStore(
    indexUrl = indexUrl,
    name = "Example",
    badgeLabel = "EX",
    signingKey = "abc123",
    contact = ExtensionStore.Contact(website = "https://example.test", discord = discord),
    isLegacy = false,
    extensionListUrl = null,
)
