package eu.kanade.domain.source.interactor

import tachiyomi.domain.source.model.Source

/** An installed online source. */
internal fun source(id: Long, name: String = "Source $id", lang: String = "en", isStub: Boolean = false): Source =
    Source(id = id, lang = lang, name = name, supportsLatest = true, isStub = isStub)
