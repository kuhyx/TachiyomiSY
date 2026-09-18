package tachiyomi.data

import app.cash.sqldelight.db.SqlDriver
import io.mockk.every
import io.mockk.mockk
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope
import java.lang.reflect.Type

/**
 * Everything this module's code pulls from [Injekt]: the [driver] behind the hand-written
 * `LibraryQuery`/`UpdatesQuery`, and a [GetCustomMangaInfo] with no edits for the domain models
 * that look up a custom title or cover. The domain companions cache that lookup once per JVM, so it
 * is a real instance rather than a mock. [install] swaps the global scope in; [uninstall] puts the
 * previous one back.
 */
internal class InjektHarness(val driver: SqlDriver = inMemoryDriver()) {
    val database: Database = databaseOn(driver)
    private var previous: InjektScope? = null
    private val registrar: InjektRegistrar = mockk {
        every { getInstance<Any>(any<Type>()) } answers { serve(firstArg()) }
    }

    private fun serve(type: Type): Any = when (type) {
        SqlDriver::class.java -> driver
        GetCustomMangaInfo::class.java -> customMangaInfo
        else -> error("Injekt type not served by InjektHarness: $type")
    }

    /** Makes this harness the global [Injekt] scope. */
    fun install() {
        previous = Injekt
        Injekt = InjektScope(registrar)
    }

    /** Restores the [Injekt] scope that was active before [install]. */
    fun uninstall() {
        previous?.let { Injekt = it }
        previous = null
    }

    private companion object {
        val customMangaInfo: GetCustomMangaInfo = GetCustomMangaInfo(NoCustomManga)
    }
}

/** A custom-manga store holding no edits. */
private object NoCustomManga : CustomMangaRepository {
    override fun get(mangaId: Long): CustomMangaInfo? = null

    override fun set(mangaInfo: CustomMangaInfo) = Unit
}
