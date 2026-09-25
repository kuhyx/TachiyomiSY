package exh.util

import exh.metadata.metadata.base.RaisedTag
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.PURURIN_SOURCE_ID
import exh.source.TSUMINO_SOURCE_ID
import exh.source.lanraragiSourceIds
import exh.source.mangaDexSourceIds
import exh.source.nHentaiSourceIds
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

internal class SourceTagsUtilTest {
    private val savedNHentai = nHentaiSourceIds
    private val savedMangaDex = mangaDexSourceIds
    private val savedLanraragi = lanraragiSourceIds

    @BeforeEach
    fun setUp() {
        nHentaiSourceIds = listOf(NHENTAI)
        mangaDexSourceIds = listOf(MANGADEX)
        lanraragiSourceIds = listOf(LANRARAGI)
    }

    @AfterEach
    fun tearDown() {
        nHentaiSourceIds = savedNHentai
        mangaDexSourceIds = savedMangaDex
        lanraragiSourceIds = savedLanraragi
    }

    @Test
    fun unsupportedSourcesGetNothing() {
        SourceTagsUtil.getWrappedTag(null, fullTag = "a:b").shouldBeNull()
        SourceTagsUtil.getWrappedTag(99L, fullTag = "a:b").shouldBeNull()
    }

    @Test
    fun incompleteInputGetsNothing() {
        SourceTagsUtil.getWrappedTag(EH_SOURCE_ID).shouldBeNull()
        SourceTagsUtil.getWrappedTag(EH_SOURCE_ID, namespace = "a").shouldBeNull()
        SourceTagsUtil.getWrappedTag(EH_SOURCE_ID, tag = "b").shouldBeNull()
        SourceTagsUtil.getWrappedTag(EH_SOURCE_ID, fullTag = "no namespace").shouldBeNull()
    }

    @Test
    fun ehWrapsWithDollar() {
        SourceTagsUtil.getWrappedTag(EH_SOURCE_ID, namespace = "female", tag = "x y") shouldBe "female:\"x y$\""
        SourceTagsUtil.getWrappedTag(EXH_SOURCE_ID, fullTag = "male:solo | alias") shouldBe "male:solo$"
        SourceTagsUtil.getWrappedTag(LANRARAGI, fullTag = "artist:someone") shouldBe "artist:someone$"
    }

    @Test
    fun nhentaiWrapsQuotedSpaces() {
        SourceTagsUtil.getWrappedTag(NHENTAI, fullTag = "tag:big x") shouldBe "\"big x\""
        SourceTagsUtil.getWrappedTag(NHENTAI, fullTag = "artist:a b") shouldBe "artist:\"a b\""
        SourceTagsUtil.getWrappedTag(NHENTAI, fullTag = "tag:solo") shouldBe "tag:solo"
    }

    @Test
    fun mangaDexAndPururinUseName() {
        SourceTagsUtil.getWrappedTag(MANGADEX, fullTag = "genre:Sci Fi | x") shouldBe "Sci Fi | x"
        SourceTagsUtil.getWrappedTag(PURURIN_SOURCE_ID, fullTag = "genre:Sci Fi | x") shouldBe "Sci Fi"
    }

    @Test
    fun tsuminoUnderscoresSpaces() {
        SourceTagsUtil.getWrappedTag(TSUMINO_SOURCE_ID, fullTag = "tags:big x") shouldBe "\"big_x\""
        SourceTagsUtil.getWrappedTag(TSUMINO_SOURCE_ID, fullTag = "artist:a b") shouldBe "\"artist: a_b\""
        SourceTagsUtil.getWrappedTag(TSUMINO_SOURCE_ID, fullTag = "tags:solo") shouldBe "solo"
        SourceTagsUtil.getWrappedTag(TSUMINO_SOURCE_ID, fullTag = "artist:solo") shouldBe "artist:solo"
    }

    @Test
    fun parseTagSplitsAndExcludes() {
        SourceTagsUtil.parseTag("female:x y") shouldBe RaisedTag("female", "x y", 1)
        SourceTagsUtil.parseTag("-female:x") shouldBe RaisedTag("female", "x", 69)
        SourceTagsUtil.parseTag("loose") shouldBe RaisedTag(null, "loose", 1)
        SourceTagsUtil.parseTag(" : x ") shouldBe RaisedTag(null, "x", 1)
    }

    @Test
    fun localeLookup() {
        SourceTagsUtil.getLocaleSourceUtil("english") shouldBe Locale.forLanguageTag("en")
        SourceTagsUtil.getLocaleSourceUtil("eng") shouldBe Locale.forLanguageTag("en")
        SourceTagsUtil.getLocaleSourceUtil("polish") shouldBe Locale.forLanguageTag("pl")
        SourceTagsUtil.getLocaleSourceUtil("klingon").shouldBeNull()
        SourceTagsUtil.getLocaleSourceUtil(null).shouldBeNull()
    }

    private companion object {
        const val NHENTAI = 1001L
        const val MANGADEX = 1002L
        const val LANRARAGI = 1003L
    }
}
