package tachiyomi.core.common.preference

/** Factory of typed [Preference]s over one key-value store. */
public interface PreferenceStore {

    /** A string preference. */
    public fun getString(key: String, defaultValue: String = ""): Preference<String>

    /** A long preference. */
    public fun getLong(key: String, defaultValue: Long = 0): Preference<Long>

    /** An int preference. */
    public fun getInt(key: String, defaultValue: Int = 0): Preference<Int>

    /** A float preference. */
    public fun getFloat(key: String, defaultValue: Float = 0f): Preference<Float>

    /** A boolean preference. */
    public fun getBoolean(key: String, defaultValue: Boolean = false): Preference<Boolean>

    /** A string-set preference. */
    public fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Preference<Set<String>>

    /** An object preference converted to and from a string. */
    public fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T>

    /** An object preference converted to and from an int. */
    public fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T>

    /** An object-set preference converted to and from strings. */
    public fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>>

    /** Every stored key and raw value. */
    public fun getAll(): Map<String, *>
}

/** A long-array preference stored as a comma-separated string. */
public fun PreferenceStore.getLongArray(
    key: String,
    defaultValue: List<Long>,
): Preference<List<Long>> {
    return getObjectFromString(
        key = key,
        defaultValue = defaultValue,
        serializer = { it.joinToString(",") },
        deserializer = { it.split(",").mapNotNull { l -> l.toLongOrNull() } },
    )
}

/** An enum preference stored by name. */
public inline fun <reified T : Enum<T>> PreferenceStore.getEnum(
    key: String,
    defaultValue: T,
): Preference<T> {
    return getObjectFromString(
        key = key,
        defaultValue = defaultValue,
        serializer = { it.name },
        deserializer = {
            try {
                enumValueOf(it)
            } catch (_: IllegalArgumentException) {
                defaultValue
            }
        },
    )
}

/** An enum-set preference stored by names. */
public inline fun <reified T : Enum<T>> PreferenceStore.getEnumSet(
    key: String,
    defaultValue: Set<T>,
): Preference<Set<T>> {
    return getObjectSetFromStringSet(
        key = key,
        defaultValue = defaultValue,
        serializer = { it.name },
        deserializer = {
            try {
                enumValueOf<T>(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        },
    )
}
