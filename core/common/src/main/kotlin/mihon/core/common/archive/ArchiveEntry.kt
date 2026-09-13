package mihon.core.common.archive

/**
 * One entry of an archive as listed by libarchive.
 *
 * @property name the entry path inside the archive.
 * @property isFile false for directories.
 * @property isEncrypted true when the entry is password protected.
 */
public data class ArchiveEntry(
    public val name: String,
    public val isFile: Boolean,
    public val isEncrypted: Boolean,
)
