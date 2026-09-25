package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.serveMetadataSource
import exh.eh.EHentaiUpdateHelper
import exh.eh.GalleryEntry
import exh.eh.MemAutoFlushingLookupTable
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

/** The parent-gallery cache the update helper serves, as an in-memory map. */
internal class FakeParentTable {
    val entries: MutableMap<Int, GalleryEntry> = mutableMapOf()
    val table: MemAutoFlushingLookupTable<GalleryEntry> = mockk()

    init {
        coEvery { table.get(any()) } answers { entries[firstArg()] }
        coEvery { table.put(any(), any()) } answers { entries[firstArg()] = secondArg() }
    }
}

/** Serves what an [EHentai] pulls from Injekt beyond the harness defaults, then builds one. */
internal fun SourceTestHarness.ehentai(exh: Boolean = false, parents: FakeParentTable = FakeParentTable()): EHentai {
    val helper = mockk<EHentaiUpdateHelper> { every { parentLookupTable } returns parents.table }
    serve(helper)
    serveMetadataSource()
    return EHentai(if (exh) EXH_SOURCE_ID else EH_SOURCE_ID, exh, application)
}
