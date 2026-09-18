package mihon.data.extension.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.protobuf.ProtoNumber
import mihon.data.extension.model.NetworkExtensionStore.ContentWarning
import mihon.data.extension.model.NetworkExtensionStore.ExtensionList
import mihon.domain.extension.model.ExtensionStore
import eu.kanade.tachiyomi.extension.model.Extension as TachiyomiExtension

// Protobuf field tags of the extension-store schema; the JSON form uses the property names.
private const val STORE_NAME = 1
private const val STORE_BADGE_LABEL = 2
private const val STORE_SIGNING_KEY = 3
private const val STORE_CONTACT = 4
private const val STORE_EXTENSION_LIST = 101
private const val STORE_EXTENSION_LIST_URL = 102
private const val CONTACT_WEBSITE = 1
private const val CONTACT_DISCORD = 2
private const val LIST_EXTENSIONS = 1
private const val EXTENSION_NAME = 1
private const val EXTENSION_PACKAGE_NAME = 2
private const val EXTENSION_RESOURCES = 3
private const val EXTENSION_LIB = 4
private const val EXTENSION_VERSION_CODE = 5
private const val EXTENSION_VERSION_NAME = 6
private const val EXTENSION_CONTENT_WARNING = 7
private const val EXTENSION_SOURCES = 8
private const val RESOURCES_APK_URL = 1
private const val RESOURCES_ICON_URL = 2
private const val SOURCE_ID = 1
private const val SOURCE_NAME = 2
private const val SOURCE_LANGUAGE = 3
private const val SOURCE_HOME_URL = 4
private const val SOURCE_MIRROR_URLS = 5
private const val SOURCE_MESSAGE = 7
private const val WARNING_UNSPECIFIED = 0
private const val WARNING_SAFE = 1
private const val WARNING_MIXED = 2
private const val WARNING_NSFW = 3

/**
 * A current-format store index as served over the network, in protobuf or JSON.
 *
 * @property name Display name of the store.
 * @property badgeLabel Short label shown on extensions that come from this store.
 * @property signingKey Fingerprint the store's extension APKs are signed with.
 * @property contact Where to reach the store's maintainers.
 * @property extensionList The extensions when the index embeds them; null when they live at [extensionListUrl].
 * @property extensionListUrl Url of the extension list when it lives apart from the index; null otherwise.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
public data class NetworkExtensionStore(
    @ProtoNumber(STORE_NAME) val name: String,
    @ProtoNumber(STORE_BADGE_LABEL) val badgeLabel: String,
    @ProtoNumber(STORE_SIGNING_KEY) val signingKey: String,
    @ProtoNumber(STORE_CONTACT) val contact: Contact,
    @ProtoNumber(STORE_EXTENSION_LIST) val extensionList: ExtensionList?,
    @ProtoNumber(STORE_EXTENSION_LIST_URL) val extensionListUrl: String?,
) : BaseNetworkExtensionStore {
    /**
     * Maintainer contact points of a store.
     *
     * @property website Home page of the store.
     * @property discord Invite url of the store's Discord, or null.
     */
    @Serializable
    public data class Contact(
        @ProtoNumber(CONTACT_WEBSITE) val website: String,
        @ProtoNumber(CONTACT_DISCORD) val discord: String?,
    )

    /**
     * The extensions a store offers, embedded in the index or fetched from its own url.
     *
     * @property extensions Every extension the store offers.
     */
    @Serializable
    public data class ExtensionList(@ProtoNumber(LIST_EXTENSIONS) val extensions: List<Extension>)

    /**
     * One extension in a store's list.
     *
     * @property name Display name of the extension.
     * @property packageName Android package name of the extension.
     * @property resources Where its APK and icon are downloaded from.
     * @property extensionLib Extension-lib version the APK was built against, as a decimal string.
     * @property versionCode Version code of the APK.
     * @property versionName Version name of the APK.
     * @property contentWarning How explicit the extension's content is; [ContentWarning.MIXED] and up count as NSFW.
     * @property sources The sources the extension ships.
     */
    @Serializable
    public data class Extension(
        @ProtoNumber(EXTENSION_NAME) val name: String,
        @ProtoNumber(EXTENSION_PACKAGE_NAME) val packageName: String,
        @ProtoNumber(EXTENSION_RESOURCES) val resources: Resources,
        @ProtoNumber(EXTENSION_LIB) val extensionLib: String,
        @ProtoNumber(EXTENSION_VERSION_CODE) val versionCode: Long,
        @ProtoNumber(EXTENSION_VERSION_NAME) val versionName: String,
        @ProtoNumber(EXTENSION_CONTENT_WARNING) val contentWarning: ContentWarning,
        @ProtoNumber(EXTENSION_SOURCES) val sources: List<Source>,
    )

    /**
     * Download locations of an extension.
     *
     * @property apkUrl Url of the extension APK.
     * @property iconUrl Url of the extension icon.
     */
    @Serializable
    public data class Resources(
        @ProtoNumber(RESOURCES_APK_URL) val apkUrl: String,
        @ProtoNumber(RESOURCES_ICON_URL) val iconUrl: String,
    )

    /**
     * One source inside a store extension.
     *
     * @property id Source id.
     * @property name Display name of the source.
     * @property language Language code of the source.
     * @property homeUrl Home url of the source; empty when the store omits it.
     * @property mirrorUrls Alternative urls of the same site; empty when the store lists none.
     * @property message Notice the store attaches to the source, or null.
     */
    @Serializable
    public data class Source(
        @ProtoNumber(SOURCE_ID) val id: Long,
        @ProtoNumber(SOURCE_NAME) val name: String,
        @ProtoNumber(SOURCE_LANGUAGE) val language: String,
        @ProtoNumber(SOURCE_HOME_URL) val homeUrl: String = "",
        @ProtoNumber(SOURCE_MIRROR_URLS) val mirrorUrls: List<String> = emptyList(),
        // Tag 6 is the source's own content warning, not read yet.
        @ProtoNumber(SOURCE_MESSAGE) val message: String? = null,
    )

    /** How explicit an extension's content is, ordered from unknown to fully NSFW. */
    public enum class ContentWarning {
        /** The store did not say. */
        @ProtoNumber(WARNING_UNSPECIFIED)
        @JsonNames("CONTENT_WARNING_UNSPECIFIED")
        UNSPECIFIED,

        /** Safe for work. */
        @ProtoNumber(WARNING_SAFE)
        @JsonNames("CONTENT_WARNING_SAFE")
        SAFE,

        /** Some sources are NSFW; treated as NSFW. */
        @ProtoNumber(WARNING_MIXED)
        @JsonNames("CONTENT_WARNING_MIXED")
        MIXED,

        /** Not safe for work. */
        @ProtoNumber(WARNING_NSFW)
        @JsonNames("CONTENT_WARNING_NSFW")
        NSFW,
    }

    override fun toExtensionStore(indexUrl: String): ExtensionStore {
        return ExtensionStore(
            indexUrl = indexUrl,
            name = name,
            badgeLabel = badgeLabel,
            signingKey = signingKey,
            contact = ExtensionStore.Contact(
                website = contact.website,
                discord = contact.discord,
            ),
            isLegacy = false,
            extensionListUrl = extensionListUrl,
        )
    }
}

/**
 * The installable form of every extension in this list for [store]; an extension whose sources span
 * more than one language is filed under `all`.
 */
public fun ExtensionList.toAvailableExtensions(store: ExtensionStore): List<TachiyomiExtension.Available> {
    return extensions.map { extension ->
        val lang = extension.sources.map { it.language }.toSet()
        TachiyomiExtension.Available(
            name = extension.name,
            pkgName = extension.packageName,
            apkUrl = extension.resources.apkUrl,
            iconUrl = extension.resources.iconUrl,
            libVersion = extension.extensionLib.toDouble(),
            versionCode = extension.versionCode,
            versionName = extension.versionName,
            lang = if (lang.size == 1) lang.first() else "all",
            isNsfw = extension.contentWarning >= ContentWarning.MIXED,
            sources = extension.sources.map { source ->
                TachiyomiExtension.Available.Source(
                    id = source.id,
                    name = source.name,
                    lang = source.language,
                    baseUrl = source.homeUrl,
                )
            },
            store = store,
        )
    }
}
