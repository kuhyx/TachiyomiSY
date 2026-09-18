package mihon.data.extension.model

import android.annotation.SuppressLint
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.serialization.Serializable
import mihon.domain.extension.model.ExtensionStore

/**
 * One entry of a legacy store's `index.min.json` extension array.
 *
 * @property name Display name, usually prefixed with `Tachiyomi: `.
 * @property pkg Android package name of the extension.
 * @property apk File name of the APK under the store's `apk/` directory.
 * @property lang Language code the extension serves, or `all`.
 * @property code Version code of the APK.
 * @property version Version name; its last dotted component is the extension-lib version.
 * @property nsfw 1 when the extension is NSFW, 0 otherwise.
 * @property sources The sources the extension ships, or null on old indexes.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
public data class NetworkLegacyExtension(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val sources: List<Source>?,
) {
    /**
     * One source inside a legacy extension entry.
     *
     * @property id Source id.
     * @property lang Language code of the source.
     * @property name Display name of the source.
     * @property baseUrl Home url of the source.
     */
    @Serializable
    public data class Source(
        val id: Long,
        val lang: String,
        val name: String,
        val baseUrl: String,
    )

    /**
     * The installable form of this entry for [store], with APK and icon urls built on [storeBaseUrl];
     * an entry without sources gets one placeholder source with id 0.
     */
    public fun toAvailableExtension(store: ExtensionStore, storeBaseUrl: String): Extension.Available {
        return Extension.Available(
            name = name.substringAfter("Tachiyomi: "),
            pkgName = pkg,
            apkUrl = "$storeBaseUrl/apk/$apk",
            iconUrl = "$storeBaseUrl/icon/$pkg.png",
            libVersion = version.substringBeforeLast('.').toDouble(),
            versionCode = code,
            versionName = version,
            lang = lang,
            isNsfw = nsfw == 1,
            sources = if (sources.isNullOrEmpty()) {
                listOf(
                    Extension.Available.Source(
                        id = 0,
                        name = name,
                        lang = lang,
                        baseUrl = "",
                    ),
                )
            } else {
                sources.map { source ->
                    Extension.Available.Source(
                        id = source.id,
                        name = source.name,
                        lang = source.lang,
                        baseUrl = source.baseUrl,
                    )
                }
            },
            store = store,
        )
    }
}
