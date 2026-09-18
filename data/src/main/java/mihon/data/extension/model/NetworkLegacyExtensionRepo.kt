package mihon.data.extension.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mihon.domain.extension.model.ExtensionStore

/**
 * A legacy store's `repo.json`, the index format that predates [NetworkExtensionStore].
 *
 * @property indexV2 Url of the current-format index that replaces this repo, or null when there is none.
 * @property meta The repo's own description.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
public data class NetworkLegacyExtensionRepo(
    @SerialName("index_v2")
    val indexV2: String?,
    val meta: Meta,
) : BaseNetworkExtensionStore {
    /**
     * Description block of a legacy repo.
     *
     * @property name Display name of the repo.
     * @property shortName Short label shown on its extensions, or null to reuse [name].
     * @property website Home page of the repo.
     * @property signingKeyFingerprint Fingerprint the repo's extension APKs are signed with.
     */
    @Serializable
    public data class Meta(
        val name: String,
        val shortName: String?,
        val website: String,
        val signingKeyFingerprint: String,
    )

    override fun toExtensionStore(indexUrl: String): ExtensionStore {
        return ExtensionStore(
            indexUrl = indexUrl,
            name = meta.name,
            badgeLabel = meta.shortName ?: meta.name,
            signingKey = meta.signingKeyFingerprint,
            contact = ExtensionStore.Contact(
                website = meta.website,
                discord = null,
            ),
            isLegacy = true,
            extensionListUrl = null,
        )
    }
}
