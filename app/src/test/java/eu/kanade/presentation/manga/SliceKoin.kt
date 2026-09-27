package eu.kanade.presentation.manga

import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.PresentationKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/** Starts [PresentationKoin] with the base and source preferences the manga screens also read (English only). */
internal fun PresentationKoin.startWithPreferences(vararg extra: Module) {
    val sourcePreferences = SourcePreferences(store)
    sourcePreferences.enabledLanguages.set(setOf("en"))
    val base = BasePreferences(ApplicationProvider.getApplicationContext(), store)
    start(
        module {
            single { sourcePreferences }
            single { base }
        },
        *extra,
    )
}
