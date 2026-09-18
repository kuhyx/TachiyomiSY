package xyz.nulldev.ts.api.http.serializer

import eu.kanade.tachiyomi.source.model.Filter
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

internal const val JAVA_STRING: String = "java.lang.String"
internal const val JAVA_INTEGER: String = "java.lang.Integer"
internal const val JAVA_BOOLEAN: String = "java.lang.Boolean"

/** A [Filter.Select] with every constructor argument spelled out. */
internal class FixtureSelect<V>(name: String, values: Array<V>, state: Int) : Filter.Select<V>(name, values, state)

/** A [Filter.Select] that relies on the default initial state. */
internal class DefaultSelect<V>(name: String, values: Array<V>) : Filter.Select<V>(name, values)

internal class FixtureText(name: String, state: String) : Filter.Text(name, state)

internal class DefaultText(name: String) : Filter.Text(name)

internal class FixtureCheckBox(name: String, state: Boolean) : Filter.CheckBox(name, state)

internal class DefaultCheckBox(name: String) : Filter.CheckBox(name)

internal class FixtureTriState(name: String, state: Int) : Filter.TriState(name, state)

internal class DefaultTriState(name: String) : Filter.TriState(name)

/** A group whose state may mix nested filters with plain values. */
internal class FixtureGroup(name: String, state: List<Any?>) : Filter.Group<Any?>(name, state)

internal class FixtureSort(name: String, values: Array<String>, state: Filter.Sort.Selection?) :
    Filter.Sort(name, values, state)

internal class DefaultSort(name: String, values: Array<String>) : Filter.Sort(name, values)

internal class FixtureAutoComplete(
    name: String,
    hint: String,
    values: List<String>,
    skipAutoFillTags: List<String>,
    validPrefixes: List<String>,
    state: List<String>,
) : Filter.AutoComplete(
    name = name,
    hint = hint,
    values = values,
    skipAutoFillTags = skipAutoFillTags,
    validPrefixes = validPrefixes,
    state = state,
)

internal class DefaultAutoComplete(name: String, hint: String, values: List<String>, state: List<String>) :
    Filter.AutoComplete(name = name, hint = hint, values = values, state = state)

/** Supplies the skip list but leaves the prefix list to its default. */
internal class SkipOnlyAutoComplete(name: String, skipAutoFillTags: List<String>) :
    Filter.AutoComplete(
        name = name,
        hint = "",
        values = emptyList(),
        skipAutoFillTags = skipAutoFillTags,
        state = emptyList(),
    )

/**
 * The erased-type view [FilterSerializer] works on; goes through `filterIsInstance` exactly like the
 * serializer does so no unchecked cast is needed in tests.
 */
internal fun Filter<*>.erased(): Filter<Any?> = listOf<Filter<*>>(this).filterIsInstance<Filter<Any?>>().single()

/** The JSON [FilterSerializer] writes for a filter whose serialized fields all come from its mappings. */
internal fun mappedJson(type: String, vararg fields: Triple<String, String, String>): JsonObject = buildJsonObject {
    fields.forEach { (key, value, _) -> put(key, value) }
    putJsonObject(FilterSerializer.CLASS_MAPPINGS) {
        fields.forEach { (key, _, className) -> put(key, className) }
    }
    put(FilterSerializer.TYPE, type)
}

/** [this] with one extra top-level entry, for filters that also write their own fields. */
internal fun JsonObject.withField(key: String, value: JsonElement): JsonObject = JsonObject(this + (key to value))
