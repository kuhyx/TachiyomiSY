package exh.pref

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

public class DelegateSourcePreferences(
    private val preferenceStore: PreferenceStore,
) {

    public val delegateSources: Preference<Boolean> = preferenceStore.getBoolean("eh_delegate_sources", true)

    public val useJapaneseTitle: Preference<Boolean> = preferenceStore.getBoolean("use_jp_title", false)
}
