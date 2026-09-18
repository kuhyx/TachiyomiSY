package eu.kanade.tachiyomi.extension.model

import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.source.Source
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.domain.source.model.StubSource

/** An extension package: installed on the device, listed by a store, or installed but not yet trusted. */
public sealed class Extension {

    /** Display name of the extension. */
    public abstract val name: String

    /** Android package name of the extension APK. */
    public abstract val pkgName: String

    /** Human-readable version, as in the APK manifest. */
    public abstract val versionName: String

    /** Numeric version, as in the APK manifest; larger is newer. */
    public abstract val versionCode: Long

    /** Version of the extensions-lib the package was built against. */
    public abstract val libVersion: Double

    /** Language code of the extension (`all` or `other` for multi-language packs); null when unknown. */
    public abstract val lang: String?

    /** Whether the extension declares adult content. */
    public abstract val isNsfw: Boolean

    /**
     * An extension installed on this device, with its loaded sources.
     *
     * @property name Display name of the extension.
     * @property pkgName Android package name of the extension APK.
     * @property versionName Human-readable version, as in the APK manifest.
     * @property versionCode Numeric version, as in the APK manifest.
     * @property libVersion Version of the extensions-lib the package was built against.
     * @property lang Language code of the extension.
     * @property isNsfw Whether the extension declares adult content.
     * @property pkgFactory Class name of the package's `SourceFactory`; null when it exposes a single source.
     * @property sources The sources the package provides, already instantiated.
     * @property icon Launcher icon of the package; null when it could not be loaded.
     * @property hasUpdate Whether a store lists a newer version.
     * @property isObsolete Whether no store lists the package any more ("orphaned" in the UI).
     * @property isShared Whether the APK is installed system-wide rather than into the app's private directory.
     * @property store The store the package was matched to; null when none lists it.
     * @property isRedundant SY: whether the package is on the built-in blacklist because the app ships the source.
     */
    public data class Installed(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        val pkgFactory: String?,
        val sources: List<Source>,
        val icon: Drawable?,
        val hasUpdate: Boolean = false,
        val isObsolete: Boolean = false,
        val isShared: Boolean,
        val store: ExtensionStore? = null,
        // SY -->
        val isRedundant: Boolean = false,
        // SY <--
    ) : Extension()

    /**
     * An extension a store lists for download.
     *
     * @property name Display name of the extension.
     * @property pkgName Android package name of the extension APK.
     * @property versionName Human-readable version of the listed build.
     * @property versionCode Numeric version of the listed build.
     * @property libVersion Version of the extensions-lib the build was built against.
     * @property lang Language code of the extension.
     * @property isNsfw Whether the extension declares adult content.
     * @property sources The sources the package provides, as the store describes them.
     * @property apkUrl Download url of the APK.
     * @property iconUrl Url of the package icon.
     * @property store The store that lists it.
     */
    public data class Available(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val isNsfw: Boolean,
        val sources: List<Source>,
        val apkUrl: String,
        val iconUrl: String,
        val store: ExtensionStore,
    ) : Extension() {

        /**
         * A source as a store's index describes it, before the package is installed.
         *
         * @property id Source id, stable across installs.
         * @property lang Language code of the source.
         * @property name Display name of the source.
         * @property baseUrl Site the source scrapes.
         */
        public data class Source(
            val id: Long,
            val lang: String,
            val name: String,
            val baseUrl: String,
        ) {
            /** A [StubSource] carrying this id, language and name, to stand in until the package is installed. */
            public fun toStubSource(): StubSource {
                return StubSource(
                    id = this.id,
                    lang = this.lang,
                    name = this.name,
                )
            }
        }
    }

    /**
     * An installed extension whose signing key the user has not trusted yet, so its sources are not loaded.
     *
     * @property name Display name of the extension.
     * @property pkgName Android package name of the extension APK.
     * @property versionName Human-readable version, as in the APK manifest.
     * @property versionCode Numeric version, as in the APK manifest.
     * @property libVersion Version of the extensions-lib the package was built against.
     * @property signatureHash SHA-256 of the signing certificate, which trusting records.
     * @property lang Language code; unknown (null) until the package is loaded.
     * @property isNsfw Whether the extension declares adult content; false until the package is loaded.
     */
    public data class Untrusted(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        val signatureHash: String,
        override val lang: String? = null,
        override val isNsfw: Boolean = false,
    ) : Extension()
}
