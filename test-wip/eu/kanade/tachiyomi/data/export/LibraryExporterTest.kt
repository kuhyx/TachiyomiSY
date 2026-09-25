package eu.kanade.tachiyomi.data.export

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.export.LibraryExporter.ExportOptions
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga
import java.io.ByteArrayOutputStream
import java.io.OutputStream

private const val QUOTE = "\""
private const val CR = "\r"
private const val LF = "\n"

private fun manga(title: String, author: String?, artist: String?): Manga = Manga.create().copy(
    ogTitle = title,
    ogAuthor = author,
    ogArtist = artist,
)

private val allColumns = ExportOptions(includeTitle = true, includeAuthor = true, includeArtist = true)

internal class LibraryExporterTest {

    private val uri: Uri = mockk()
    private var stream: OutputStream? = ByteArrayOutputStream()

    private val context: Context = mockk<Context>().also {
        val resolver = mockk<ContentResolver>()
        every { resolver.openOutputStream(uri) } answers { stream }
        every { it.contentResolver } returns resolver
    }

    private suspend fun export(
        favorites: List<Manga>,
        options: ExportOptions = allColumns,
    ): Pair<String, Int> {
        var completions = 0
        LibraryExporter.exportToCsv(
            context = context,
            uri = uri,
            favorites = favorites,
            options = options,
            onExportComplete = { completions++ },
        )
        return (stream as ByteArrayOutputStream).toString("UTF-8") to completions
    }

    @Test
    fun writesEveryColumn() = runTest {
        val (csv, completions) = export(listOf(manga("Title", "Author", "Artist")))
        csv shouldBe "Title,Author,Artist"
        completions shouldBe 1
    }

    /** Called from the IO dispatcher the exporter switches to, so its `withContext` returns undispatched. */
    @Test
    fun writesEveryColumnUndispatched() = runTest {
        withContext(Dispatchers.IO) {
            val (csv, completions) = export(listOf(manga("Title", "Author", "Artist")))
            csv shouldBe "Title,Author,Artist"
            completions shouldBe 1
        }
    }

    @Test
    fun joinsRowsWithCrLf() = runTest {
        val (csv, _) = export(listOf(manga("A", "B", "C"), manga("D", "E", "F")))
        csv shouldBe "A,B,C$CR${LF}D,E,F"
    }

    @Test
    fun blankAndEmptyCases() = runTest {
        val (blank, _) = export(listOf(manga("   ", null, "")))
        blank shouldBe ",,"
        stream = ByteArrayOutputStream()
        val (none, completions) = export(emptyList())
        none shouldBe ""
        completions shouldBe 1
    }

    @Test
    fun quotesCommaAndNewline() = runTest {
        val (csv, _) = export(listOf(manga("a,b", "c${LF}d", "e${CR}f")))
        csv shouldBe "${QUOTE}a,b$QUOTE,${QUOTE}c${LF}d$QUOTE,${QUOTE}e${CR}f$QUOTE"
    }

    @Test
    fun doublesEmbeddedQuotes() = runTest {
        val (csv, _) = export(listOf(manga("say ${QUOTE}hi$QUOTE", "plain", "plain")))
        csv shouldBe "${QUOTE}say $QUOTE${QUOTE}hi$QUOTE$QUOTE$QUOTE,plain,plain"
    }

    @Test
    fun titleOnlyOptionSkipsRest() = runTest {
        val options = ExportOptions(includeTitle = true, includeAuthor = false, includeArtist = false)
        val (csv, _) = export(favorites = listOf(manga("Only", "Author", "Artist")), options = options)
        csv shouldBe "Only"
    }

    @Test
    fun authorAndArtistOnly() = runTest {
        val options = ExportOptions(includeTitle = false, includeAuthor = true, includeArtist = true)
        val (csv, _) = export(favorites = listOf(manga("Title", "Author", "Artist")), options = options)
        csv shouldBe "Author,Artist"
    }

    @Test
    fun missingStreamStillCompletes() = runTest {
        stream = null
        var completions = 0
        LibraryExporter.exportToCsv(
            context = context,
            uri = uri,
            favorites = listOf(manga("T", "A", "R")),
            options = allColumns,
            onExportComplete = { completions++ },
        )
        completions shouldBe 1
    }

    @Test
    fun optionsDataClassBehaves() {
        allColumns shouldBe allColumns.copy()
        allColumns.hashCode() shouldBe allColumns.copy().hashCode()
        allColumns.toString() shouldBe "ExportOptions(includeTitle=true, includeAuthor=true, includeArtist=true)"
        allColumns.includeTitle shouldBe true
    }
}
