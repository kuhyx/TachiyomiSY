package eu.kanade.tachiyomi.util.system

import android.os.Build
import com.google.android.material.color.DynamicColors

internal val DeviceUtil.isDynamicColorAvailable by lazy {
    DynamicColors.isDynamicColorAvailable() || (DeviceUtil.isSamsung && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
}
