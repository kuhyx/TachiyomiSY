package eu.kanade.tachiyomi.source

import android.content.Context
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.renameSource
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.getSourceData
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.Lanraragi
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.source.online.all.NHentai
import eu.kanade.tachiyomi.source.online.english.EightMuses
import eu.kanade.tachiyomi.source.online.english.HBrowse
import eu.kanade.tachiyomi.source.online.english.Pururin
import eu.kanade.tachiyomi.source.online.english.Tsumino
import exh.source.BlacklistedSources
import exh.source.DelegatedHttpSource
import exh.source.EIGHTMUSES_SOURCE_ID
import exh.source.EnhancedHttpSource
import exh.source.ExhPreferences
import exh.source.HBROWSE_SOURCE_ID
import exh.source.PURURIN_SOURCE_ID
import exh.source.TSUMINO_SOURCE_ID
import exh.source.handleSourceLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

internal class AndroidSourceManager(
    internal val context: Context,
    internal val extensionManager: ExtensionManager,
    internal val sourceRepository: StubSourceRepository,
) : SourceManager {

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val downloadManager: DownloadManager by injectLazy()

    internal val scope = CoroutineScope(Job() + Dispatchers.IO)

    internal val sourcesMapFlow = MutableStateFlow(ConcurrentHashMap<Long, Source>())

    internal val stubSourcesMap = ConcurrentHashMap<Long, StubSource>()

    override val sources: Flow<List<Source>> = sourcesMapFlow.map { it.values.toList() }

    // SY -->
    internal val exhPreferences: ExhPreferences by injectLazy()
    internal val sourcePreferences: SourcePreferences by injectLazy()
    // SY <--

    init {
        observeExtensions()
        observeStubSources()
    }

    internal fun markInitialized() {
        _isInitialized.value = true
    }

    override fun get(sourceKey: Long): Source? = sourcesMapFlow.value[sourceKey]

    override fun getOrStub(sourceKey: Long): Source {
        return sourcesMapFlow.value[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
    }

    override fun getAll() = sourcesMapFlow.value.values.toList()

    override fun getOnlineSources() = sourcesMapFlow.value.values.filterIsInstance<HttpSource>()

    override fun getStubSources(): List<StubSource> {
        val onlineSourceIds = getOnlineSources().map { it.id }
        return stubSourcesMap.values.filterNot { it.id in onlineSourceIds }
    }

    // SY -->
    override fun getVisibleOnlineSources() = sourcesMapFlow.value.values
        .filterIsInstance<HttpSource>()
        .filter {
            it.id !in BlacklistedSources.HIDDEN_SOURCES
        }

    override fun getVisibleSources() = sourcesMapFlow.value.values
        .filter {
            it.id !in BlacklistedSources.HIDDEN_SOURCES
        }

    fun getDelegatedCatalogueSources() = sourcesMapFlow.value.values
        .filterIsInstance<EnhancedHttpSource>()
        .mapNotNull { enhancedHttpSource ->
            enhancedHttpSource.enhancedSource as? DelegatedHttpSource
        }
    // SY <--

    internal fun registerStubSource(source: StubSource) {
        scope.launch {
            val dbSource = sourceRepository.getStubSource(source.id)
            if (dbSource != source) {
                sourceRepository.upsertStubSource(source.id, source.lang, source.name)
                if (dbSource != null) {
                    downloadManager.renameSource(dbSource, source)
                }
            }
        }
    }

    // A stub already stored, else one built from the extension's data (and stored), else a nameless one.
    private suspend fun createStubSource(id: Long): StubSource =
        sourceRepository.getStubSource(id)
            ?: extensionManager.getSourceData(id)?.also { registerStubSource(it) }
            ?: StubSource(id = id, lang = "", name = "")

    // SY -->
    companion object {
        private const val fillInSourceId = Long.MAX_VALUE
        val DELEGATED_SOURCES = listOf(
            DelegatedSource(
                "Pururin",
                PURURIN_SOURCE_ID,
                "eu.kanade.tachiyomi.extension.en.pururin.Pururin",
                Pururin::class,
            ),
            DelegatedSource(
                "Tsumino",
                TSUMINO_SOURCE_ID,
                "eu.kanade.tachiyomi.extension.en.tsumino.Tsumino",
                Tsumino::class,
            ),
            DelegatedSource(
                "MangaDex",
                fillInSourceId,
                "eu.kanade.tachiyomi.extension.all.mangadex",
                MangaDex::class,
                true,
            ),
            DelegatedSource(
                "HBrowse",
                HBROWSE_SOURCE_ID,
                "eu.kanade.tachiyomi.extension.en.hbrowse.HBrowse",
                HBrowse::class,
            ),
            DelegatedSource(
                "8Muses",
                EIGHTMUSES_SOURCE_ID,
                "eu.kanade.tachiyomi.extension.en.eightmuses.EightMuses",
                EightMuses::class,
            ),
            DelegatedSource(
                "NHentai",
                fillInSourceId,
                "eu.kanade.tachiyomi.extension.all.nhentai.NHentai",
                NHentai::class,
                true,
            ),
            DelegatedSource(
                "LANraragi",
                fillInSourceId,
                "eu.kanade.tachiyomi.extension.all.lanraragi.LANraragi",
                Lanraragi::class,
                true,
            ),
        ).associateBy { it.originalSourceQualifiedClassName }

        val currentDelegatedSources: MutableMap<Long, DelegatedSource> =
            ListenMutableMap(mutableMapOf(), ::handleSourceLibrary)

        data class DelegatedSource(
            val sourceName: String,
            val sourceId: Long,
            val originalSourceQualifiedClassName: String,
            val newSourceClass: KClass<out DelegatedHttpSource>,
            val factory: Boolean = false,
        )
    }

    // SY <--
}
