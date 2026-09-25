package eu.kanade.tachiyomi.util.system

import android.os.Build
import com.google.android.material.color.DynamicColors

internal val DeviceUtil.isDynamicColorAvailable by lazy {
    dynamicColorAvailable(
        sdkInt = Build.VERSION.SDK_INT,
        isSamsung = DeviceUtil.isSamsung,
        materialAvailable = DynamicColors.isDynamicColorAvailable(),
    )
}

/**
 * Samsung ships dynamic colours from S regardless of what Material reports; every other vendor is
 * asked through [DynamicColors]. Both inputs are read eagerly: the value is resolved once per
 * process, and the Material query is a manufacturer/SDK table lookup.
 */
internal fun dynamicColorAvailable(sdkInt: Int, isSamsung: Boolean, materialAvailable: Boolean): Boolean =
    (sdkInt >= Build.VERSION_CODES.S && isSamsung) || materialAvailable
