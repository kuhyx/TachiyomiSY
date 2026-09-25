package eu.kanade.tachiyomi.source

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.all.nhentai.NHentaiFake
import eu.kanade.tachiyomi.extension.en.pururin.Pururin
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.online.FakeDelegateSource
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.all.FakeParentTable
import eu.kanade.tachiyomi.source.online.all.NHentai
import eu.kanade.tachiyomi.source.online.serveMetadataSource
import exh.eh.EHentaiUpdateHelper
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.EnhancedHttpSource
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.repository.StubSourceRepository
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem
import java.util.concurrent.ConcurrentHashMap

private const val WAIT_MS = 10_000L

@RunWith(RobolectricTestRunner::class)
internal class AndroidSourceManagerTest {
    private val harness = SourceTestHarness()
    private val extensions = MutableStateFlow<List<Extension.Installed>>(emptyList())
    private val stubs = MutableStateFlow<List<StubSource>>(emptyList())
    private val extensionManager = mockk<ExtensionManager>()
    private val repository = mockk<StubSourceRepository>()
    private val nhentai = NHentaiFake("http://n.example")
    private val pururin = Pururin("http://p.example")
    private val plain = FakeDelegateSource("http://plain.example", name = "Plain")
    private val blacklisted = FakeDelegateSource("http://eh.example", lang = "en", name = "E-Hentai")
    private val managers = mutableListOf<AndroidSourceManager>()

    @Before
    fun setUp() {
        harness.install()
        harness.serveMetadataSource()
        harness.serve(SourcePreferences(harness.store))
        harness.serve<LocalSourceFileSystem>(mockk())
        harness.serve<LocalCoverManager>(mockk())
        harness.serve<EHentaiUpdateHelper>(mockk { every { parentLookupTable } returns FakeParentTable().table })
        val downloadProvider = mockk<DownloadProvider> { every { findSourceDir(any()) } returns null }
        val downloadManager = mockk<DownloadManager>()
        every { downloadManager.provider } returns downloadProvider
        harness.serve(downloadManager)
        every { extensionManager.installedExtensionsFlow } returns extensions
        every { extensionManager.availableExtensionsSourcesData } returns emptyMap()
        every { repository.subscribeAll() } returns stubs
        coEvery { repository.getStubSource(any()) } returns null
        coEvery { repository.upsertStubSource(any(), any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        // The manager's scope outlives the test; stop it before the served graph goes away.
        managers.forEach { it.scope.cancel() }
        harness.uninstall()
    }

    private fun installed(vararg sources: Source) = Extension.Installed(
        name = "ext",
        pkgName = "eu.kanade.tachiyomi.extension.test",
        versionName = "1",
        versionCode = 1L,
        libVersion = 1.5,
        lang = "all",
        isNsfw = false,
        pkgFactory = null,
        sources = sources.toList(),
        icon = null,
        isShared = false,
    )

    private fun manager(): AndroidSourceManager =
        AndroidSourceManager(harness.application, extensionManager, repository).also { managers += it }

    private fun AndroidSourceManager.awaitInitialized(): AndroidSourceManager = apply {
        runBlocking { withTimeout(WAIT_MS) { isInitialized.first { it } } }
    }

    @Test
    fun builtInSourcesOnly() {
        val manager = manager().awaitInitialized()
        manager.getAll().map { it.id }.toSet() shouldBe setOf(LocalSource.ID, EH_SOURCE_ID, MERGED_SOURCE_ID)
        manager.get(EH_SOURCE_ID)?.name shouldBe "E-Hentai"
        manager.get(EXH_SOURCE_ID).shouldBeNull()
        manager.getOnlineSources().map { it.id }.toSet() shouldBe setOf(EH_SOURCE_ID, MERGED_SOURCE_ID)
        manager.getVisibleOnlineSources().map { it.id } shouldContainExactly listOf(EH_SOURCE_ID)
        manager.getVisibleSources().map { it.id }.toSet() shouldBe setOf(LocalSource.ID, EH_SOURCE_ID)
        manager.getDelegatedCatalogueSources().isEmpty() shouldBe true
        runBlocking { manager.sources.first() }.size shouldBe 3
    }

    @Test
    fun exhentaiFollowsPreference() {
        val manager = manager().awaitInitialized()
        harness.exhPreferences.enableExhentai.set(true)
        runBlocking { withTimeout(WAIT_MS) { manager.sourcesMapFlow.first { it.containsKey(EXH_SOURCE_ID) } } }
        manager.get(EXH_SOURCE_ID)?.name shouldBe "ExHentai"
    }

    @Test
    fun extensionsAreDelegated() {
        extensions.value = listOf(installed(nhentai, pururin, plain, blacklisted))
        val manager = manager().awaitInitialized()
        val delegated = manager.get(nhentai.id) as EnhancedHttpSource
        (delegated.enhancedSource is NHentai) shouldBe true
        (manager.get(pururin.id) as EnhancedHttpSource).enhancedSource.javaClass.simpleName shouldBe "Pururin"
        manager.get(plain.id) shouldBe plain
        manager.get(blacklisted.id).shouldBeNull()
        manager.getDelegatedCatalogueSources().size shouldBe 2
        AndroidSourceManager.currentDelegatedSources[nhentai.id]?.factory shouldBe true
        AndroidSourceManager.currentDelegatedSources[pururin.id]?.factory shouldBe false
        coVerify(timeout = WAIT_MS) { repository.upsertStubSource(plain.id, plain.lang, plain.name) }
    }

    @Test
    fun internalSourceEdgeCases() {
        val manager = manager().awaitInitialized()
        val anonymous = object : FakeDelegateSource("http://anon.example", name = "Anon") {}
        manager.toInternalSource(anonymous) shouldBe anonymous
        val notHttp = mockk<Source> { every { id } returns 5L }
        manager.toInternalSource(notHttp) shouldBe notHttp
        manager.toInternalSource(blacklisted).shouldBeNull()
        // An enhanced source whose replacement is a plain extension source, which is not delegated.
        manager.sourcesMapFlow.value = ConcurrentHashMap(mapOf(9L to EnhancedHttpSource(plain, anonymous)))
        manager.getDelegatedCatalogueSources().isEmpty() shouldBe true
    }

    @Test
    fun stubSourcesFromEverySource() {
        coEvery { repository.getStubSource(11L) } returns StubSource(id = 11L, lang = "en", name = "Stored")
        every { extensionManager.availableExtensionsSourcesData } returns
            mapOf(12L to StubSource(id = 12L, lang = "en", name = "Available"))
        val manager = manager().awaitInitialized()
        manager.getOrStub(EH_SOURCE_ID).id shouldBe EH_SOURCE_ID
        manager.getOrStub(11L).name shouldBe "Stored"
        manager.getOrStub(12L).name shouldBe "Available"
        manager.getOrStub(12L).name shouldBe "Available"
        manager.getOrStub(13L).name shouldBe ""
        coVerify(timeout = WAIT_MS) { repository.upsertStubSource(12L, "en", "Available") }
        manager.getStubSources().map { it.id }.toSet() shouldBe setOf(11L, 12L, 13L)
    }

    @Test
    fun registerStubSourceBranches() {
        val stored = StubSource(id = 21L, lang = "en", name = "Old")
        coEvery { repository.getStubSource(21L) } returns stored
        // StubSource is not a data class, so "unchanged" means the very instance the repository returned.
        val same = StubSource(id = 22L, lang = "en", name = "Same")
        coEvery { repository.getStubSource(22L) } returns same
        val manager = manager().awaitInitialized()
        manager.registerStubSource(StubSource(id = 21L, lang = "en", name = "New"))
        coVerify(timeout = WAIT_MS) { repository.upsertStubSource(21L, "en", "New") }
        manager.registerStubSource(same)
        manager.registerStubSource(StubSource(id = 23L, lang = "en", name = "Fresh"))
        coVerify(timeout = WAIT_MS) { repository.upsertStubSource(23L, "en", "Fresh") }
        coVerify(exactly = 0) { repository.upsertStubSource(22L, any(), any()) }
        stubs.value = listOf(stored)
        manager.stubSourcesMap.isEmpty() shouldBe true
    }

    @Test
    fun delegatedSourceTable() {
        AndroidSourceManager.DELEGATED_SOURCES.size shouldBe 7
        val entry = AndroidSourceManager.DELEGATED_SOURCES.getValue("eu.kanade.tachiyomi.extension.en.pururin.Pururin")
        entry.sourceName shouldBe "Pururin"
        entry.factory shouldBe false
        entry.copy(sourceId = 1L).sourceId shouldBe 1L
        val dex = AndroidSourceManager.DELEGATED_SOURCES.getValue("eu.kanade.tachiyomi.extension.all.mangadex")
        dex.factory shouldBe true
        val explicit = AndroidSourceManager.Companion.DelegatedSource("n", 1L, "q", NHentai::class, false)
        explicit.newSourceClass shouldBe NHentai::class
    }
}
