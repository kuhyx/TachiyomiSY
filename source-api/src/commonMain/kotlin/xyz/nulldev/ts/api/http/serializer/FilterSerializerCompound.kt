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

/** JSON conversion of [Filter.Group]. */
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

    /** JSON keys. */
    public companion object {
        /** Key of the filter name. */
        public const val NAME: String = "name"

        /** Key of the state. */
        public const val STATE: String = STATE_KEY
    }
}

/** JSON conversion of [Filter.Sort]. */
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

    /** JSON keys. */
    public companion object {
        /** Key of the filter name. */
        public const val NAME: String = "name"

        /** Key of the option list. */
        public const val VALUES: String = "values"

        /** Key of the state. */
        public const val STATE: String = STATE_KEY

        /** Key of the selected sort index. */
        public const val STATE_INDEX: String = "index"

        /** Key of the sort direction. */
        public const val STATE_ASCENDING: String = "ascending"
    }
}

/** JSON conversion of [Filter.AutoComplete]. */
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

    /** JSON keys. */
    public companion object {
        /** Key of the filter name. */
        public const val NAME: String = "name"

        /** Key of the state. */
        public const val STATE: String = STATE_KEY
    }
}
