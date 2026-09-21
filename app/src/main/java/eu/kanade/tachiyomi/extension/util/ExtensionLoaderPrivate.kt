package eu.kanade.tachiyomi.extension.util

import android.content.Context
import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.tachiyomi.util.storage.copyAndSetReadOnlyTo
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File

internal fun ExtensionLoader.getPrivateExtensionDir(context: Context) = File(context.filesDir, "exts")

internal fun ExtensionLoader.installPrivateExtensionFile(context: Context, file: File): Boolean {
    val extension = context.packageManager.getPackageArchiveInfo(file.absolutePath, PACKAGE_FLAGS)
        ?.takeIf { isPackageAnExtension(it) }
        ?: return false
    val currentExtension = getExtensionPackageInfo(context, extension.packageName)

    val rejection = currentExtension?.let { replacementRejection(it, extension) }
    if (rejection != null) {
        logcat(LogPriority.ERROR) { rejection }
        return false
    }

    val target = File(getPrivateExtensionDir(context), "${extension.packageName}.$PRIVATE_EXTENSION_EXTENSION")
    return try {
        target.delete()
        file.copyAndSetReadOnlyTo(target, overwrite = true)
        if (currentExtension != null) {
            ExtensionInstallReceiver.notifyReplaced(context, extension.packageName)
        } else {
            ExtensionInstallReceiver.notifyAdded(context, extension.packageName)
        }
        true
    } catch (expected: Exception) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected) { "Failed to copy extension file." }
        target.delete()
        false
    }
}

// Why [extension] may not replace [current]: only a same-or-newer build signed by the same keys may. Null when it may.
private fun ExtensionLoader.replacementRejection(current: PackageInfo, extension: PackageInfo): String? {
    val extensionSignatures = getSignatures(extension).orEmpty()
    return when {
        PackageInfoCompat.getLongVersionCode(extension) < PackageInfoCompat.getLongVersionCode(current) -> {
            "Installed extension version is higher. Downgrading is not allowed."
        }
        extensionSignatures.isEmpty() -> {
            "Extension to be installed is not signed."
        }
        !extensionSignatures.containsAll(getSignatures(current)!!) -> {
            "Installed extension signature is not matched."
        }
        else -> {
            null
        }
    }
}

internal fun ExtensionLoader.uninstallPrivateExtension(context: Context, pkgName: String) {
    File(getPrivateExtensionDir(context), "$pkgName.$PRIVATE_EXTENSION_EXTENSION").delete()
}
