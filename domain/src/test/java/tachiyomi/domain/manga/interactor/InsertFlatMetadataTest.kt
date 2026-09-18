package tachiyomi.domain.manga.interactor

import exh.metadata.metadata.RankedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.sql.models.SearchMetadata
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.repository.MangaMetadataRepository

internal class InsertFlatMetadataTest {

    private val flat = FlatMetadata(
        metadata = SearchMetadata(mangaId = 4L, uploader = null, extra = "{}", indexedExtra = null, extraVersion = 0),
        tags = emptyList(),
        titles = emptyList(),
    )
    private val raised = RankedSearchMetadata().apply { mangaId = 4L }
    private val repository = mockk<MangaMetadataRepository>()
    private val insertFlatMetadata = InsertFlatMetadata(repository)

    @Test
    fun flatInsertDelegates() = runTest {
        coEvery { repository.insertFlatMetadata(flat) } returns Unit

        insertFlatMetadata.await(flat)

        coVerify(exactly = 1) { repository.insertFlatMetadata(flat) }
    }

    @Test
    fun flatInsertSwallowsFailure() = runTest {
        coEvery { repository.insertFlatMetadata(flat) } throws IllegalStateException("store failed")

        insertFlatMetadata.await(flat)

        coVerify(exactly = 1) { repository.insertFlatMetadata(flat) }
    }

    @Test
    fun raisedInsertDelegates() = runTest {
        coEvery { repository.insertMetadata(raised) } returns Unit

        insertFlatMetadata.await(raised)

        coVerify(exactly = 1) { repository.insertMetadata(raised) }
    }

    @Test
    fun raisedInsertSwallowsFailure() = runTest {
        coEvery { repository.insertMetadata(raised) } throws IllegalStateException("store failed")

        insertFlatMetadata.await(raised)

        coVerify(exactly = 1) { repository.insertMetadata(raised) }
    }
}
