package tachiyomi.domain.history.model

import io.mockk.every
import io.mockk.mockk
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope

/**
 * Swaps [Injekt] for a scope that hands out one shared [GetCustomMangaInfo] mock.
 *
 * The rows that read custom titles resolve the interactor through a companion `injectLazy`, once per
 * JVM, so the mock is held here for the whole run and stubbed per manga id: [CUSTOM_TITLE_MANGA_ID]
 * has an edited title, [NULL_TITLE_MANGA_ID] has edits without a title, every other id has none.
 */
internal object CustomMangaInfoScope {
    const val CUSTOM_TITLE_MANGA_ID: Long = 11L
    const val NULL_TITLE_MANGA_ID: Long = 12L
    const val PLAIN_MANGA_ID: Long = 13L
    const val CUSTOM_TITLE: String = "custom title"

    private val getCustomMangaInfo: GetCustomMangaInfo = mockk()

    private val registrar: InjektRegistrar = mockk()

    private var previous: InjektScope? = null

    init {
        every { getCustomMangaInfo.get(any()) } returns null
        every {
            getCustomMangaInfo.get(CUSTOM_TITLE_MANGA_ID)
        } returns CustomMangaInfo(id = CUSTOM_TITLE_MANGA_ID, title = CUSTOM_TITLE)
        every {
            getCustomMangaInfo.get(NULL_TITLE_MANGA_ID)
        } returns CustomMangaInfo(id = NULL_TITLE_MANGA_ID, title = null)
        every { registrar.getInstance<GetCustomMangaInfo>(any()) } returns getCustomMangaInfo
    }

    /** Installs the scope, remembering the one that was active. */
    fun install() {
        previous = Injekt
        Injekt = InjektScope(registrar)
    }

    /** Puts back the scope that [install] replaced. */
    fun restore() {
        previous?.let { Injekt = it }
        previous = null
    }
}
