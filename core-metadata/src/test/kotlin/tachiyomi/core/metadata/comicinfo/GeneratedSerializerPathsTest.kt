package tachiyomi.core.metadata.comicinfo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Test
import tachiyomi.core.metadata.tachiyomi.MangaDetails

/**
 * The compiler-generated serializers have paths Json never takes: the sequential fast path,
 * an element index outside the descriptor and encoding of default values. Two tiny decoders
 * and an `encodeDefaults` Json reach them so the classes can be held to 100% branches.
 */
@OptIn(ExperimentalSerializationApi::class)
internal class GeneratedSerializerPathsTest {
    /** Decodes every element in order; strings become "x", nullable elements become null. */
    private class SequentialDecoder : AbstractDecoder() {
        override val serializersModule: SerializersModule = EmptySerializersModule()
        override fun decodeSequentially(): Boolean = true
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = error("sequential")
        override fun decodeString(): String = "x"
        override fun decodeNotNullMark(): Boolean = false
        override fun decodeInt(): Int = 1
    }

    /** Reports an element index the descriptor does not have. */
    private class BogusIndexDecoder : AbstractDecoder() {
        override val serializersModule: SerializersModule = EmptySerializersModule()
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = BOGUS_INDEX
    }

    private val elements: List<KSerializer<*>> = listOf(
        ComicInfo.Title.serializer(), ComicInfo.Series.serializer(), ComicInfo.Number.serializer(),
        ComicInfo.Summary.serializer(), ComicInfo.Writer.serializer(), ComicInfo.Penciller.serializer(),
        ComicInfo.Inker.serializer(), ComicInfo.Colorist.serializer(), ComicInfo.Letterer.serializer(),
        ComicInfo.CoverArtist.serializer(), ComicInfo.Translator.serializer(), ComicInfo.Genre.serializer(),
        ComicInfo.Tags.serializer(), ComicInfo.Web.serializer(), ComicInfo.PublishingStatusTachiyomi.serializer(),
        ComicInfo.CategoriesTachiyomi.serializer(), ComicInfo.SourceMihon.serializer(),
        ComicInfo.PaddingTachiyomiSY.serializer(), ComicInfo.serializer(), MangaDetails.serializer(),
    )

    @Test
    fun sequentialDecodeFillsElements() {
        SequentialDecoder().decodeSerializableValue(ComicInfo.Title.serializer()) shouldBe ComicInfo.Title("x")
        val info = SequentialDecoder().decodeSerializableValue(ComicInfo.serializer())
        info.title shouldBe null
        SequentialDecoder().decodeSerializableValue(MangaDetails.serializer()) shouldBe MangaDetails()
        elements.forEach { serializer -> SequentialDecoder().decodeSerializableValue(serializer) }
    }

    @Test
    fun unknownElementIndexIsRejected() {
        elements.forEach { serializer ->
            shouldThrow<SerializationException> { BogusIndexDecoder().decodeSerializableValue(serializer) }
        }
    }

    @Test
    fun defaultsAreWrittenWhenAsked() {
        val json = Json { encodeDefaults = true }
        json.encodeToString(ComicInfo.Title.serializer(), ComicInfo.Title()) shouldBe """{"value":""}"""
        json.encodeToString(MangaDetails.serializer(), MangaDetails()).contains(""""title":null""") shouldBe true
        val info = SequentialDecoder().decodeSerializableValue(ComicInfo.serializer())
        json.encodeToString(ComicInfo.serializer(), info).contains("xmlSchema") shouldBe true
        elements.forEach { serializer -> encodeDefaults(json, serializer) }
    }

    private fun <T> encodeDefaults(json: Json, serializer: KSerializer<T>) {
        json.encodeToString(serializer, SequentialDecoder().decodeSerializableValue(serializer))
    }

    private companion object {
        const val BOGUS_INDEX = 99
    }
}
