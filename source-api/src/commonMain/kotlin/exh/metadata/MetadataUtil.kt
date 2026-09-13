package exh.metadata

import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/** Metadata helpers shared by the SY sources. */
public object MetadataUtil {
    private const val KB_FACTOR: Long = 1000
    private const val KIB_FACTOR: Long = 1024
    private const val MB_FACTOR = 1000 * KB_FACTOR
    private const val MIB_FACTOR = 1024 * KIB_FACTOR
    private const val GB_FACTOR = 1000 * MB_FACTOR
    private const val GIB_FACTOR = 1024 * MIB_FACTOR

    /** Title suffixes a site uses to mark an unfinished gallery. */
    public val ONGOING_SUFFIX: Array<String> = arrayOf(
        "[ongoing]",
        "(ongoing)",
        "{ongoing}",
        "<ongoing>",
        "ongoing",
        "[incomplete]",
        "(incomplete)",
        "{incomplete}",
        "<incomplete>",
        "incomplete",
        "[wip]",
        "(wip)",
        "{wip}",
        "<wip>",
        "wip",
    )

    /** The date format E-Hentai prints, `yyyy-MM-dd HH:mm`. */
    public val EX_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** [bytes] as `1.5 MB` (SI, [si] true) or `1.5 MiB` (binary). */
    public fun humanReadableByteCount(bytes: Long, si: Boolean): String {
        val unit = if (si) KB_FACTOR else KIB_FACTOR
        if (bytes < unit) return "$bytes B"
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
        return String.format(Locale.ROOT, "%.1f %sB", bytes / unit.toDouble().pow(exp.toDouble()), pre)
    }

    /** The inverse of [humanReadableByteCount] for `KB`..`GiB`; null for any other unit. */
    public fun parseHumanReadableByteCount(bytes: String): Double? {
        val ret = bytes.substringBefore(' ').toDouble()
        return when (bytes.substringAfter(' ')) {
            "GB" -> ret * GB_FACTOR
            "GiB" -> ret * GIB_FACTOR
            "MB" -> ret * MB_FACTOR
            "MiB" -> ret * MIB_FACTOR
            "KB" -> ret * KB_FACTOR
            "KiB" -> ret * KIB_FACTOR
            else -> null
        }
    }
}
