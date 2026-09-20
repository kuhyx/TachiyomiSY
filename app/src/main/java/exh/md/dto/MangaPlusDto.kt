package exh.md.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class MangaPlusResponse(
    val success: SuccessResult? = null,
)

@Serializable
internal data class SuccessResult(
    val mangaViewer: MangaViewer? = null,
)

@Serializable
internal data class MangaViewer(val pages: List<MangaPlusPage> = emptyList())

@Serializable
internal data class MangaPlusPage(val mangaPage: MangaPage? = null)

@Serializable
internal data class MangaPage(
    val imageUrl: String,
    val width: Int,
    val height: Int,
    val encryptionKey: String? = null,
)
