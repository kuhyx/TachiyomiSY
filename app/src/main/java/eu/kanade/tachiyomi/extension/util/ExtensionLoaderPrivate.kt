package eu.kanade.tachiyomi.extension.util

import android.content.Context
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

    if (currentExtension != null) {
        if (PackageInfoCompat.getLongVersionCode(extension) <
            PackageInfoCompat.getLongVersionCode(currentExtension)
        ) {
            logcat(LogPriority.ERROR) { "Installed extension version is higher. Downgrading is not allowed." }
            return false
        }

        val extensionSignatures = getSignatures(extension)
        if (extensionSignatures.isNullOrEmpty()) {
            logcat(LogPriority.ERROR) { "Extension to be installed is not signed." }
            return false
        }

        if (!extensionSignatures.containsAll(getSignatures(currentExtension)!!)) {
            logcat(LogPriority.ERROR) { "Installed extension signature is not matched." }
            return false
        }
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

internal fun ExtensionLoader.uninstallPrivateExtension(context: Context, pkgName: String) {
    File(getPrivateExtensionDir(context), "$pkgName.$PRIVATE_EXTENSION_EXTENSION").delete()
}
