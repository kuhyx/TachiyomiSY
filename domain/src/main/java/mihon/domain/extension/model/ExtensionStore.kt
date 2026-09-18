package mihon.domain.extension.model

/**
 * An extension repository ("store") the user has added, as read from its index.
 *
 * @property indexUrl Url of the store's index file; also the store's identity.
 * @property name Display name of the store.
 * @property badgeLabel Short label shown on extensions that come from this store.
 * @property signingKey Fingerprint the store's extension APKs are signed with.
 * @property contact Where to reach the store's maintainers.
 * @property isLegacy Whether the store is an old-format repo listing extensions in `index.min.json`.
 * @property extensionListUrl Url of the extension list when it lives apart from the index; null otherwise.
 */
public data class ExtensionStore(
    val indexUrl: String,
    val name: String,
    val badgeLabel: String,
    val signingKey: String,
    val contact: Contact,
    val isLegacy: Boolean,
    val extensionListUrl: String?,
) {
    /**
     * Maintainer contact points of a store.
     *
     * @property website Home page of the store.
     * @property discord Invite url of the store's Discord, or null.
     */
    public data class Contact(
        val website: String,
        val discord: String?,
    )
}
