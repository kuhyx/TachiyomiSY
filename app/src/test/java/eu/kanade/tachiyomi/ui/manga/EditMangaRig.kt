package eu.kanade.tachiyomi.ui.manga

import android.app.Application
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.databinding.EditMangaDialogBinding
import eu.kanade.tachiyomi.source.online.installSilentXLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.CustomMangaRepository

/** Custom info kept in a map, so a favorited manga reads the user's edits back. */
internal class MapCustomInfo : CustomMangaRepository {
    val infos: MutableMap<Long, CustomMangaInfo> = mutableMapOf()

    override fun get(mangaId: Long): CustomMangaInfo? = infos[mangaId]

    override fun set(mangaInfo: CustomMangaInfo) {
        infos[mangaInfo.id] = mangaInfo
    }
}

/** The edit dialog's inflated form, in the app theme, over Koin holding [MapCustomInfo]. */
internal class EditMangaRig {
    val custom: MapCustomInfo = MapCustomInfo()
    val context: ContextThemeWrapper =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_Tachiyomi)
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    lateinit var binding: EditMangaDialogBinding

    fun start(vararg extra: Module) {
        installSilentXLog()
        stopKoin()
        startKoin {
            modules(
                module {
                    single { GetCustomMangaInfo(custom) }
                    single { BasePreferences(context.applicationContext as Application, MapPreferenceStore()) }
                },
                *extra,
            )
        }
        binding = inflate()
    }

    fun inflate(): EditMangaDialogBinding = EditMangaDialogBinding.inflate(LayoutInflater.from(context))

    fun stop() {
        stopKoin()
    }

    /** A favorite whose shown values are [info]'s edits over the source's own. */
    fun edited(info: CustomMangaInfo?, source: Long = 5L, base: Manga = sourced()): Manga {
        if (info != null) custom.set(info.copy(id = base.id))
        return base.copy(favorite = true, source = source)
    }

    fun sourced(): Manga = Manga.create().copy(
        id = 1L,
        url = "/m/1",
        ogTitle = "Og title",
        ogAuthor = "Og author",
        ogArtist = "Og artist",
        ogDescription = "An original description\nthat is long",
        ogThumbnailUrl = "https://example.org/a/very/long/path/to/the/cover/image.jpeg",
        ogGenre = listOf("one", " "),
        ogStatus = 1L,
    )
}
