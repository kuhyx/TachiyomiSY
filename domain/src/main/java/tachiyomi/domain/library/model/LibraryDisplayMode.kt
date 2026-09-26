package tachiyomi.domain.library.model

/** How the library grid lays out its entries, stored in preferences as the [serialize] name. */
public sealed interface LibraryDisplayMode {

    /** Small covers with the title overlaid. */
    public data object CompactGrid : LibraryDisplayMode

    /** Covers with the title underneath. */
    public data object ComfortableGrid : LibraryDisplayMode

    /** One row per entry. */
    public data object List : LibraryDisplayMode

    /** Covers only, no title. */
    public data object CoverOnlyGrid : LibraryDisplayMode

    /** Adapter for `PreferenceStore.getObjectFromString`. */
    public object Serializer {
        /** See [LibraryDisplayMode.deserialize]. */
        public fun deserialize(serialized: String): LibraryDisplayMode = LibraryDisplayMode.deserialize(serialized)

        /** See [LibraryDisplayMode.serialize]. */
        public fun serialize(value: LibraryDisplayMode): String = value.serialize()
    }

    /** The stored name, parsed back by [deserialize]. */
    public fun serialize(): String = when (this) {
        ComfortableGrid -> "COMFORTABLE_GRID"
        CompactGrid -> "COMPACT_GRID"
        CoverOnlyGrid -> "COVER_ONLY_GRID"
        List -> "LIST"
    }

    /** Every mode, the default, and the parser for the stored name. */
    public companion object {
        /** Every mode, in menu order. */
        public val values: Set<LibraryDisplayMode> by lazy { setOf(CompactGrid, ComfortableGrid, List, CoverOnlyGrid) }

        /**
         * [CompactGrid]. A getter, not a stored field: loading [CompactGrid] first initializes this
         * interface (it has a default method) while `CompactGrid.INSTANCE` is still unset, and a
         * stored field captured that null for the life of the class loader.
         */
        public val default: LibraryDisplayMode get() = CompactGrid

        /** Parses the [serialize] form; an unknown name yields [default]. */
        public fun deserialize(serialized: String): LibraryDisplayMode {
            return when (serialized) {
                "COMFORTABLE_GRID" -> ComfortableGrid
                "COMPACT_GRID" -> CompactGrid
                "COVER_ONLY_GRID" -> CoverOnlyGrid
                "LIST" -> List
                else -> default
            }
        }
    }
}
