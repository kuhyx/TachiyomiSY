package tachiyomi.domain.source.model

/**
 * A source with the number of its manga that are not in the library; what the
 * "clear database" screen lists.
 *
 * @property source The source, a stub when it is not installed.
 * @property count Number of non-library manga stored for [source].
 */
public data class SourceWithCount(
    val source: Source,
    val count: Long,
) {

    /** [source]'s id. */
    val id: Long
        get() = source.id

    /** [source]'s name. */
    val name: String
        get() = source.name
}
