package tachiyomi.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.SourceWithCount
import tachiyomi.domain.source.repository.SourceRepository

/** Lists the sources that still have manga outside the library, for the "clear database" screen. */
public class GetSourcesWithNonLibraryManga(
    private val repository: SourceRepository,
) {

    /** Each such source with its non-library manga count, as a flow that re-emits on every change. */
    public fun subscribe(): Flow<List<SourceWithCount>> = repository.getSourcesWithNonLibraryManga()
}
