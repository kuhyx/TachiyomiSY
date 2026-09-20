@file:Suppress("PackageDirectoryMismatch")

package androidx.preference

/**
 * Returns package-private [EditTextPreference.getOnBindEditTextListener]
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
internal fun EditTextPreference.getOnBindEditTextListener(): EditTextPreference.OnBindEditTextListener? =
    onBindEditTextListener
