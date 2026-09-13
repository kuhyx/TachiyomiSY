package exh.metadata.metadata

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.metadata.metadata.base.RaisedTitle
import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import exh.util.plusAssign
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Serializable
public sealed class RaisedSearchMetadata {
    @Transient
    public var mangaId: Long = -1

    @Transient
    public var uploader: String? = null

    @Transient
    protected open var indexedExtra: String? = null

    @Transient
    public val tags: MutableList<RaisedTag> = mutableListOf<RaisedTag>()

    @Transient
    public val titles: MutableList<RaisedTitle> = mutableListOf<RaisedTitle>()

    public fun getTitleOfType(type: Int): String? = titles.find { it.type == type }?.title

    public fun replaceTitleOfType(type: Int, newTitle: String?) {
        titles.removeAll { it.type == type }
        if (newTitle != null) titles += RaisedTitle(newTitle, type)
    }

    public fun <T : Any> getItem(
        item: T?,
        toString: (T) -> String = Any::toString,
        block: (T) -> String,
    ): Pair<String, String>? {
        item ?: return null
        return block(item) to toString(item)
    }

    /*open fun copyTo(manga: SManga) {
        val infoManga = createMangaInfo(manga.copy())
        manga.copyFrom(infoManga)
    }*/

    public abstract fun createMangaInfo(manga: SManga): SManga

    public fun tagsToGenreString(): String = tags.toGenreString()

    public fun tagsToGenreList(): List<String> = tags.toGenreList()

    public fun tagsToDescription(): StringBuilder =
        StringBuilder("Tags:\n").apply {
            // BiConsumer only available in Java 8, don't bother calling forEach directly on 'tags'
            val groupedTags = tags.filter { it.type != TAG_TYPE_VIRTUAL }.groupBy {
                it.namespace
            }.entries

            groupedTags.forEach { (namespace, tags) ->
                if (tags.isNotEmpty()) {
                    val joinedTags = tags.joinToString(separator = " ", transform = { "<${it.name}>" })
                    if (namespace != null) {
                        this += "▪ "
                        this += namespace
                        this += ": "
                    }
                    this += joinedTags
                    this += "\n"
                }
            }
        }

    public fun List<RaisedTag>.ofNamespace(ns: String): List<RaisedTag> {
        return filter { it.namespace == ns }
    }

    public fun flatten(): FlatMetadata {
        require(mangaId != -1L)

        val extra = raiseFlattenJson.encodeToString(this)
        return FlatMetadata(
            SearchMetadata(
                mangaId,
                uploader,
                extra,
                indexedExtra,
                0,
            ),
            tags.map {
                SearchTag(
                    null,
                    mangaId,
                    it.namespace,
                    it.name,
                    it.type,
                )
            },
            titles.map {
                SearchTitle(
                    null,
                    mangaId,
                    it.title,
                    it.type,
                )
            },
        )
    }

    public fun fillBaseFields(metadata: FlatMetadata) {
        mangaId = metadata.metadata.mangaId
        uploader = metadata.metadata.uploader
        indexedExtra = metadata.metadata.indexedExtra

        this.tags.clear()
        this.tags += metadata.tags.map {
            RaisedTag(it.namespace, it.name, it.type)
        }

        this.titles.clear()
        this.titles += metadata.titles.map {
            RaisedTitle(it.title, it.type)
        }
    }

    public abstract fun getExtraInfoPairs(context: Context): List<Pair<String, String>>

    public companion object {
        // Virtual tags allow searching of otherwise unindexed fields
        public const val TAG_TYPE_VIRTUAL: Int = -2

        public fun MutableList<RaisedTag>.toGenreString(): String =
            this.filter { it.type != TAG_TYPE_VIRTUAL }
                .joinToString { (if (it.namespace != null) "${it.namespace}: " else "") + it.name }

        public fun MutableList<RaisedTag>.toGenreList(): List<String> =
            this.filter { it.type != TAG_TYPE_VIRTUAL }
                .map { (if (it.namespace != null) "${it.namespace}: " else "") + it.name }

        public val raiseFlattenJson: Json = Json {
            ignoreUnknownKeys = true
        }

        public fun titleDelegate(type: Int): ReadWriteProperty<RaisedSearchMetadata, String?> =
            object : ReadWriteProperty<RaisedSearchMetadata, String?> {
                /**
                 * Returns the value of the property for the given object.
                 * @param thisRef the object for which the value is requested.
                 * @param property the metadata for the property.
                 * @return the property value.
                 */
                override fun getValue(thisRef: RaisedSearchMetadata, property: KProperty<*>) =
                    thisRef.getTitleOfType(type)

                /**
                 * Sets the value of the property for the given object.
                 * @param thisRef the object for which the value is requested.
                 * @param property the metadata for the property.
                 * @param value the value to set.
                 */
                override fun setValue(thisRef: RaisedSearchMetadata, property: KProperty<*>, value: String?) =
                    thisRef.replaceTitleOfType(type, value)
            }
    }
}
