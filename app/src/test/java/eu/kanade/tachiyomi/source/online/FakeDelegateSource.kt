package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.Request
import okhttp3.Response
import tachiyomi.core.common.preference.PreferenceStore

/**
 * The extension source an in-app delegate wraps: identity, plus listings that answer without the
 * network (a page naming the query) so a wrapper's pass-through can be asserted.
 */
internal open class FakeDelegateSource(
    override val baseUrl: String,
    override val lang: String = "en",
    override val name: String = "Fake",
    override val versionId: Int = 1,
) : HttpSource() {
    override val supportsLatest: Boolean = true

    override suspend fun getPopularManga(page: Int): MangasPage = pageOf("/popular/$page")

    override suspend fun getLatestUpdates(page: Int): MangasPage = pageOf("/latest/$page")

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        pageOf("/search/$page/$query")

    override suspend fun getImageUrl(page: Page): String = "resolved:${page.url}"

    override suspend fun getImage(page: Page, existingSize: Long): Response = cannedResponse("image:${page.url}")

    // The latest-updates helpers are what a wrapper rewrites; a deprecated override raises no warning.
    @Deprecated("extension helper")
    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/latest?page=$page&includeFutureUpdates=1", headers)

    @Deprecated("extension helper")
    override fun latestUpdatesParse(response: Response): MangasPage = pageOf("/latest/${response.body.string()}")

    private fun pageOf(url: String): MangasPage = MangasPage(listOf(SManga(url = url, title = url)), false)
}

/**
 * Serves the metadata-source collaborators and the preference store: the manga is saved under
 * [mangaId] (none by default) with [saved] as its stored metadata, and writes are recorded in [inserted].
 */
internal fun SourceTestHarness.serveMetadataSource(
    mangaId: Long? = null,
    saved: FlatMetadata? = null,
    inserted: MutableList<RaisedSearchMetadata> = mutableListOf(),
) {
    serve<PreferenceStore>(store)
    serve<MetadataSource.GetMangaId>(mockk { coEvery { awaitId(any(), any()) } returns mangaId })
    val insert = mockk<MetadataSource.InsertFlatMetadata>()
    coEvery { insert.await(any()) } answers { inserted += firstArg<RaisedSearchMetadata>() }
    serve(insert)
    serve<MetadataSource.GetFlatMetadataById>(mockk { coEvery { await(any()) } returns saved })
}
