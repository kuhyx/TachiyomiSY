package mihon.domain.migration.models

internal enum class MigrationFlag(val flag: Int) {
    CHAPTER(flag = 0b00001),
    CATEGORY(flag = 0b00010),

    // 0b00100 was used for manga trackers
    CUSTOM_COVER(flag = 0b01000),
    NOTES(flag = 0b100000),
    REMOVE_DOWNLOAD(flag = 0b10000),
    ;

    companion object {
        fun fromBit(bit: Int): Set<MigrationFlag> {
            return buildSet {
                entries.forEach { entry ->
                    if (bit and entry.flag != 0) add(entry)
                }
            }
        }

        fun toBit(flags: Set<MigrationFlag>): Int {
            return flags.map { it.flag }
                .reduceOrNull { acc, mask -> acc or mask }
                ?: 0
        }
    }
}
