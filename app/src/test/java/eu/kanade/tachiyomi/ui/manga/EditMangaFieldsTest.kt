package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.mutableStateOf
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.CustomMangaInfo

/** How the edit form is pre-filled for sourced and local entries, and what it submits. */
@RunWith(RobolectricTestRunner::class)
internal class EditMangaFieldsTest {
    private val rig = EditMangaRig()
    private val binding get() = rig.binding

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun statusFor(status: Long?): Int {
        rig.custom.infos.clear()
        val manga = rig.edited(status?.let { CustomMangaInfo(id = 1L, title = null, status = it) })
        binding.setUpStatusSpinner(manga, rig.context)
        return binding.status.selectedItemPosition
    }

    @Test
    fun statusFollowsEdits() {
        statusFor(null) shouldBe 0
        statusFor(SManga.COMPLETED.toLong()) shouldBe 2
        statusFor(61L) shouldBe STATUS_OPTIONS.indexOf(SManga.PUBLISHING_FINISHED)
        statusFor(62L) shouldBe STATUS_OPTIONS.indexOf(SManga.CANCELLED)
        statusFor(63L) shouldBe STATUS_OPTIONS.indexOf(SManga.ON_HIATUS)
        statusFor(99L) shouldBe 0
        binding.status.adapter.getItem(0) shouldBe "Default"
    }

    @Test
    fun sourcedShowsOnlyEdits() {
        val info = CustomMangaInfo(
            id = 1L,
            title = "New title",
            author = "New author",
            artist = "New artist",
            thumbnailUrl = "https://new",
            description = "New description",
            genre = listOf("two"),
        )
        binding.fillSourcedFields(rig.edited(info), rig.context, rig.scope)
        binding.title.text.toString() shouldBe "New title"
        binding.mangaAuthor.text.toString() shouldBe "New author"
        binding.mangaArtist.text.toString() shouldBe "New artist"
        binding.thumbnailUrl.text.toString() shouldBe "https://new"
        binding.mangaDescription.text.toString() shouldBe "New description"
        binding.mangaGenresTags.getTextStrings() shouldBe listOf("two")
    }

    @Test
    fun sourcedWithoutEditsKeepsHints() {
        val bare = rig.sourced()
            .copy(ogAuthor = null, ogArtist = null, ogDescription = " ", ogThumbnailUrl = "https://s")
        binding.fillSourcedFields(rig.edited(null, base = bare), rig.context, rig.scope)
        binding.title.text.toString() shouldBe ""
        val noCover = bare.copy(ogThumbnailUrl = null, ogDescription = null, ogGenre = null)
        binding.fillSourcedFields(rig.edited(null, base = noCover), rig.context, rig.scope)
    }

    @Test
    fun localShowsEveryValue() {
        val local = rig.edited(null, source = 0L)
        binding.fillLocalFields(local, rig.context, rig.scope)
        binding.title.text.toString() shouldBe "Og title"
        binding.mangaAuthor.text.toString() shouldBe "Og author"
        val bare = rig.sourced().copy(
            ogTitle = "/m/1",
            ogAuthor = null,
            ogArtist = null,
            ogThumbnailUrl = null,
            ogDescription = null,
            ogGenre = null,
        )
        val fresh = rig.inflate()
        fresh.fillLocalFields(rig.edited(null, source = 0L, base = bare), rig.context, rig.scope)
        fresh.title.text.toString() shouldBe ""
        fresh.mangaAuthor.text.toString() shouldBe ""
    }

    @Test
    fun submitReadsEveryField() {
        binding.fillLocalFields(rig.edited(null, source = 0L), rig.context, rig.scope)
        binding.setUpStatusSpinner(rig.sourced(), rig.context)
        binding.status.setSelection(2)
        var submitted: List<Any?> = emptyList()
        binding.submit { title, author, artist, thumbnail, description, tags, status ->
            submitted = listOf(title, author, artist, thumbnail, description, tags, status)
        }
        submitted shouldBe listOf(
            "Og title",
            "Og author",
            "Og artist",
            "https://example.org/a/very/long/path/to/the/cover/image.jpeg",
            "An original description\nthat is long",
            listOf("one"),
            SManga.COMPLETED.toLong(),
        )
    }

    @Test
    fun resetsClearTheForm() {
        val sourced = rig.edited(CustomMangaInfo(id = 1L, title = "T", genre = listOf("x")))
        binding.fillSourcedFields(sourced, rig.context, rig.scope)
        resetInfo(sourced, binding, rig.scope)
        binding.title.text.toString() shouldBe ""
        binding.mangaGenresTags.getTextStrings() shouldBe listOf("one", " ")
        resetTags(rig.edited(null, source = 0L), binding, rig.scope)
        binding.mangaGenresTags.getTextStrings() shouldBe emptyList()
        resetTags(rig.sourced().copy(ogGenre = emptyList()), binding, rig.scope)
        binding.mangaGenresTags.getTextStrings() shouldBe emptyList()
    }

    @Test
    fun localFormAndTagReset() {
        val local = rig.edited(null, source = 0L)
        onViewCreated(
            manga = local,
            context = rig.context,
            binding = binding,
            scope = rig.scope,
            getTracks = mockk(),
            trackerManager = mockk(),
            tracks = mutableStateOf(emptyList()),
            showTrackerSelectionDialogue = mutableStateOf(false),
        )
        binding.title.text.toString() shouldBe "Og title"
        resetTags(local, binding, rig.scope)
        binding.mangaGenresTags.getTextStrings() shouldBe emptyList()
    }
}
