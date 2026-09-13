package xyz.nulldev.ts.api.http.serializer

import eu.kanade.tachiyomi.source.model.Filter
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

public interface Serializer<in T : Filter<out Any?>> {
    public fun JsonObjectBuilder.serialize(filter: T) {}
    public fun deserialize(json: JsonObject, filter: T) {}

    /**
     * Automatic two-way mappings between fields and JSON
     */
    public fun mappings(): List<Pair<String, KProperty1<in T, *>>> = emptyList()

    public val serializer: FilterSerializer
    public val type: String
    public val clazz: KClass<in T>
}

public class HeaderSerializer(override val serializer: FilterSerializer) : Serializer<Filter.Header> {
    override val type: String = "HEADER"
    override val clazz: KClass<Filter.Header> = Filter.Header::class

    override fun mappings(): List<Pair<String, KProperty1<in Filter.Header, *>>> = listOf(
        Pair(NAME, Filter.Header::name),
    )

    public companion object {
        public const val NAME: String = "name"
    }
}

public class SeparatorSerializer(override val serializer: FilterSerializer) : Serializer<Filter.Separator> {
    override val type: String = "SEPARATOR"
    override val clazz: KClass<Filter.Separator> = Filter.Separator::class

    override fun mappings(): List<Pair<String, KProperty1<in Filter.Separator, *>>> = listOf(
        Pair(NAME, Filter.Separator::name),
    )

    public companion object {
        public const val NAME: String = "name"
    }
}

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

    public companion object {
        public const val NAME: String = "name"
        public const val VALUES: String = "values"
        public const val STATE: String = "state"
    }
}

public class TextSerializer(override val serializer: FilterSerializer) : Serializer<Filter.Text> {
    override val type: String = "TEXT"
    override val clazz: KClass<Filter.Text> = Filter.Text::class

    override fun mappings(): List<Pair<String, KProperty1<in Filter.Text, *>>> = listOf(
        Pair(NAME, Filter.Text::name),
        Pair(STATE, Filter.Text::state),
    )

    public companion object {
        public const val NAME: String = "name"
        public const val STATE: String = "state"
    }
}

public class CheckboxSerializer(override val serializer: FilterSerializer) : Serializer<Filter.CheckBox> {
    override val type: String = "CHECKBOX"
    override val clazz: KClass<Filter.CheckBox> = Filter.CheckBox::class

    override fun mappings(): List<Pair<String, KProperty1<in Filter.CheckBox, *>>> = listOf(
        Pair(NAME, Filter.CheckBox::name),
        Pair(STATE, Filter.CheckBox::state),
    )

    public companion object {
        public const val NAME: String = "name"
        public const val STATE: String = "state"
    }
}

public class TriStateSerializer(override val serializer: FilterSerializer) : Serializer<Filter.TriState> {
    override val type: String = "TRISTATE"
    override val clazz: KClass<Filter.TriState> = Filter.TriState::class

    override fun mappings(): List<Pair<String, KProperty1<in Filter.TriState, *>>> = listOf(
        Pair(NAME, Filter.TriState::name),
        Pair(STATE, Filter.TriState::state),
    )

    public companion object {
        public const val NAME: String = "name"
        public const val STATE: String = "state"
    }
}

public class GroupSerializer(override val serializer: FilterSerializer) : Serializer<Filter.Group<Any?>> {
    override val type: String = "GROUP"
    override val clazz: KClass<Filter.Group<*>> = Filter.Group::class

    override fun JsonObjectBuilder.serialize(filter: Filter.Group<Any?>) {
        putJsonArray(STATE) {
            filter.state.forEach {
                add(
                    if (it is Filter<*>) {
                        @Suppress("UNCHECKED_CAST")
                        serializer.serialize(it as Filter<Any?>)
                    } else {
                        JsonNull
                    },
                )
            }
        }
    }

    override fun deserialize(json: JsonObject, filter: Filter.Group<Any?>) {
        json[STATE]!!.jsonArray.forEachIndexed { index, jsonElement ->
            if (jsonElement !is JsonNull) {
                @Suppress("UNCHECKED_CAST")
                serializer.deserialize(filter.state[index] as Filter<Any?>, jsonElement.jsonObject)
            }
        }
    }

    override fun mappings(): List<Pair<String, KProperty1<in Filter.Group<Any?>, *>>> = listOf(
        Pair(NAME, Filter.Group<Any?>::name),
    )

    public companion object {
        public const val NAME: String = "name"
        public const val STATE: String = "state"
    }
}

public class SortSerializer(override val serializer: FilterSerializer) : Serializer<Filter.Sort> {
    override val type: String = "SORT"
    override val clazz: KClass<Filter.Sort> = Filter.Sort::class

    override fun JsonObjectBuilder.serialize(filter: Filter.Sort) {
        // Serialize values
        putJsonArray(VALUES) {
            filter.values.forEach { add(it) }
        }
        // Serialize state
        put(
            STATE,
            filter.state?.let { (index, ascending) ->
                buildJsonObject {
                    put(STATE_INDEX, index)
                    put(STATE_ASCENDING, ascending)
                }
            } ?: JsonNull,
        )
    }

    override fun deserialize(json: JsonObject, filter: Filter.Sort) {
        // Deserialize state
        filter.state = (json[STATE] as? JsonObject)?.let {
            Filter.Sort.Selection(
                it[STATE_INDEX]!!.jsonPrimitive.int,
                it[STATE_ASCENDING]!!.jsonPrimitive.boolean,
            )
        }
    }

    override fun mappings(): List<Pair<String, KProperty1<in Filter.Sort, *>>> = listOf(
        Pair(NAME, Filter.Sort::name),
    )

    public companion object {
        public const val NAME: String = "name"
        public const val VALUES: String = "values"
        public const val STATE: String = "state"

        public const val STATE_INDEX: String = "index"
        public const val STATE_ASCENDING: String = "ascending"
    }
}

public class AutoCompleteSerializer(override val serializer: FilterSerializer) : Serializer<Filter.AutoComplete> {
    override val type: String = "AUTOCOMPLETE"
    override val clazz: KClass<Filter.AutoComplete> = Filter.AutoComplete::class

    override fun JsonObjectBuilder.serialize(filter: Filter.AutoComplete) {
        // Serialize values to JSON
        putJsonArray(STATE) {
            filter.state.forEach { add(it) }
        }
    }

    override fun deserialize(json: JsonObject, filter: Filter.AutoComplete) {
        // Deserialize state
        filter.state = json[STATE]!!.jsonArray.map {
            it.jsonPrimitive.content
        }
    }

    override fun mappings(): List<Pair<String, KProperty1<in Filter.AutoComplete, *>>> = listOf(
        Pair(NAME, Filter.AutoComplete::name),
    )

    public companion object {
        public const val NAME: String = "name"
        public const val STATE: String = "state"
    }
}
