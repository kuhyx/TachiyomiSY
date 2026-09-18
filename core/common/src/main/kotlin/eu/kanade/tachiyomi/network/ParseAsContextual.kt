// detekt 1.23.8 parses a Kotlin 2.2 context parameter as an expression: the KDoc before it is
// invisible to UndocumentedPublicFunction, ktlint forbids a KDoc after it and orders annotations
// before it, so a function-level @Suppress is invisible too, and an import used only by the
// context parameter reads as unused. This file holds only that one function, with the
// parameter type spelled out. Drop the suppression when detekt 2.0 is stable.
@file:Suppress("UndocumentedPublicFunction")

package eu.kanade.tachiyomi.network

import kotlinx.serialization.serializer
import mihon.core.common.InlinedOnly
import okhttp3.Response

/** [parseAs] with the contextual [kotlinx.serialization.json.Json]. */
@InlinedOnly
context(json: kotlinx.serialization.json.Json)
public inline fun <reified T> Response.parseAs(): T = parseAs(json, serializer<T>())
