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

/**
 * Source-specific metadata of a manga, kept next to the tags and titles it was scraped with;
 * subclasses add the site's own fields and know how to project them onto an [SManga].
 */
@Serializable
public sealed class RaisedSearchMetadata {
    /** Database id of the manga, -1 until saved. */
    @Transient
    public var mangaId: Long = -1

    /** Uploader name, when the site has one. */
    @Transient
    public var uploader: String? = null

    @Transient
    protected open var indexedExtra: String? = null

    /** Scraped tags; virtual tags are search-only. */
    @Transient
    public val tags: MutableList<RaisedTag> = mutableListOf<RaisedTag>()

    /** Scraped titles by type. */
    @Transient
    public val titles: MutableList<RaisedTitle> = mutableListOf<RaisedTitle>()

    /** The stored title of [type], if any. */
    public fun getTitleOfType(type: Int): String? = titles.find { it.type == type }?.title

    /** Replaces the title of [type] with [newTitle], or removes it when null. */
    public fun replaceTitleOfType(type: Int, newTitle: String?) {
        titles.removeAll { it.type == type }
        if (newTitle != null) titles += RaisedTitle(newTitle, type)
    }

    /** A label/value pair for the details screen, or null when [item] is null. */
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

    /** A copy of [manga] with this metadata applied. */
    public abstract fun createMangaInfo(manga: SManga): SManga

    /** Non-virtual tags as one comma-separated string. */
    public fun tagsToGenreString(): String = tags.toGenreString()

    /** Non-virtual tags as a list of `namespace: name` strings. */
    public fun tagsToGenreList(): List<String> = tags.toGenreList()

    /** Non-virtual tags grouped by namespace, formatted for a description. */
    public fun tagsToDescription(): StringBuilder {
        val description = StringBuilder("Tags:\n")
        tags.filter { it.type != TAG_TYPE_VIRTUAL }
            .groupBy { it.namespace }
            .forEach { (namespace, tags) -> description.appendNamespace(namespace, tags) }
        return description
    }

    private fun StringBuilder.appendNamespace(namespace: String?, tags: List<RaisedTag>) {
        if (tags.isEmpty()) return
        if (namespace != null) {
            this += "▪ "
            this += namespace
            this += ": "
        }
        this += tags.joinToString(separator = " ", transform = { "<${it.name}>" })
        this += "\n"
    }

    /** The tags whose namespace is [ns]. */
    public fun List<RaisedTag>.ofNamespace(ns: String): List<RaisedTag> = filter { it.namespace == ns }

    /** The database representation; requires a saved [mangaId]. */
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

    /** Restores the base fields, tags and titles from a database row. */
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

    /** Label/value pairs shown on the details screen. */
    public abstract fun getExtraInfoPairs(context: Context): List<Pair<String, String>>

    /** Shared tag helpers. */
    public companion object {
        /** Type of virtual tags, which index otherwise unindexed fields for search only. */
        public const val TAG_TYPE_VIRTUAL: Int = -2

        /** The JSON format the `extra` column is written and read with. */
        public val raiseFlattenJson: Json = Json {
            ignoreUnknownKeys = true
        }

        /** Non-virtual tags as one comma-separated string. */
        public fun MutableList<RaisedTag>.toGenreString(): String =
            this.filter { it.type != TAG_TYPE_VIRTUAL }
                .joinToString { (if (it.namespace != null) "${it.namespace}: " else "") + it.name }

        /** Non-virtual tags as `namespace: name` strings. */
        public fun MutableList<RaisedTag>.toGenreList(): List<String> =
            this.filter { it.type != TAG_TYPE_VIRTUAL }
                .map { (if (it.namespace != null) "${it.namespace}: " else "") + it.name }

        /** A property delegate backed by the title of [type]. */
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
