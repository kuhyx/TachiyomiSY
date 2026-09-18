package exh.source

import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.StubSource

/** Marks the extension half of a mocked [EnhancedHttpSource]. */
internal interface OriginalMarker

/** Marks the in-app half of a mocked [EnhancedHttpSource]. */
internal interface EnhancedMarker

internal class EnhancedSourceHelpersTest {

    private val original: HttpSource = mockk(moreInterfaces = arrayOf(OriginalMarker::class))
    private val enhanced: HttpSource = mockk(moreInterfaces = arrayOf(EnhancedMarker::class))
    private val wrapper: EnhancedHttpSource = mockk {
        every { source() } returns enhanced
        every { originalSource } returns original
        every { enhancedSource } returns enhanced
    }
    private val stub = StubSource(id = 1L, lang = "en", name = "Src")

    @Test
    fun mainSourceOfWrapperIsActive() {
        wrapper.getMainSource() shouldBe enhanced
    }

    @Test
    fun mainSourceOfPlainIsItself() {
        stub.getMainSource() shouldBe stub
    }

    @Test
    fun typedMainSourceCasts() {
        wrapper.getMainSource<HttpSource>() shouldBe enhanced
        wrapper.getMainSource<StubSource>() shouldBe null
        stub.getMainSource<StubSource>() shouldBe stub
        stub.getMainSource<HttpSource>() shouldBe null
    }

    @Test
    fun originalSourceUnwraps() {
        wrapper.getOriginalSource() shouldBe original
        stub.getOriginalSource() shouldBe stub
    }

    @Test
    fun enhancedSourceUnwraps() {
        wrapper.getEnhancedSource() shouldBe enhanced
        stub.getEnhancedSource() shouldBe stub
    }

    @Test
    fun anyIsChecksBothHalves() {
        wrapper.anyIs<OriginalMarker>() shouldBe true
        wrapper.anyIs<EnhancedMarker>() shouldBe true
        wrapper.anyIs<StubSource>() shouldBe false
    }

    @Test
    fun anyIsChecksPlainSource() {
        stub.anyIs<StubSource>() shouldBe true
        stub.anyIs<OriginalMarker>() shouldBe false
    }
}
