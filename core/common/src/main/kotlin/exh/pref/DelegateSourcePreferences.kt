package exh.pref

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** SY settings for the in-app replacements of extension sources. */
public data class DelegateSourcePreferences(
    private val preferenceStore: PreferenceStore,
) {

    /** Route supported extensions through their in-app replacements. */
    public val delegateSources: Preference<Boolean> = preferenceStore.getBoolean("eh_delegate_sources", true)

    /** Prefer the Japanese title when a gallery has one. */
    public val useJapaneseTitle: Preference<Boolean> = preferenceStore.getBoolean("use_jp_title", false)
}
