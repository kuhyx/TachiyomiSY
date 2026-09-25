package exh.debug

import eu.kanade.tachiyomi.source.AndroidSourceManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.all.NHentai
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.source.service.SourceManager

internal class DebugSourceFunctionsTest {
    @Test
    fun describesEverySourceList() {
        DebugSourceFunctions.listAllSources() shouldBe "1: Alpha (EN)\n2: Beta (JA)"
        DebugSourceFunctions.listVisibleSources() shouldBe "1: Alpha (EN)"
        DebugSourceFunctions.listAllHttpSources() shouldBe "3: Web (DE)"
        DebugSourceFunctions.listVisibleHttpSources() shouldBe "3: Web (DE)"
        DebugSourceFunctions.listAllSourcesClassName().lines().size shouldBe 2
        DebugSourceFunctions.listAllSourcesClassName() shouldBe
            "${alpha::class.qualifiedName}: Alpha (EN)\n${beta::class.qualifiedName}: Beta (JA)"
        describe(alpha) shouldBe "1: Alpha (EN)"
    }

    @Test
    fun delegatedSourcesAreListed() {
        AndroidSourceManager.currentDelegatedSources[9L] = AndroidSourceManager.Companion.DelegatedSource(
            sourceName = "Nine",
            sourceId = 9L,
            originalSourceQualifiedClassName = "c9",
            newSourceClass = NHentai::class,
            factory = true,
        )
        DebugSourceFunctions.getDelegatedSourceList() shouldBe "Nine : 9 : true"
        AndroidSourceManager.currentDelegatedSources.remove(9L)
        DebugSourceFunctions.getDelegatedSourceList() shouldBe ""
    }

    private companion object {
        val alpha = source(id = 1, sourceName = "Alpha", language = "en")
        val beta = source(id = 2, sourceName = "Beta", language = "ja")
        val web = httpSource(id = 3, sourceName = "Web", language = "de")
        val sourceManager = mockk<SourceManager> {
            every { getAll() } returns listOf(alpha, beta)
            every { getVisibleSources() } returns listOf(alpha)
            every { getOnlineSources() } returns listOf(web)
            every { getVisibleOnlineSources() } returns listOf(web)
        }

        fun source(id: Long, sourceName: String, language: String): Source {
            val source = mockk<Source>()
            every { source.id } returns id
            every { source.name } returns sourceName
            every { source.lang } returns language
            return source
        }

        fun httpSource(id: Long, sourceName: String, language: String): HttpSource {
            val source = mockk<HttpSource>()
            every { source.id } returns id
            every { source.name } returns sourceName
            every { source.lang } returns language
            return source
        }

        @JvmStatic
        @BeforeAll
        fun before() {
            stopKoin()
            startKoin { modules(module { single<SourceManager> { sourceManager } }) }
        }

        @JvmStatic
        @AfterAll
        fun after() = stopKoin()
    }
}
