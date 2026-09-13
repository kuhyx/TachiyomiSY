package xyz.nulldev.ts.api.http.serializer

import eu.kanade.tachiyomi.source.model.Filter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.putJsonArray
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal const val STATE_KEY: String = "state"

/**
 * Two-way JSON conversion of one [Filter] subtype.
 *
 * @param T the filter type handled.
 */
public interface Serializer<in T : Filter<out Any?>> {
    /** The owning [FilterSerializer], for nested filters. */
    public val serializer: FilterSerializer

    /** The discriminator written as `_type`. */
    public val type: String

    /** The filter class this serializer handles. */
    public val clazz: KClass<in T>

    /** Writes the filter-specific fields; the default writes nothing. */
    public fun JsonObjectBuilder.serialize(filter: T) {}

    /** Reads the filter-specific fields back; the default reads nothing. */
    public fun deserialize(json: JsonObject, filter: T) {}

    /** Automatic two-way mappings between fields and JSON. */
    public fun mappings(): List<Pair<String, KProperty1<in T, *>>> = emptyList()
}

/** JSON conversion of [Filter.Header]. */
public class HeaderSerializer(override val serializer: FilterSerializer) : Serializer<Filter.Header> {
    override val type: String = "HEADER"
    override val clazz: KClass<Filter.Header> = Filter.Header::class

    override fun mappings(): List<Pair<String, KProperty1<in Filter.Header, *>>> = listOf(
        Pair(NAME, Filter.Header::name),
    )

    /** JSON keys. */
    public companion object {
        /** Key of the filter name. */
        public const val NAME: String = "name"
    }
}

/** JSON conversion of [Filter.Separator]. */
public class SeparatorSerializer(override val serializer: FilterSerializer) : Serializer<Filter.Separator> {
    override val type: String = "SEPARATOR"
    override val clazz: KClass<Filter.Separator> = Filter.Separator::class

    override fun mappings(): List<Pair<String, KProperty1<in Filter.Separator, *>>> = listOf(
        Pair(NAME, Filter.Separator::name),
    )

    /** JSON keys. */
    public companion object {
        /** Key of the filter name. */
        public const val NAME: String = "name"
    }
}

/** JSON conversion of [Filter.Select]. */
public class SelectSerializer(override val serializer: FilterSerializer) : Serializer<Filter.Select<Any>> {
    override val type: String = "SELECT"
    override val clazz: KClass<Filter.Select<*>> = Filter.Select::class

    override fun JsonObjectBuilder.serialize(filter: Filter.Select<Any>) {
        // Serialize values to JSON
        putJsonArray(VALUES) {
            filter.values.map {
                it.toString()
            }.forEach { add(it) }
        }
    }

    override fun mappings(): List<Pair<String, KProperty1<in Filter.Select<Any>, *>>> = listOf(
        Pair(NAME, Filter.Select<Any>::name),
        Pair(STATE, Filter.Select<Any>::state),
    )

    /** JSON keys. */
    public companion object {
        /** Key of the filter name. */
        public const val NAME: String = "name"

        /** Key of the option list. */
        public const val VALUES: String = "values"

        /** Key of the state. */
        public const val STATE: String = STATE_KEY
    }
}

/** JSON conversion of [Filter.Text]. */
public class TextSerializer(override val serializer: FilterSerializer) : Serializer<Filter.Text> {
    override val type: String = "TEXT"
    override val clazz: KClass<Filter.Text> = Filter.Text::class

    override fun mappings(): List<Pair<String, KProperty1<in Filter.Text, *>>> = listOf(
        Pair(NAME, Filter.Text::name),
        Pair(STATE, Filter.Text::state),
    )

    /** JSON keys. */
    public companion object {
        /** Key of the filter name. */
        public const val NAME: String = "name"

        /** Key of the state. */
        public const val STATE: String = STATE_KEY
    }
}

/** JSON conversion of [Filter.CheckBox]. */
public class CheckboxSerializer(override val serializer: FilterSerializer) : Serializer<Filter.CheckBox> {
    override val type: String = "CHECKBOX"
    override val clazz: KClass<Filter.CheckBox> = Filter.CheckBox::class

    override fun mappings(): List<Pair<String, KProperty1<in Filter.CheckBox, *>>> = listOf(
        Pair(NAME, Filter.CheckBox::name),
        Pair(STATE, Filter.CheckBox::state),
    )

    /** JSON keys. */
    public companion object {
        /** Key of the filter name. */
        public const val NAME: String = "name"

        /** Key of the state. */
        public const val STATE: String = STATE_KEY
    }
}

/** JSON conversion of [Filter.TriState]. */
public class TriStateSerializer(override val serializer: FilterSerializer) : Serializer<Filter.TriState> {
    override val type: String = "TRISTATE"
    override val clazz: KClass<Filter.TriState> = Filter.TriState::class

    override fun mappings(): List<Pair<String, KProperty1<in Filter.TriState, *>>> = listOf(
        Pair(NAME, Filter.TriState::name),
        Pair(STATE, Filter.TriState::state),
    )

    /** JSON keys. */
    public companion object {
        /** Key of the filter name. */
        public const val NAME: String = "name"

        /** Key of the state. */
        public const val STATE: String = STATE_KEY
    }
}
