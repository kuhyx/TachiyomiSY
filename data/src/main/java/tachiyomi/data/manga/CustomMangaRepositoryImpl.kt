package tachiyomi.data.manga

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository
import java.io.File

/**
 * [CustomMangaRepository] backed by `edits.json` in the app's external files
 * directory (SY): the whole map is read once at construction and rewritten on
 * every change.
 */
public class CustomMangaRepositoryImpl(context: Context) : CustomMangaRepository {
    private val editJson = File(context.getExternalFilesDir(null), "edits.json")

    private val customMangaMap = fetchCustomData()

    override fun get(mangaId: Long): CustomMangaInfo? = customMangaMap[mangaId]

    private fun fetchCustomData(): MutableMap<Long, CustomMangaInfo> {
        val json = if (editJson.isFile) readEdits() else null
        return json?.mangas.orEmpty()
            .mapNotNull { mangaJson -> mangaJson.id?.let { it to mangaJson.toManga() } }
            .toMap()
            .toMutableMap()
    }

    private fun readEdits(): MangaList? {
        return try {
            Json.decodeFromString<MangaList>(editJson.readText())
        } catch (expected: Exception) {
            // A malformed edits file is treated as empty.
            null
        }
    }

    override fun set(mangaInfo: CustomMangaInfo) {
        if (mangaInfo.isEmpty()) {
            customMangaMap.remove(mangaInfo.id)
        } else {
            customMangaMap[mangaInfo.id] = mangaInfo
        }
        saveCustomInfo()
    }

    private fun CustomMangaInfo.isEmpty(): Boolean {
        val fields = listOf(title, author, artist, thumbnailUrl, description, genre, status)
        return fields.all { it == null }
    }

    private fun saveCustomInfo() {
        val jsonElements = customMangaMap.values.map { it.toJson() }
        if (jsonElements.isNotEmpty()) {
            editJson.delete()
            editJson.writeText(Json.encodeToString(MangaList(jsonElements)))
        }
    }

    /**
     * The file's root object.
     *
     * @property mangas The edited manga, or null in an empty file.
     */
    @Serializable
    public data class MangaList(
        val mangas: List<MangaJson>? = null,
    )

    /**
     * One edited manga as stored in the file; every field optional so old files still parse.
     *
     * @property id Id of the manga row the edit applies to; an entry without one is skipped.
     * @property title Edited title, or null to keep the source's.
     * @property author Edited author, or null to keep the source's.
     * @property artist Edited artist, or null to keep the source's.
     * @property thumbnailUrl Edited cover url, or null to keep the source's.
     * @property description Edited description, or null to keep the source's.
     * @property genre Edited genre list, or null to keep the source's.
     * @property status Edited publishing status, or null to keep the source's.
     */
    @Serializable
    public data class MangaJson(
        val id: Long? = null,
        val title: String? = null,
        val author: String? = null,
        val artist: String? = null,
        val thumbnailUrl: String? = null,
        val description: String? = null,
        val genre: List<String>? = null,
        val status: Long? = null,
    )

    /** The domain form of this entry; a blank title and a zero status count as unset. */
    public fun MangaJson.toManga(): CustomMangaInfo = CustomMangaInfo(
        id = id!!,
        title = title?.takeUnless { it.isBlank() },
        author = author,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        description = description,
        genre = genre,
        status = status?.takeUnless { it == 0L },
    )

    /** The stored form of this edit. */
    public fun CustomMangaInfo.toJson(): MangaJson = MangaJson(
        id = id,
        title = title,
        author = author,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        description = description,
        genre = genre,
        status = status,
    )
}
