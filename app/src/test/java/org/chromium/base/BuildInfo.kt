package org.chromium.base

import android.content.Context

/**
 * Stands in for Chromium's own `BuildInfo` on the call stack: WebView reads the package name
 * from `getAll`, which is the frame the app's getPackageName override looks for.
 */
internal object BuildInfo {
    fun getAll(context: Context): String = context.packageName

    fun other(context: Context): String = context.packageName
}
