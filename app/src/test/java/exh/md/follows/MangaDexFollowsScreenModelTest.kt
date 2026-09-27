package exh.md.follows

import eu.kanade.tachiyomi.source.model.FilterList
import exh.metadata.metadata.RaisedSearchMetadata
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class MangaDexFollowsScreenModelTest {
    private val rig = FollowsRig()

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun followsAreNotFilterable() {
        val model = MangaDexFollowsScreenModel(1L)
        model.state.value.filterable shouldBe false
    }

    @Test
    fun pagesComeFromTheFollows() {
        val model = MangaDexFollowsScreenModel(1L)
        val paging = model.createSourcePagingSource("ignored", FilterList())
        paging.shouldBeInstanceOf<MangaDexFollowsPagingSource>().mangadex shouldBe rig.mangaDex
    }

    @Test
    fun metadataIsPassedThrough() {
        val model = MangaDexFollowsScreenModel(1L)
        val manga = Manga.create().copy(id = 3L)
        val metadata = mockk<RaisedSearchMetadata>()
        val pairs = runBlocking { with(model) { flowOf(manga).combineMetadata(metadata).toList() } }
        pairs shouldBe listOf(manga to metadata)
    }
}
