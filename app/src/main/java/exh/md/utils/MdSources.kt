package exh.md.utils

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.source.getMainSource
import exh.util.nullIfZero
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal fun MdUtil.getEnabledMangaDex(
    sourcePreferences: SourcePreferences = Injekt.get(),
    sourceManager: SourceManager = Injekt.get(),
): MangaDex? {
    return getEnabledMangaDexs(sourcePreferences, sourceManager).let { mangadexs ->
        sourcePreferences.preferredMangaDexId.get().toLongOrNull()?.nullIfZero()
            ?.let { preferredMangaDexId ->
                mangadexs.firstOrNull { it.id == preferredMangaDexId }
            }
            ?: mangadexs.firstOrNull()
    }
}

internal fun MdUtil.getEnabledMangaDexs(
    preferences: SourcePreferences,
    sourceManager: SourceManager = Injekt.get(),
): List<MangaDex> {
    val languages = preferences.enabledLanguages.get()
    val disabledSourceIds = preferences.disabledSources.get()

    return sourceManager.getVisibleOnlineSources()
        .asSequence()
        .mapNotNull { it.getMainSource<MangaDex>() }
        .filter { it.lang in languages }
        .filterNot { it.id.toString() in disabledSourceIds }
        .toList()
}
