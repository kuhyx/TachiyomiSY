package eu.kanade.tachiyomi

import android.os.Build
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.Printer
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import exh.SY_DEBUG_VERSION
import exh.log.CrashlyticsPrinter
import exh.log.EHLogLevel
import exh.log.EnhancedFilePrinter
import exh.log.xLogD
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale

// EXH
// [debug] is BuildConfig.DEBUG, a per-variant constant: a parameter so both builds' paths run in tests.
internal fun App.setupExhLogging(debug: Boolean = BuildConfig.DEBUG) {
    EHLogLevel.init(this)

    val logLevel = when {
        EHLogLevel.shouldLog(EHLogLevel.EXTREME) -> LogLevel.ALL
        EHLogLevel.shouldLog(EHLogLevel.EXTRA) || debug -> LogLevel.DEBUG
        else -> LogLevel.WARN
    }

    val logConfig = LogConfiguration.Builder()
        .logLevel(logLevel)
        .disableStackTrace()
        .disableBorder()
        .build()

    val printers = mutableListOf<Printer>(AndroidPrinter())

    val logFolder = Injekt.get<StorageManager>().getLogsDirectory()

    if (logFolder != null) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        printers += EnhancedFilePrinter
            .Builder(logFolder) {
                fileNameGenerator = object : DateFileNameGenerator() {
                    override fun generateFileName(logLevel: Int, timestamp: Long): String {
                        return super.generateFileName(
                            logLevel,
                            timestamp,
                        ) + "-${BuildConfig.BUILD_TYPE}.txt"
                    }
                }
                flattener { timeMillis, level, tag, message ->
                    "${dateFormat.format(timeMillis)} ${LogLevel.getShortLevelName(level)}/$tag: $message"
                }
                backupStrategy = NeverBackupStrategy()
            }
    }

    // Install Crashlytics in prod
    if (!debug) {
        printers += CrashlyticsPrinter(LogLevel.ERROR)
    }

    XLog.init(logConfig, FanOutPrinter(printers))

    xLogD("Application booting...")
    xLogD(
        """
            App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE}, ${BuildConfig.COMMIT_SHA}, ${BuildConfig.VERSION_CODE})
            Preview build: $SY_DEBUG_VERSION
            Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            Android build ID: ${Build.DISPLAY}
            Device brand: ${Build.BRAND}
            Device manufacturer: ${Build.MANUFACTURER}
            Device name: ${Build.DEVICE}
            Device model: ${Build.MODEL}
            Device product name: ${Build.PRODUCT}
        """.trimIndent(),
    )
}

/** One xlog [Printer] over several: the vararg `XLog.init` would otherwise need a spread copy. */
private class FanOutPrinter(private val printers: List<Printer>) : Printer {
    override fun println(logLevel: Int, tag: String, msg: String) {
        printers.forEach { it.println(logLevel, tag, msg) }
    }
}
