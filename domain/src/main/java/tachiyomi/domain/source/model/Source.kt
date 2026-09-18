package tachiyomi.domain.source.model

/**
 * A source row of the browse and filter screens: the extension's identity plus the user's
 * pin, "last used", category and data-saver state layered on top. The sources tab shows one
 * copy per placement, so the same source can appear as its main row, its "last used" row and
 * once per category.
 *
 * @property id Id of the source; unique per extension source.
 * @property lang Language code of the source; empty for language-less (local) sources.
 * @property name Display name of the source.
 * @property supportsLatest Whether the source has a "latest updates" listing.
 * @property isStub Whether the source is not installed and only known by id ([StubSource]).
 * @property pin The user's pin bits for this row.
 * @property isUsedLast Whether this row is the "last used" copy shown at the top of the list.
 * @property category Category this row is the copy for, or null for the main row.
 * @property isExcludedFromDataSaver Whether the data-saver proxy is bypassed for this source.
 * @property categories Every category the user put the source in.
 */
public data class Source(
    val id: Long,
    val lang: String,
    val name: String,
    val supportsLatest: Boolean,
    val isStub: Boolean,
    val pin: Pins = Pins.unpinned,
    val isUsedLast: Boolean = false,
    // SY -->
    val category: String? = null,
    val isExcludedFromDataSaver: Boolean = false,
    val categories: Set<String> = emptySet(),
    // SY <--
) {

    /** [name] followed by the upper-cased [lang] in brackets, or just [name] when there is no language. */
    val visualName: String
        get() = if (lang.isEmpty()) name else "$name (${lang.uppercase()})"

    /** A list key that stays unique across the main, "last used" and per-category copies of one source. */
    val key: () -> String = {
        when {
            isUsedLast -> "$id-lastused"
            // SY -->
            category != null -> "$id-$category"
            // SY <--
            else -> "$id"
        }
    }
}
