package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.track.domainTrack
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.core.common.preference.TriState
import eu.kanade.tachiyomi.data.track.BaseTracker
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
internal class LibrarySearchTextTest {
    private val harness = LibraryHarness()
    private val rig = LibrarySearchRig(harness)
    private val full by lazy {
        manga(1, "Title").copy(
            source = 5,
            ogAuthor = "Author",
            ogArtist = "Artist",
            ogDescription = "Blurb",
            ogGenre = listOf("Genre"),
        )
    }
    private val bare by lazy { manga(2, "Bare").copy(source = 5) }
    private val tracks by lazy { listOf(domainTrack(trackerId = 7L)) }
    private val loggedIn = mapOf(7L to TriState.DISABLED)

    @Before
    fun setUp() {
        startKoin { modules(harness.koinModules()) }
        val tracker = mockk<BaseTracker> {
            every { name } returns "Tracker"
            every { getStatus(any()) } returns MR.strings.reading
        }
        every { harness.trackerManager.get(7L) } returns tracker
    }

    @After
    fun tearDown() = stopKoin()

    private fun hit(query: String, extras: SearchExtras = SearchExtras()) = rig.matches(rig.text(query), full, extras)

    @Test
    fun eachFieldCanMatch() {
        listOf("title", "author", "artist", "blurb", "genre", "5").forEach { hit(it).shouldBeTrue() }
        hit("site", SearchExtras(source = rig.source("Site"))).shouldBeTrue()
        hit("reading", SearchExtras(tracks = tracks, loggedIn = loggedIn)).shouldBeTrue()
        hit("tag", SearchExtras(tags = listOf(tag("Tag")))).shouldBeTrue()
        hit("alt", SearchExtras(titles = listOf(title("Alt")))).shouldBeTrue()
    }

    @Test
    fun nothingMatches() {
        val extras = SearchExtras(
            tracks = tracks,
            source = rig.source("Site"),
            tags = listOf(tag("Tag")),
            titles = listOf(title("Alt")),
            loggedIn = loggedIn,
        )
        hit("zzz", extras).shouldBeFalse()
        hit("zzz", SearchExtras(tracks = tracks)).shouldBeFalse()
        rig.matches(rig.text("zzz"), bare).shouldBeFalse()
    }

    @Test
    fun exclusionNeedsEveryFieldToMiss() {
        listOf("title", "author", "artist", "blurb", "genre").forEach {
            rig.matches(rig.text(it, excluded = true), full).shouldBeFalse()
        }
        rig.matches(rig.text("zzz", excluded = true), full).shouldBeTrue()
        rig.matches(rig.text("zzz", excluded = true), bare).shouldBeTrue()
        rig.matches(rig.text("", excluded = true), full).shouldBeTrue()
    }

    @Test
    fun exclusionChecksSourceAndExtras() {
        fun excluded(query: String, extras: SearchExtras) = rig.matches(rig.text(query, excluded = true), full, extras)
        excluded("site", SearchExtras(source = rig.source("Site"))).shouldBeFalse()
        excluded("zzz", SearchExtras(source = rig.source("Site"))).shouldBeTrue()
        excluded("5", SearchExtras()).shouldBeFalse()
        excluded("reading", SearchExtras(tracks = tracks, loggedIn = loggedIn)).shouldBeFalse()
        excluded("zzz", SearchExtras(tracks = tracks, loggedIn = loggedIn)).shouldBeTrue()
        excluded("tag", SearchExtras(tags = listOf(tag("Tag")))).shouldBeFalse()
        excluded("alt", SearchExtras(titles = listOf(title("Alt")))).shouldBeFalse()
        excluded("zzz", SearchExtras(tags = listOf(tag("Tag")), titles = listOf(title("Alt")))).shouldBeTrue()
    }

    @Test
    fun localSourceEntriesAndExclusion() {
        val local = full.copy(source = 0)
        rig.matches(rig.text("0"), local).shouldBeFalse()
        // Pins current behaviour (see the issue linked in the PR): a local entry never survives an exclusion.
        rig.matches(rig.text("zzz", excluded = true), local).shouldBeFalse()
    }
}
