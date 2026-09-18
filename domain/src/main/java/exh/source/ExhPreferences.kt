package exh.source

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/** Settings of the E-Hentai / ExHentai sources: feature toggles, login cookies and sync. */
public class ExhPreferences(
    private val preferenceStore: PreferenceStore,
) {

    // SY -->
    /** Whether the hentai sources are shown at all. */
    public val isHentaiEnabled: Preference<Boolean> = preferenceStore.getBoolean("eh_is_hentai_enabled", true)

    /** Whether ExHentai (login required) is used instead of E-Hentai. */
    public val enableExhentai: Preference<Boolean> = preferenceStore.getBoolean(privateKey("enable_exhentai"), false)

    /** Image quality requested from the site. */
    public val imageQuality: Preference<String> = preferenceStore.getString("ehentai_quality", "auto")

    /** Hentai@Home usage mode. */
    public val useHentaiAtHome: Preference<Int> = preferenceStore.getInt("eh_enable_hah", 0)

    /** Whether the Japanese title is preferred over the romanised one. */
    public val useJapaneseTitle: Preference<Boolean> = preferenceStore.getBoolean("use_jp_title", false)

    /** Whether original (unresampled) images are downloaded. */
    public val exhUseOriginalImages: Preference<Boolean> = preferenceStore.getBoolean("eh_useOrigImages", false)

    /** Tag filtering threshold. */
    public val ehTagFilterValue: Preference<Int> = preferenceStore.getInt("eh_tag_filtering_value", 0)

    /** Tag watching threshold. */
    public val ehTagWatchingValue: Preference<Int> = preferenceStore.getInt("eh_tag_watching_value", 0)

    // EH Cookies

    /** `ipb_member_id` login cookie. */
    public val memberIdVal: Preference<String> = preferenceStore.getString(privateKey("eh_ipb_member_id"), "")

    /** `ipb_pass_hash` login cookie. */
    public val passHashVal: Preference<String> = preferenceStore.getString(privateKey("eh_ipb_pass_hash"), "")

    /** `igneous` login cookie. */
    public val igneousVal: Preference<String> = preferenceStore.getString(privateKey("eh_igneous"), "")

    /** Settings profile id on E-Hentai; -1 for none. */
    public val ehSettingsProfile: Preference<Int> = preferenceStore.getInt(privateKey("eh_ehSettingsProfile"), -1)

    /** Settings profile id on ExHentai; -1 for none. */
    public val exhSettingsProfile: Preference<Int> = preferenceStore.getInt(privateKey("eh_exhSettingsProfile"), -1)

    /** `sk` settings-key cookie. */
    public val exhSettingsKey: Preference<String> = preferenceStore.getString(privateKey("eh_settingsKey"), "")

    /** `s` session cookie. */
    public val exhSessionCookie: Preference<String> = preferenceStore.getString(privateKey("eh_sessionCookie"), "")

    /** `hath_perks` cookie. */
    public val exhHathPerksCookies: Preference<String> = preferenceStore.getString(privateKey("eh_hathPerksCookie"), "")

    /** Whether the favourites-sync introduction is still to be shown. */
    public val exhShowSyncIntro: Preference<Boolean> = preferenceStore.getBoolean("eh_show_sync_intro", true)

    /** Whether favourites sync only reads from the site. */
    public val exhReadOnlySync: Preference<Boolean> = preferenceStore.getBoolean("eh_sync_read_only", false)

    /** Whether favourites sync tolerates conflicts. */
    public val exhLenientSync: Preference<Boolean> = preferenceStore.getBoolean("eh_lenient_sync", false)

    /** Whether uploading settings to the site still warns first. */
    public val exhShowSettingsUploadWarning: Preference<Boolean> =
        preferenceStore.getBoolean("eh_showSettingsUploadWarning2", true)

    /** Verbosity of the EH log. */
    public val logLevel: Preference<Int> = preferenceStore.getInt("eh_log_level", 0)

    /** Hours between automatic gallery updates. */
    public val exhAutoUpdateFrequency: Preference<Int> = preferenceStore.getInt("eh_auto_update_frequency", 1)

    /** Device conditions the automatic update waits for. */
    public val exhAutoUpdateRequirements: Preference<Set<String>> =
        preferenceStore.getStringSet("eh_auto_update_restrictions", emptySet())

    /** Serialized statistics of the last automatic update; app state. */
    public val exhAutoUpdateStats: Preference<String> =
        preferenceStore.getString(Preference.appStateKey("eh_auto_update_stats"), "")

    /** Default state of the "watched" toggle in the browse filters. */
    public val exhWatchedListDefaultState: Preference<Boolean> =
        preferenceStore.getBoolean("eh_watched_list_default_state", false)

    /** Language filter grid, one `original*translated*rewrite` triple per line. */
    public val exhSettingsLanguages: Preference<String> = preferenceStore.getString(
        "eh_settings_languages",
        """
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
            |false*false*false
        """.trimMargin(),
    )

    /** Category filter, one flag per site category. */
    public val exhEnabledCategories: Preference<String> = preferenceStore.getString(
        "eh_enabled_categories",
        "false,false,false,false,false,false,false,false,false,false",
    )

    /** Whether the enhanced gallery view is used. */
    public val enhancedEHentaiView: Preference<Boolean> = preferenceStore.getBoolean("enhanced_e_hentai_view", true)

    /** The settings-profile preference of the site in use: ExHentai's or E-Hentai's. */
    public fun settingsProfile(exh: Boolean): Preference<Int> = if (exh) exhSettingsProfile else ehSettingsProfile

    private fun privateKey(key: String): String = Preference.privateKey(key)
}
