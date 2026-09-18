package exh.metadata.metadata

import android.net.Uri
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.metadata.base.RaisedTag
import io.mockk.every
import io.mockk.mockk

/** The label [stubbedContext] resolves [resource] to. */
internal fun labelFor(resource: StringResource): String = LABEL_PREFIX + resource.resourceId

/** A manga with every field set, so every fallback in `createMangaInfo` is observable. */
internal fun sampleManga(): SManga = SManga(
    url = "/fallback/url",
    title = "Fallback title",
    artist = "Fallback artist",
    author = "Fallback author",
    description = "Fallback description",
    genre = "fallback: genre",
    status = SManga.LICENSED,
    thumbnailUrl = "https://fallback/thumb.jpg",
    initialized = true,
)

/** A scraped tag in namespace [ns]. */
internal fun tag(ns: String?, name: String, type: Int = 0): RaisedTag =
    RaisedTag(namespace = ns, name = name, type = type)

/**
 * Routes every [Uri.parse] to a [Uri.Builder] that renders as [rendered]; the caller must have
 * called `mockkStatic(Uri::class)` first. Returns the builder so query parameters can be verified.
 */
internal fun stubUriBuilder(rendered: String): Uri.Builder {
    val builder = mockk<Uri.Builder>()
    every { builder.appendQueryParameter(any(), any()) } returns builder
    every { builder.toString() } returns rendered
    val uri = mockk<Uri>()
    every { uri.buildUpon() } returns builder
    every { Uri.parse(any()) } returns uri
    return builder
}
