package eu.kanade.tachiyomi.extension.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.extension.installer.Installer
import eu.kanade.tachiyomi.extension.installer.PackageInstallerInstaller
import eu.kanade.tachiyomi.extension.installer.ShizukuInstaller
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.Companion.EXTRA_DOWNLOAD_ID
import eu.kanade.tachiyomi.util.system.getSerializableExtraCompat
import eu.kanade.tachiyomi.util.system.notificationBuilder
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR

private const val PROGRESS_MAX = 100

internal class ExtensionInstallService : Service() {

    private var installer: Installer? = null

    override fun onCreate() {
        val notification = notificationBuilder(Notifications.CHANNEL_EXTENSIONS_UPDATE) {
            setSmallIcon(R.drawable.ic_tachi)
            setAutoCancel(false)
            setOngoing(true)
            setShowWhen(false)
            setContentTitle(stringResource(MR.strings.ext_install_service_notif))
            setProgress(PROGRESS_MAX, PROGRESS_MAX, true)
        }.build()
        startForeground(Notifications.ID_EXTENSION_INSTALLER, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!enqueue(intent)) stopSelf()
        return START_NOT_STICKY
    }

    // False when the intent is incomplete or names an installer this service cannot drive.
    private fun enqueue(intent: Intent?): Boolean {
        val uri = intent?.data ?: return false
        val id = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1).takeIf { it != -1L } ?: return false
        val installerUsed = intent.getSerializableExtraCompat<BasePreferences.ExtensionInstaller>(EXTRA_INSTALLER)
            ?: return false
        val installer = installer ?: createInstaller(installerUsed) ?: return false
        this.installer = installer
        installer.addToQueue(id, uri)
        return true
    }

    private fun createInstaller(installerUsed: BasePreferences.ExtensionInstaller): Installer? = when (installerUsed) {
        BasePreferences.ExtensionInstaller.PACKAGEINSTALLER -> {
            PackageInstallerInstaller(this)
        }
        BasePreferences.ExtensionInstaller.SHIZUKU -> {
            ShizukuInstaller(this)
        }
        else -> {
            logcat(LogPriority.ERROR) { "Not implemented for installer $installerUsed" }
            null
        }
    }

    override fun onDestroy() {
        installer?.onDestroy()
        installer = null
    }

    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_INSTALLER = "EXTRA_INSTALLER"

        fun getIntent(
            context: Context,
            downloadId: Long,
            uri: Uri,
            installer: BasePreferences.ExtensionInstaller,
        ): Intent {
            return Intent(context, ExtensionInstallService::class.java)
                .setDataAndType(uri, ExtensionInstaller.APK_MIME)
                .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                .putExtra(EXTRA_INSTALLER, installer)
        }
    }
}
