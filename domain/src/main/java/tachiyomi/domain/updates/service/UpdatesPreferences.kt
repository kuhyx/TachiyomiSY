package tachiyomi.domain.updates.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum

/**
 * The filters of the updates screen. Each tri-state keeps only matching entries when
 * enabled, only non-matching ones when inverted, and everything when disabled (the default).
 */
public class UpdatesPreferences(
    preferenceStore: PreferenceStore,
) {

    /** Filter on whether the chapter is downloaded. */
    public val filterDownloaded: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_downloaded",
        TriState.DISABLED,
    )

    /** Filter on whether the chapter is unread. */
    public val filterUnread: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_unread",
        TriState.DISABLED,
    )

    /** Filter on whether the chapter is started but not finished. */
    public val filterStarted: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_started",
        TriState.DISABLED,
    )

    /** Filter on whether the chapter is bookmarked. */
    public val filterBookmarked: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_updates_bookmarked",
        TriState.DISABLED,
    )

    /** Whether chapters from scanlators the manga excludes are hidden; off by default. */
    public val filterExcludedScanlators: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_filter_updates_hide_excluded_scanlators",
        false,
    )
}
