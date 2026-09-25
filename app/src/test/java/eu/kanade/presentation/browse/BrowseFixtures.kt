package eu.kanade.presentation.browse

import eu.kanade.tachiyomi.source.Source
import io.mockk.every
import io.mockk.mockk

/** A source stub named [name] in [lang]. */
internal fun browseSource(id: Long, name: String = "Source $id", lang: String = "en"): Source {
    val source = mockk<Source>()
    every { source.id } returns id
    every { source.name } returns name
    every { source.lang } returns lang
    return source
}
