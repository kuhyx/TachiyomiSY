package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.metadata.base.raise
import rx.Completable
import rx.Single
import tachiyomi.core.common.util.lang.awaitSingle
import tachiyomi.core.common.util.lang.runAsObservable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.reflect.KClass

/**
 * LEWD!
 */
public interface MetadataSource<M : RaisedSearchMetadata, I> : Source {
    /** Looks a manga's database id up by URL and source. */
    public interface GetMangaId {
        /** The id of the manga at [url] in [sourceId], if saved. */
        public suspend fun awaitId(url: String, sourceId: Long): Long?
    }

    /** Saves metadata to the database. */
    public interface InsertFlatMetadata {
        /** Saves [metadata] in its flattened form. */
        public suspend fun await(metadata: RaisedSearchMetadata)
    }

    /** Loads saved metadata by manga id. */
    public interface GetFlatMetadataById {
        /** The flattened metadata of manga [id], if any. */
        public suspend fun await(id: Long): FlatMetadata?
    }

    /** Injected id lookup. */
    public val getMangaId: GetMangaId get() = Injekt.get()

    /** Injected metadata writer. */
    public val insertFlatMetadata: InsertFlatMetadata get() = Injekt.get()

    /** Injected metadata reader. */
    public val getFlatMetadataById: GetFlatMetadataById get() = Injekt.get()

    /**
     * The class of the metadata used by this source.
     */
    public val metaClass: KClass<M>

    /**
     * Parse the supplied input into the supplied metadata object.
     */
    public suspend fun parseIntoMetadata(metadata: M, input: I)

    /**
     * Use reflection to create a new instance of metadata.
     */
    public fun newMetaInstance(): M

    /**
     * Parses metadata from the input and then copies it into the manga.
     *
     * Will also save the metadata to the DB if possible.
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use the MangaInfo variant")
    public fun parseToMangaCompletable(manga: SManga, input: I): Completable = runAsObservable {
        parseToManga(manga, input)
    }.toCompletable()

    /** Parses [input] into fresh metadata, saves it and returns [manga] with it applied. */

    public suspend fun parseToManga(manga: SManga, input: I): SManga {
        val mangaId = manga.mangaId()
        val metadata = if (mangaId != null) {
            val flatMetadata = getFlatMetadataById.await(mangaId)
            flatMetadata?.raise(metaClass) ?: newMetaInstance()
        } else {
            newMetaInstance()
        }

        parseIntoMetadata(metadata, input)
        if (mangaId != null) {
            metadata.mangaId = mangaId
            insertFlatMetadata.await(metadata)
        }

        return metadata.createMangaInfo(manga)
    }

    /**
     * Try to first get the metadata from the DB. If the metadata is not in the DB, calls the input
     * producer and parses the metadata from the input
     *
     * If the metadata needs to be parsed from the input producer, the resulting parsed metadata will
     * also be saved to the DB.
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("use fetchOrLoadMetadata made for MangaInfo")
    public fun getOrLoadMetadata(mangaId: Long?, inputProducer: () -> Single<I>): Single<M> =
        runAsObservable {
            fetchOrLoadMetadata(mangaId) { inputProducer().toObservable().awaitSingle() }
        }.toSingle()

    /**
     * Try to first get the metadata from the DB. If the metadata is not in the DB, calls the input
     * producer and parses the metadata from the input
     *
     * If the metadata needs to be parsed from the input producer, the resulting parsed metadata will
     * also be saved to the DB.
     */
    public suspend fun fetchOrLoadMetadata(mangaId: Long?, inputProducer: suspend () -> I): M {
        val meta = mangaId?.let { getFlatMetadataById.await(it)?.raise(metaClass) }

        return meta ?: inputProducer().let { input ->
            val newMeta = newMetaInstance()
            parseIntoMetadata(newMeta, input)
            if (mangaId != null) {
                newMeta.mangaId = mangaId
                insertFlatMetadata.await(newMeta)
            }
            newMeta
        }
    }

    /** The database id of this manga in this source, if saved. */

    public suspend fun SManga.mangaId(): Long? = getMangaId.awaitId(url, id)
}
