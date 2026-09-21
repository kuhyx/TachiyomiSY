package eu.kanade.tachiyomi.extension.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import eu.kanade.domain.extension.interactor.TrustExtension
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.model.LoadResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.injectLazy
import java.io.File

// Class that handles the loading of the extensions. Supports two kinds of extensions:
// 1. Shared extension: This extension is installed to the system with package
// installer, so other variants of Tachiyomi and its forks can also use this extension.
// 2. Private extension: This extension is put inside private data directory of the
// running app, so this extension can only be used by the running app and not shared
// with other apps.
// When both kinds of extensions are installed with a same package name, shared
// extension will be used unless the version codes are different. In that case the
// one with higher version code will be used.
private const val LIB_VERSION_1_4 = 1.4
private const val LIB_VERSION_1_6 = 1.6

internal object ExtensionLoader {

    private val preferences: SourcePreferences by injectLazy()
    internal val trustExtension: TrustExtension by injectLazy()
    internal val loadNsfwSource by lazy {
        preferences.showNsfwSource.get()
    }

    internal const val EXTENSION_FEATURE = "tachiyomi.extension"
    internal const val METADATA_SOURCE_FACTORY = "tachiyomi.extension.factory"
    internal const val METADATA_NSFW = "tachiyomi.extension.nsfw"

    internal const val METADATA_NAME = "tachiyomix.name"
    internal const val METADATA_EXTENSION_LIB = "tachiyomix.extensionLib"
    internal const val METADATA_CONTENT_WARNING = "tachiyomix.contentWarning"

    internal val SUPPORTED_LIB_VERSIONS = listOf(LIB_VERSION_1_4, LIB_VERSION_1_6)

    @Suppress("DEPRECATION")
    internal val PACKAGE_FLAGS = PackageManager.GET_CONFIGURATIONS or
        PackageManager.GET_META_DATA or
        PackageManager.GET_SIGNATURES or
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else 0

    internal const val PRIVATE_EXTENSION_EXTENSION = "ext"

    /**
     * Return a list of all the available extensions initialized concurrently.
     *
     * @param context The application context.
     */
    fun loadExtensions(context: Context): List<LoadResult> {
        val pkgManager = context.packageManager

        val installedPkgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pkgManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PACKAGE_FLAGS.toLong()))
        } else {
            pkgManager.getInstalledPackages(PACKAGE_FLAGS)
        }

        val sharedExtPkgs = installedPkgs
            .asSequence()
            .filter { isPackageAnExtension(it) }
            .map { ExtensionInfo(packageInfo = it, isShared = true) }

        val privateExtPkgs = getPrivateExtensionDir(context)
            .listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.extension == PRIVATE_EXTENSION_EXTENSION }
            ?.mapNotNull {
                // Just in case, since Android 14+ requires them to be read-only
                if (it.canWrite()) {
                    it.setReadOnly()
                }

                val path = it.absolutePath
                pkgManager.getPackageArchiveInfo(path, PACKAGE_FLAGS)
                    ?.apply { applicationInfo!!.fixBasePaths(path) }
            }
            ?.filter { isPackageAnExtension(it) }
            ?.map { ExtensionInfo(packageInfo = it, isShared = false) }
            ?: emptySequence()

        val extPkgs = (sharedExtPkgs + privateExtPkgs)
            // Remove duplicates. Shared takes priority than private by default
            .distinctBy { it.packageInfo.packageName }
            // Compare version number
            .mapNotNull { sharedPkg ->
                val privatePkg = privateExtPkgs
                    .singleOrNull { it.packageInfo.packageName == sharedPkg.packageInfo.packageName }
                selectExtensionPackage(sharedPkg, privatePkg)
            }
            .toList()

        if (extPkgs.isEmpty()) return emptyList()

        // Load each extension concurrently and wait for completion
        return runBlocking {
            val deferred = extPkgs.map {
                async { loadExtension(context, it) }
            }
            deferred.awaitAll()
        }
    }

    /**
     * Attempts to load an extension from the given package name. It checks if the extension
     * contains the required feature flag before trying to load it.
     */
    suspend fun loadExtensionFromPkgName(context: Context, pkgName: String): LoadResult {
        val extensionPackage = getExtensionInfoFromPkgName(context, pkgName)
        if (extensionPackage == null) {
            logcat(LogPriority.ERROR) { "Extension package is not found ($pkgName)" }
            return LoadResult.Error
        }
        return loadExtension(context, extensionPackage)
    }

    fun getExtensionPackageInfo(context: Context, pkgName: String): PackageInfo? =
        getExtensionInfoFromPkgName(context, pkgName)?.packageInfo

    private fun getExtensionInfoFromPkgName(context: Context, pkgName: String): ExtensionInfo? {
        val privateExtensionFile = File(getPrivateExtensionDir(context), "$pkgName.$PRIVATE_EXTENSION_EXTENSION")
        val privatePkg = if (privateExtensionFile.isFile) {
            context.packageManager.getPackageArchiveInfo(privateExtensionFile.absolutePath, PACKAGE_FLAGS)
                ?.takeIf { isPackageAnExtension(it) }
                ?.let {
                    it.applicationInfo!!.fixBasePaths(privateExtensionFile.absolutePath)
                    ExtensionInfo(
                        packageInfo = it,
                        isShared = false,
                    )
                }
        } else {
            null
        }

        val sharedPkg = try {
            context.packageManager.getPackageInfo(pkgName, PACKAGE_FLAGS)
                .takeIf { isPackageAnExtension(it) }
                ?.let {
                    ExtensionInfo(
                        packageInfo = it,
                        isShared = true,
                    )
                }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        return selectExtensionPackage(sharedPkg, privatePkg)
    }

    // Loads an extension.
    // @param context The application context.
    // @param extensionInfo The extension to load.
    private suspend fun loadExtension(context: Context, extensionInfo: ExtensionInfo): LoadResult {
        val pkgInfo = extensionInfo.packageInfo
        val header = extensionHeader(context, pkgInfo) ?: return LoadResult.Error
        return checkTrust(pkgInfo, header) ?: buildExtension(context, extensionInfo, header)
    }

    /** The manifest fields every load path needs; null (logged) when the version or lib version is unusable. */
    internal data class ExtensionHeader(
        val name: String,
        val versionName: String,
        val versionCode: Long,
        val libVersion: Double,
        val isNsfw: Boolean,
    )

    // On Android 13+ the ApplicationInfo generated by getPackageArchiveInfo doesn't
    // have sourceDir which breaks assets loading (used for getting icon here).
    private fun ApplicationInfo.fixBasePaths(apkPath: String) {
        if (sourceDir == null) {
            sourceDir = apkPath
        }
        if (publicSourceDir == null) {
            publicSourceDir = apkPath
        }
    }

    internal data class ExtensionInfo(
        val packageInfo: PackageInfo,
        val isShared: Boolean,
    )
}
