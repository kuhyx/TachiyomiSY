package tachiyomi.core.common

/** Intent actions, extras and help URLs shared across modules. */
public object Constants {
    /** Troubleshooting guide. */
    public const val URL_HELP: String = "https://mihon.app/docs/guides/troubleshooting/"

    /** FAQ entry on the upcoming-updates view. */
    public const val URL_HELP_UPCOMING: String = "https://mihon.app/docs/faq/updates/upcoming"

    /** Intent extra carrying a manga id. */
    public const val MANGA_EXTRA: String = "manga"

    /** Class name of the main activity. */
    public const val MAIN_ACTIVITY: String = "eu.kanade.tachiyomi.ui.main.MainActivity"

    // Shortcut actions

    /** Shortcut action opening the library. */
    public const val SHORTCUT_LIBRARY: String = "eu.kanade.tachiyomi.SHOW_LIBRARY"

    /** Shortcut action opening a manga. */
    public const val SHORTCUT_MANGA: String = "eu.kanade.tachiyomi.SHOW_MANGA"

    /** Shortcut action opening updates. */
    public const val SHORTCUT_UPDATES: String = "eu.kanade.tachiyomi.SHOW_RECENTLY_UPDATED"

    /** Shortcut action opening history. */
    public const val SHORTCUT_HISTORY: String = "eu.kanade.tachiyomi.SHOW_RECENTLY_READ"

    /** Shortcut action opening sources. */
    public const val SHORTCUT_SOURCES: String = "eu.kanade.tachiyomi.SHOW_CATALOGUES"

    /** Shortcut action opening extensions. */
    public const val SHORTCUT_EXTENSIONS: String = "eu.kanade.tachiyomi.EXTENSIONS"

    /** Shortcut action opening downloads. */
    public const val SHORTCUT_DOWNLOADS: String = "eu.kanade.tachiyomi.SHOW_DOWNLOADS"
}
