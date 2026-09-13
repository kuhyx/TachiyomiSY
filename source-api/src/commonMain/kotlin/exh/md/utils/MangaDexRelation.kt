package exh.md.utils

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.sy.SYMR

/**
 * How a related MangaDex title relates to the current one.
 *
 * @property res the label resource.
 * @property mdString the MangaDex API value; null for the app-side [SIMILAR] group.
 */
public enum class MangaDexRelation(public val res: StringResource, public val mdString: String?) {
    /** Similar titles suggested by the app, not a MangaDex relation. */
    SIMILAR(SYMR.strings.relation_similar, null),

    /** A monochrome edition. */
    MONOCHROME(SYMR.strings.relation_monochrome, "monochrome"),

    /** The main story this is a side story of. */
    MAIN_STORY(SYMR.strings.relation_main_story, "main_story"),

    /** The work this was adapted from. */
    ADAPTED_FROM(SYMR.strings.relation_adapted_from, "adapted_from"),

    /** The work this is based on. */
    BASED_ON(SYMR.strings.relation_based_on, "based_on"),

    /** The story before this one. */
    PREQUEL(SYMR.strings.relation_prequel, "prequel"),

    /** A side story. */
    SIDE_STORY(SYMR.strings.relation_side_story, "side_story"),

    /** A doujinshi. */
    DOUJINSHI(SYMR.strings.relation_doujinshi, "doujinshi"),

    /** Same franchise. */
    SAME_FRANCHISE(SYMR.strings.relation_same_franchise, "same_franchise"),

    /** Shared universe. */
    SHARED_UNIVERSE(SYMR.strings.relation_shared_universe, "shared_universe"),

    /** The story after this one. */
    SEQUEL(SYMR.strings.relation_sequel, "sequel"),

    /** A spin-off. */
    SPIN_OFF(SYMR.strings.relation_spin_off, "spin_off"),

    /** An alternate story. */
    ALTERNATE_STORY(SYMR.strings.relation_alternate_story, "alternate_story"),

    /** The pre-serialization version. */
    PRESERIALIZATION(SYMR.strings.relation_preserialization, "preserialization"),

    /** A colored edition. */
    COLORED(SYMR.strings.relation_colored, "colored"),

    /** The serialized version. */
    SERIALIZATION(SYMR.strings.relation_serialization, "serialization"),

    /** An alternate version. */
    ALTERNATE_VERSION(SYMR.strings.relation_alternate_version, "alternate_version"),
    ;

    /** Lookup from the API value. */
    public companion object {
        /** The relation for a MangaDex [mdString], or null when unknown. */
        public fun fromDex(mdString: String): MangaDexRelation? = entries.find { it.mdString == mdString }
    }
}
