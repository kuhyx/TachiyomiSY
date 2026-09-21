package eu.kanade.tachiyomi.data.track.mdlist

import eu.kanade.tachiyomi.data.track.TrackKoin
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.MangaDex
import io.mockk.every
import io.mockk.mockk

/**
 * An [MdList] whose MangaDex source is a mock found through the real [exh.md.utils.MdUtil]
 * lookup (source manager + source preferences), or no MangaDex at all.
 */
internal class MdListHarness {
    val koin: TrackKoin = TrackKoin()
    val mangaDex: MangaDex = mockk {
        every { id } returns MANGADEX_ID
        every { lang } returns "en"
    }
    lateinit var tracker: MdList

    fun start(withMangaDex: Boolean = true) {
        koin.start()
        koin.sourcePreferences.enabledLanguages.set(setOf("en"))
        val sources = if (withMangaDex) listOf(mangaDex) else emptyList()
        every { koin.sourceManager.getVisibleOnlineSources() } returns sources
        tracker = MdList(TRACK_ID)
    }

    fun stop() {
        koin.stop()
    }

    companion object {
        const val TRACK_ID = 60L
        const val MANGADEX_ID = 2_499_283_573_021_220_255L

        fun sManga(url: String, title: String, thumbnail: String? = null, description: String? = null): SManga =
            SManga.create().also {
                it.url = url
                it.title = title
                it.thumbnail_url = thumbnail
                it.description = description
            }
    }
}
