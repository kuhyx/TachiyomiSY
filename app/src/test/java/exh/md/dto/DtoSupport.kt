package exh.md.dto

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal val dtoJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Decodes [json] with [serializer], round-trips it and exercises the data-class members on the result. */
internal fun <T : Any> roundTrip(serializer: KSerializer<T>, json: String, expected: T): T {
    val decoded = dtoJson.decodeFromString(serializer, json)
    decoded shouldBe expected
    decoded.hashCode() shouldBe expected.hashCode()
    decoded.toString() shouldBe expected.toString()
    decoded shouldNotBe Any()
    dtoJson.decodeFromString(serializer, dtoJson.encodeToString(serializer, decoded)) shouldBe decoded
    return decoded
}
