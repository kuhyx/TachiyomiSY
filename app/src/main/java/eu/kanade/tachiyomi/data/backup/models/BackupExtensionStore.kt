package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.domain.extension.model.ExtensionStore

private const val BACKUP_EXTENSION_STORE_INDEX_URL = 1
private const val BACKUP_EXTENSION_STORE_NAME = 2
private const val BACKUP_EXTENSION_STORE_BADGE_LABEL = 3
private const val BACKUP_EXTENSION_STORE_SIGNING_KEY = 5
private const val BACKUP_EXTENSION_STORE_CONTACT_WEBSITE = 4
private const val BACKUP_EXTENSION_STORE_CONTACT_DISCORD = 6
private const val BACKUP_EXTENSION_STORE_IS_LEGACY = 7
private const val BACKUP_EXTENSION_STORE_EXTENSION_LIST_URL = 8

@Serializable
internal data class BackupExtensionStore(
    @ProtoNumber(BACKUP_EXTENSION_STORE_INDEX_URL) val indexUrl: String,
    @ProtoNumber(BACKUP_EXTENSION_STORE_NAME) val name: String,
    @ProtoNumber(BACKUP_EXTENSION_STORE_BADGE_LABEL) val badgeLabel: String?,
    @ProtoNumber(BACKUP_EXTENSION_STORE_SIGNING_KEY) val signingKey: String,
    @ProtoNumber(BACKUP_EXTENSION_STORE_CONTACT_WEBSITE) val contactWebsite: String,
    @ProtoNumber(BACKUP_EXTENSION_STORE_CONTACT_DISCORD) val contactDiscord: String?,
    @ProtoNumber(BACKUP_EXTENSION_STORE_IS_LEGACY) val isLegacy: Boolean?,
    @ProtoNumber(BACKUP_EXTENSION_STORE_EXTENSION_LIST_URL) val extensionListUrl: String?,
)

internal val backupExtensionStoreMapper = { store: ExtensionStore ->
    BackupExtensionStore(
        indexUrl = store.indexUrl,
        name = store.name,
        badgeLabel = store.badgeLabel,
        signingKey = store.signingKey,
        contactWebsite = store.contact.website,
        contactDiscord = store.contact.discord,
        isLegacy = store.isLegacy,
        extensionListUrl = store.extensionListUrl,
    )
}
