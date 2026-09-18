package android.miui

/**
 * Test stand-in for MIUI's hidden `android.miui.AppOpsUtils`, which `DeviceUtil` reaches by name
 * through reflection. Throws by default (a non-MIUI device has no such class), so a test opts in
 * by replacing [xOptMode].
 */
internal object AppOpsUtils {
    /** What [isXOptMode] returns, or throws. */
    var xOptMode: () -> Boolean = { throw UnsupportedOperationException("not MIUI") }

    @JvmStatic
    fun isXOptMode(): Boolean = xOptMode()
}
