package tachiyomi.domain.source.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.StubSource

/** Persistence of the ids, names and languages of sources seen once but no longer installed. */
public interface StubSourceRepository {
    /** Every remembered stub, as a flow. */
    public fun subscribeAll(): Flow<List<StubSource>>

    /** The remembered stub with [id], or null when the source was never recorded. */
    public suspend fun getStubSource(id: Long): StubSource?

    /** Records or refreshes the [lang] and [name] remembered for source [id]. */
    public suspend fun upsertStubSource(id: Long, lang: String, name: String)
}
