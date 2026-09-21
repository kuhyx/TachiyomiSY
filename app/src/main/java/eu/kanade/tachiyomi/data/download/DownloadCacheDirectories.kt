package eu.kanade.tachiyomi.data.download

import android.app.Application
import androidx.core.net.toUri
import com.hippo.unifile.UniFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Class to store the files under the root downloads directory.
 */
@Serializable
internal class RootDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var sourceDirs: Map<Long, SourceDirectory> = mapOf(),
) {
    fun chapterCount(): Int = sourceDirs.values.sumOf { it.chapterCount() }
}

/**
 * Class to store the files under a source directory.
 */
@Serializable
internal class SourceDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var mangaDirs: Map<String, MangaDirectory> = mapOf(),
) {
    fun chapterCount(): Int = mangaDirs.values.sumOf { it.chapterDirs.size }
}

/**
 * Class to store the files under a manga directory.
 */
@Serializable
internal class MangaDirectory(
    @Serializable(with = UniFileAsStringSerializer::class)
    val dir: UniFile?,
    var chapterDirs: MutableSet<String> = mutableSetOf(),
) {
    operator fun contains(chapterDirName: String): Boolean = chapterDirName in chapterDirs
}

private object UniFileAsStringSerializer : KSerializer<UniFile?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UniFile", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UniFile?) {
        return if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeString(value.uri.toString())
        }
    }

    override fun deserialize(decoder: Decoder): UniFile? {
        return if (decoder.decodeNotNullMark()) {
            UniFile.fromUri(Injekt.get<Application>(), decoder.decodeString().toUri())
        } else {
            decoder.decodeNull()
        }
    }
}
