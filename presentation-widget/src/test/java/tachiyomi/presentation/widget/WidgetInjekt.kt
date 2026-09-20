package tachiyomi.presentation.widget

import android.app.Application
import android.content.Context
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import io.mockk.every
import io.mockk.mockk
import org.robolectric.RuntimeEnvironment
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.domain.updates.interactor.GetUpdates
import tachiyomi.domain.updates.model.UpdatesWithRelations
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.InjektScope
import java.lang.reflect.Type

/**
 * Everything the widget code pulls from [Injekt]: the Robolectric [Application], one [GetUpdates]
 * mock, real [SecurityPreferences] over a [FlowPreferenceStore], and a [GetCustomMangaInfo] with no
 * edits for the domain models that look up a custom title or cover. The domain companions cache
 * that lookup once per class loader, so it is one shared real instance rather than a mock.
 * [install] swaps the global scope in; [restore] puts the previous one back.
 */
internal object WidgetInjekt {
    lateinit var application: Application
    lateinit var getUpdates: GetUpdates
    lateinit var preferences: SecurityPreferences
    private var previous: InjektScope? = null
    private val customMangaInfo: GetCustomMangaInfo = GetCustomMangaInfo(NoCustomManga)
    private val registrar: InjektRegistrar = mockk {
        every { getInstance<Any>(any<Type>()) } answers { serve(firstArg()) }
    }

    /** Makes this object the global [Injekt] scope, serving [getUpdates] and unlocked [preferences]. */
    fun install(getUpdates: GetUpdates = mockk()) {
        application = RuntimeEnvironment.getApplication()
        this.getUpdates = getUpdates
        preferences = SecurityPreferences(FlowPreferenceStore())
        previous = Injekt
        Injekt = InjektScope(registrar)
    }

    /** Restores the [Injekt] scope that was active before [install]. */
    fun restore() {
        previous?.let { Injekt = it }
        previous = null
    }

    private fun serve(type: Type): Any = when (type) {
        Application::class.java, Context::class.java -> application
        GetUpdates::class.java -> getUpdates
        SecurityPreferences::class.java -> preferences
        GetCustomMangaInfo::class.java -> customMangaInfo
        else -> error("Injekt type not served by WidgetInjekt: $type")
    }
}

/** A custom-manga store holding no edits. */
private object NoCustomManga : CustomMangaRepository {
    override fun get(mangaId: Long): CustomMangaInfo? = null

    override fun set(mangaInfo: CustomMangaInfo) = Unit
}

/** An updates row for manga [mangaId] and chapter [chapterId], with a cover url derived from the manga. */
internal fun updateOf(mangaId: Long, chapterId: Long = mangaId * 10): UpdatesWithRelations = UpdatesWithRelations(
    mangaId = mangaId,
    ogMangaTitle = "Manga $mangaId",
    chapterId = chapterId,
    chapterName = "Chapter $chapterId",
    scanlator = null,
    chapterUrl = "/chapter/$chapterId",
    read = false,
    bookmark = false,
    lastPageRead = 0,
    sourceId = 7,
    dateFetch = 0,
    coverData = MangaCover(
        mangaId = mangaId,
        sourceId = 7,
        isMangaFavorite = true,
        ogUrl = "https://covers.example/$mangaId.png",
        lastModified = 0,
    ),
)
