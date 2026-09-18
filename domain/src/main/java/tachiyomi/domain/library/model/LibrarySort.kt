package tachiyomi.domain.library.model

import tachiyomi.domain.category.model.Category

/**
 * The library's sort key and direction, packed into a category's flags
 * (bits 2-5 the [Type], bit 6 the [Direction]) and stored in preferences as
 * `"TYPE_NAME,DIRECTION_NAME"` (see [serialize]).
 *
 * @property type What the library is sorted by.
 * @property direction Ascending or descending.
 */
public data class LibrarySort(
    val type: Type,
    val direction: Direction,
) : FlagWithMask {

    override val flag: Long
        get() = type + direction

    override val mask: Long
        get() = type.mask or direction.mask

    /** Whether [direction] is [Direction.Ascending]. */
    val isAscending: Boolean
        get() = direction == Direction.Ascending

    /** What the library is sorted by; [flag] is the value in the four type bits. */
    public sealed class Type(
        override val flag: Long,
    ) : FlagWithMask {

        override val mask: Long = TYPE_MASK

        /** By title. */
        public data object Alphabetical : Type(ALPHABETICAL)

        /** By the time a chapter was last read. */
        public data object LastRead : Type(LAST_READ)

        /** By the time the source last reported a change. */
        public data object LastUpdate : Type(LAST_UPDATE)

        /** By unread chapter count. */
        public data object UnreadCount : Type(UNREAD_COUNT)

        /** By total chapter count. */
        public data object TotalChapters : Type(TOTAL_CHAPTERS)

        /** By the upload date of the newest chapter. */
        public data object LatestChapter : Type(LATEST_CHAPTER)

        /** By the time chapters were last fetched. */
        public data object ChapterFetchDate : Type(CHAPTER_FETCH_DATE)

        /** By the time the manga was added to the library. */
        public data object DateAdded : Type(DATE_ADDED)

        /** By the mean tracker score. */
        public data object TrackerMean : Type(TRACKER_MEAN)

        /** Shuffled with the stored seed. */
        public data object Random : Type(RANDOM)

        // SY -->

        /** By the tags listed in the library preferences. */
        public data object TagList : Type(TAG_LIST)
        // SY <--

        /** The bit values of each type and the reverse lookup. */
        public companion object {
            private const val TYPE_MASK = 0b00111100L
            private const val ALPHABETICAL = 0b00000000L
            private const val LAST_READ = 0b00000100L
            private const val LAST_UPDATE = 0b00001000L
            private const val UNREAD_COUNT = 0b00001100L
            private const val TOTAL_CHAPTERS = 0b00010000L
            private const val LATEST_CHAPTER = 0b00010100L
            private const val CHAPTER_FETCH_DATE = 0b00011000L
            private const val DATE_ADDED = 0b00011100L
            private const val TRACKER_MEAN = 0b00100000L
            private const val RANDOM = 0b00111100L
            private const val TAG_LIST = 0b00100100L

            /** The type packed in [flag], or the default type when the bits match none. */
            public fun valueOf(flag: Long): Type =
                types.find { type -> type.flag == flag and type.mask } ?: default.type
        }
    }

    /** Sort direction; [flag] is the value of the direction bit. */
    public sealed class Direction(
        override val flag: Long,
    ) : FlagWithMask {

        override val mask: Long = DIRECTION_MASK

        /** Smallest first. */
        public data object Ascending : Direction(ASCENDING)

        /** Largest first. */
        public data object Descending : Direction(DESCENDING)

        /** The bit values of each direction and the reverse lookup. */
        public companion object {
            private const val DIRECTION_MASK = 0b01000000L
            private const val ASCENDING = 0b01000000L
            private const val DESCENDING = 0b00000000L

            /** The direction packed in [flag], or the default direction when the bit matches none. */
            public fun valueOf(flag: Long): Direction =
                directions.find { direction -> direction.flag == flag and direction.mask } ?: default.direction
        }
    }

    /** Adapter for `PreferenceStore.getObjectFromString`. */
    public object Serializer {
        /** See [LibrarySort.deserialize]. */
        public fun deserialize(serialized: String): LibrarySort = LibrarySort.deserialize(serialized)

        /** See [LibrarySort.serialize]. */
        public fun serialize(value: LibrarySort): String = value.serialize()
    }

    /** The known types and directions, the default, and the parsers for flags and stored names. */
    public companion object {
        /** Every [Type], in menu order. */
        public val types: Set<Type> by lazy { typeNames.keys }

        /** Both directions. */
        public val directions: Set<Direction> by lazy { setOf(Direction.Ascending, Direction.Descending) }

        /** Alphabetical, ascending. */
        public val default: LibrarySort = LibrarySort(Type.Alphabetical, Direction.Ascending)

        /** The stored name of each type, in menu order; also the source of [types]. */
        internal val typeNames: Map<Type, String> = linkedMapOf(
            Type.Alphabetical to "ALPHABETICAL",
            Type.LastRead to "LAST_READ",
            Type.LastUpdate to "LAST_MANGA_UPDATE",
            Type.UnreadCount to "UNREAD_COUNT",
            Type.TotalChapters to "TOTAL_CHAPTERS",
            Type.LatestChapter to "LATEST_CHAPTER",
            Type.ChapterFetchDate to "CHAPTER_FETCH_DATE",
            Type.DateAdded to "DATE_ADDED",
            Type.TrackerMean to "TRACKER_MEAN",
            Type.Random to "RANDOM",
            /* SY -->*/ Type.TagList to "TAG_LIST", /* SY <--*/
        )

        /** The sort packed in a category's [flag]; [default] for null. */
        public fun valueOf(flag: Long?): LibrarySort {
            if (flag == null) return default
            return LibrarySort(
                Type.valueOf(flag),
                Direction.valueOf(flag),
            )
        }

        /**
         * Parses the [serialize] form. An empty or malformed value, or an unknown
         * type name, yields [default] (or the default type with the parsed direction).
         */
        public fun deserialize(serialized: String): LibrarySort {
            val values = serialized.split(",")
            if (values.size < 2) return default
            val type = typeNames.entries.find { it.value == values[0] }?.key ?: Type.Alphabetical
            val ascending = if (values[1] == "ASCENDING") Direction.Ascending else Direction.Descending
            return LibrarySort(type, ascending)
        }
    }
}

/** The stored form: `"TYPE_NAME,DIRECTION_NAME"`, parsed by [LibrarySort.deserialize]. */
public fun LibrarySort.serialize(): String {
    val direction = if (direction == LibrarySort.Direction.Ascending) "ASCENDING" else "DESCENDING"
    return "${LibrarySort.typeNames.getValue(type)},$direction"
}

/** The sort packed in this category's flags; the default sort for a null category. */
public val Category?.sort: LibrarySort
    get() = LibrarySort.valueOf(this?.flags)
