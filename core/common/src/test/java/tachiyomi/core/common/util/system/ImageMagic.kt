package tachiyomi.core.common.util.system

import tachiyomi.decoder.Format
import tachiyomi.decoder.ImageType

/** Magic-byte sniffing the decoder shadow uses in place of the native `findType`. */
internal object ImageMagic {
    private const val WEBP_VP8X_OFFSET = 12
    private const val WEBP_FLAGS_OFFSET = 20
    private const val WEBP_ANIMATION_FLAG = 0x02
    private const val FTYP_OFFSET = 4
    private const val BRAND_OFFSET = 8

    fun sniff(bytes: ByteArray): ImageType? = when {
        bytes.startsWith(0, 0xFF, 0xD8, 0xFF) -> type(Format.Jpeg, false)
        bytes.startsWith(0, 0x89, 'P'.code, 'N'.code, 'G'.code) -> type(Format.Png, false)
        bytes.hasAscii(0, "GIF8") -> type(Format.Gif, false)
        bytes.hasAscii(0, "RIFF") && bytes.hasAscii(BRAND_OFFSET, "WEBP") -> type(Format.Webp, isAnimatedWebp(bytes))
        bytes.hasAscii(FTYP_OFFSET, "ftyp") -> isoBase(bytes)
        bytes.startsWith(0, 0xFF, 0x0A) -> type(Format.Jxl, false)
        else -> null
    }

    // Both ImageType constructors are Kotlin-internal to the decoder module (the JNI side calls them), so go
    // through the public JVM constructor.
    private fun type(format: Format, isAnimated: Boolean): ImageType = ImageType::class.java
        .getConstructor(Format::class.java, java.lang.Boolean.TYPE)
        .newInstance(format, isAnimated)

    private fun isAnimatedWebp(bytes: ByteArray): Boolean =
        bytes.hasAscii(WEBP_VP8X_OFFSET, "VP8X") && bytes[WEBP_FLAGS_OFFSET].toInt() and WEBP_ANIMATION_FLAG != 0

    private fun isoBase(bytes: ByteArray): ImageType = when {
        bytes.hasAscii(BRAND_OFFSET, "avif") -> type(Format.Avif, false)
        bytes.hasAscii(BRAND_OFFSET, "avis") -> type(Format.Avif, true)
        bytes.hasAscii(BRAND_OFFSET, "msf1") -> type(Format.Heif, true)
        else -> type(Format.Heif, false)
    }

    private fun ByteArray.startsWith(offset: Int, vararg values: Int): Boolean =
        size >= offset + values.size && values.indices.all { this[offset + it].toInt() and 0xFF == values[it] }

    private fun ByteArray.hasAscii(offset: Int, text: String): Boolean =
        startsWith(offset, *text.map { it.code }.toIntArray())
}

/** Builds header bytes for the formats [ImageMagic] recognises, padded to [HEADER_SIZE]. */
internal object ImageHeaders {
    const val HEADER_SIZE: Int = 32

    val jpeg: ByteArray = pad(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
    val png: ByteArray = pad(byteArrayOf(0x89.toByte()) + "PNG".toByteArray())
    val gif: ByteArray = pad("GIF89a".toByteArray())
    val jxl: ByteArray = pad(byteArrayOf(0xFF.toByte(), 0x0A))
    val webp: ByteArray = riff("VP8 ", flags = 0)
    val animatedWebp: ByteArray = riff("VP8X", flags = 0x02)
    val stillWebpVp8x: ByteArray = riff("VP8X", flags = 0)
    val avif: ByteArray = ftyp("avif")
    val animatedAvif: ByteArray = ftyp("avis")
    val heif: ByteArray = ftyp("heic")
    val animatedHeif: ByteArray = ftyp("msf1")

    private fun riff(chunk: String, flags: Int): ByteArray =
        pad(
            "RIFF".toByteArray() + ByteArray(4) + "WEBP".toByteArray() +
                chunk.toByteArray() + ByteArray(4) + flags.toByte(),
        )

    private fun ftyp(brand: String): ByteArray = pad(ByteArray(4) + "ftyp".toByteArray() + brand.toByteArray())

    private fun pad(head: ByteArray): ByteArray = head.copyOf(HEADER_SIZE)
}
