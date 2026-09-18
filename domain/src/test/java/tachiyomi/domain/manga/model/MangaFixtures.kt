package tachiyomi.domain.manga.model

import io.mockk.every
import io.mockk.isMockKMock
import io.mockk.mockk
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope
import java.util.concurrent.ConcurrentHashMap

/**
 * Shared custom-info lookup for the [Manga] and [MangaCover] companions.
 *
 * Both companions resolve their `GetCustomMangaInfo` through Injekt exactly once per JVM, so
 * the scope is installed in this object's initialiser and both lazies are forced right away.
 * Answers are keyed by manga id ([customInfos]) instead of re-stubbed per test, which keeps
 * every test class order-independent. The scope is never restored on purpose.
 */
internal object MangaFixtures {
    /** Id of a favourite whose custom info overrides every field. */
    const val CUSTOM_ID: Long = 9_100_001L

    /** Id of a favourite whose custom info exists but has every field null. */
    const val BLANK_CUSTOM_ID: Long = 9_100_002L

    /** Id of a favourite with no custom info at all. */
    const val PLAIN_FAVORITE_ID: Long = 9_100_003L

    val fullCustomInfo: CustomMangaInfo = CustomMangaInfo(
        id = CUSTOM_ID,
        title = "Custom title",
        author = "Custom author",
        artist = "Custom artist",
        thumbnailUrl = "https://example.com/custom.png",
        description = "Custom description",
        genre = listOf("Custom genre"),
        status = 5L,
    )

    val blankCustomInfo: CustomMangaInfo = CustomMangaInfo(id = BLANK_CUSTOM_ID, title = null)

    /** What the shared lookup answers, keyed by manga id; unknown ids answer null. */
    val customInfos: MutableMap<Long, CustomMangaInfo> = ConcurrentHashMap<Long, CustomMangaInfo>().apply {
        put(CUSTOM_ID, fullCustomInfo)
        put(BLANK_CUSTOM_ID, blankCustomInfo)
    }

    val customMangaRepository: CustomMangaRepository = object : CustomMangaRepository {
        override fun get(mangaId: Long): CustomMangaInfo? = customInfos[mangaId]

        override fun set(mangaInfo: CustomMangaInfo) {
            customInfos[mangaInfo.id] = mangaInfo
        }
    }

    val getCustomMangaInfo: GetCustomMangaInfo = GetCustomMangaInfo(customMangaRepository)

    private val registrar: InjektRegistrar = mockk {
        every { getInstance<GetCustomMangaInfo>(any()) } returns getCustomMangaInfo
    }

    init {
        Injekt = InjektScope(registrar)
        // Force both companions' lazies now, while this scope is the live one.
        Manga.create().copy(id = PLAIN_FAVORITE_ID, favorite = true)
        MangaCover(mangaId = PLAIN_FAVORITE_ID, sourceId = 1L, isMangaFavorite = true, ogUrl = null, lastModified = 0L)
        adoptForeignLookup(Manga::class.java)
        adoptForeignLookup(MangaCover::class.java)
    }

    /** A blank non-favourite manga with [id]. */
    fun manga(id: Long = 1L): Manga = Manga.create().copy(id = id)

    /** A favourite manga with [id]; custom info applies when [customInfos] knows the id. */
    fun favorite(id: Long): Manga = Manga.create().copy(id = id, favorite = true)

    /** A cover for manga [mangaId]; going through the fixture guarantees the scope is installed first. */
    fun cover(mangaId: Long, favorite: Boolean, ogUrl: String? = "https://example.com/og.png"): MangaCover =
        MangaCover(mangaId = mangaId, sourceId = 3L, isMangaFavorite = favorite, ogUrl = ogUrl, lastModified = 9L)

    private fun adoptForeignLookup(owner: Class<*>) {
        // If another fixture resolved this companion's lazy before this object initialised, route that
        // mock's answers through customInfos so the id-keyed expectations still hold. A non-mock
        // foreign instance cannot be adopted.
        val field = owner.declaredFields.firstOrNull { it.name == "getCustomMangaInfo\$delegate" } ?: return
        field.isAccessible = true
        val resolved = (field.get(null) as Lazy<*>).value
        if (resolved !== getCustomMangaInfo && resolved is GetCustomMangaInfo && isMockKMock(resolved)) {
            every { resolved.get(any()) } answers { customInfos[firstArg()] }
        }
    }
}
