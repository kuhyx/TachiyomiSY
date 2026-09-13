package xyz.nulldev.ts.api.http.serializer

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.isSubclassOf

/** Converts a source's [FilterList] to JSON and back, one [Serializer] per filter type. */
public class FilterSerializer {
    private val serializers = listOf<Serializer<*>>(
        // SY -->
        AutoCompleteSerializer(this),
        // SY <--
        HeaderSerializer(this),
        SeparatorSerializer(this),
        SelectSerializer(this),
        TextSerializer(this),
        CheckboxSerializer(this),
        TriStateSerializer(this),
        GroupSerializer(this),
        SortSerializer(this),
    )

    /** Every filter of [filters] as a JSON array. */
    public fun serialize(filters: FilterList): JsonArray = buildJsonArray {
        filters.filterIsInstance<Filter<Any?>>().forEach {
            add(serialize(it))
        }
    }

    /** One [filter] as a JSON object carrying its type and field class names. */
    public fun serialize(filter: Filter<Any?>): JsonObject {
        val serializer = serializers
            .filterIsInstance<Serializer<Filter<Any?>>>()
            .firstOrNull { filter::class.isSubclassOf(it.clazz) }
            ?: throw IllegalArgumentException("Cannot serialize this Filter object!")
        return serializer.let { serializer ->
            buildJsonObject {
                with(serializer) { serialize(filter) }

                val classMappings = mutableListOf<Pair<String, Any>>()

                serializer.mappings().forEach {
                    val res = it.second.get(filter)
                    put(it.first, res.toString())
                    classMappings += it.first to (res?.javaClass?.name ?: "null")
                }

                putJsonObject(CLASS_MAPPINGS) {
                    classMappings.forEach { (t, u) ->
                        put(t, u.toString())
                    }
                }

                put(TYPE, serializer.type)
            }
        }
    }

    /** Applies [json], produced by [serialize], onto [filters] in order. */
    public fun deserialize(filters: FilterList, json: JsonArray) {
        filters.filterIsInstance<Filter<Any?>>().zip(json).forEach { (filter, obj) ->
            deserialize(filter, obj.jsonObject)
        }
    }

    /** Applies one serialized filter [json] onto [filter]. */
    public fun deserialize(filter: Filter<Any?>, json: JsonObject) {
        val type = json[TYPE]!!.jsonPrimitive.content
        val serializer = serializers
            .filterIsInstance<Serializer<Filter<Any?>>>()
            .firstOrNull { it.type == type }
            ?: throw IllegalArgumentException("Cannot deserialize this type!")

        serializer.deserialize(json, filter)

        serializer.mappings().forEach {
            if (it.second is KMutableProperty1) {
                val className = json[CLASS_MAPPINGS]!!.jsonObject[it.first]!!.jsonPrimitive.content
                val res = parsePrimitive(className, json[it.first]!!.jsonPrimitive)
                @Suppress("UNCHECKED_CAST")
                (it.second as KMutableProperty1<in Filter<Any?>, in Any?>).set(filter, res)
            }
        }
    }

    private fun parsePrimitive(className: String, obj: JsonPrimitive): Any? = when (className) {
        Int::class.javaObjectType.name -> obj.int
        Long::class.javaObjectType.name -> obj.long
        Float::class.javaObjectType.name -> obj.float
        Double::class.javaObjectType.name -> obj.double
        String::class.javaObjectType.name -> obj.content
        Boolean::class.javaObjectType.name -> obj.boolean
        Byte::class.javaObjectType.name -> obj.content.toByte()
        Short::class.javaObjectType.name -> obj.content.toShort()
        Char::class.javaObjectType.name -> obj.content[0]
        "null" -> null
        else -> throw IllegalArgumentException("Cannot deserialize this type!")
    }

    /** JSON keys shared by every serialized filter. */
    public companion object {
        /** Key of the type discriminator. */
        public const val TYPE: String = "_type"

        /** Key of the field-name to class-name map. */
        public const val CLASS_MAPPINGS: String = "_cmaps"
    }
}
