package eu.kanade.tachiyomi.extension.util

import android.content.pm.PackageInfo
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.tachiyomi.extension.util.ExtensionLoader.ExtensionInfo
import eu.kanade.tachiyomi.util.lang.Hash

// Choose which extension package to use based on version code.
// @param shared extension installed to system
// @param private extension installed to data directory
internal fun ExtensionLoader.selectExtensionPackage(shared: ExtensionInfo?, private: ExtensionInfo?): ExtensionInfo? {
    when {
        private == null && shared != null -> return shared
        shared == null && private != null -> return private
        shared == null && private == null -> return null
    }

    return if (PackageInfoCompat.getLongVersionCode(shared!!.packageInfo) >=
        PackageInfoCompat.getLongVersionCode(private!!.packageInfo)
    ) {
        shared
    } else {
        private
    }
}

// Returns true if the given package is an extension.
// @param pkgInfo The package info of the application.
internal fun ExtensionLoader.isPackageAnExtension(pkgInfo: PackageInfo): Boolean =
    pkgInfo.reqFeatures.orEmpty().any { it.name == EXTENSION_FEATURE }

// Returns the signatures of the package or null if it's not signed.
// @param pkgInfo The package info of the application.
// @return List SHA256 digest of the signatures
internal fun ExtensionLoader.getSignatures(pkgInfo: PackageInfo): List<String>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val signingInfo = pkgInfo.signingInfo!!
        if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }
    } else {
        @Suppress("DEPRECATION")
        pkgInfo.signatures
    }
        ?.map { Hash.sha256(it.toByteArray()) }
        ?.toList()
}
