package mihon.domain.chapter.interactor

import io.mockk.every
import io.mockk.mockk
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope

private val noCustomInfo: GetCustomMangaInfo = mockk<GetCustomMangaInfo>().also { info ->
    every { info.get(any()) } returns null
}
private val registrar: InjektRegistrar = mockk<InjektRegistrar>().also { mock ->
    every { mock.getInstance<GetCustomMangaInfo>(any()) } returns noCustomInfo
}

/**
 * A favourite of [source] with row id [id]. A favourite [Manga] reads [GetCustomMangaInfo]
 * through Injekt in its constructor and the companion resolves it once per JVM, so the scope
 * is pointed at a "no custom info" stub for the construction and restored right after; the
 * stub itself lives for the whole run.
 */
internal fun favoriteManga(id: Long, source: Long): Manga {
    val previous = Injekt
    Injekt = InjektScope(registrar)
    try {
        return Manga.create().copy(id = id, source = source, favorite = true)
    } finally {
        Injekt = previous
    }
}
