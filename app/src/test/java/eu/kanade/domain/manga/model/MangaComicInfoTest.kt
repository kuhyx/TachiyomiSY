package eu.kanade.domain.manga.model

import eu.kanade.domain.installFakeAndroidKeyStore
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

internal class MangaComicInfoTest {

    private val securityPreferences = SecurityPreferences(InMemoryPreferenceStore())

    @BeforeEach
    fun setUp() {
        installFakeAndroidKeyStore()
        startKoin { modules(module { single { securityPreferences } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun fillsEveryFieldFromTheModels() {
        val manga = Manga.create().copy(
            ogTitle = "Series",
            ogDescription = "About",
            ogAuthor = "Writer",
            ogArtist = "Artist",
            ogGenre = listOf("a", "b"),
            ogStatus = SManga.ONGOING.toLong(),
        )
        val chapter = Chapter.create().copy(name = "Ch", chapterNumber = 2.0, scanlator = "Group")
        val info = getComicInfo(manga, chapter, listOf("u1", "u2"), listOf("c1", "c2"), "Source")
        info.title!!.value shouldBe "Ch"
        info.series!!.value shouldBe "Series"
        info.number!!.value shouldBe "2"
        info.web!!.value shouldBe "u1 u2"
        info.summary!!.value shouldBe "About"
        info.writer!!.value shouldBe "Writer"
        info.penciller!!.value shouldBe "Artist"
        info.translator!!.value shouldBe "Group"
        info.genre!!.value shouldBe "a, b"
        info.publishingStatus!!.value shouldBe "Ongoing"
        info.categories!!.value shouldBe "c1, c2"
        info.source!!.value shouldBe "Source"
        info.padding.shouldBeNull()
    }

    @Test
    fun leavesMissingFieldsNull() {
        val chapter = Chapter.create().copy(name = "Ch", chapterNumber = 2.5, scanlator = null)
        val info = getComicInfo(Manga.create(), chapter, emptyList(), null, "Source")
        info.number!!.value shouldBe "2.5"
        info.summary.shouldBeNull()
        info.writer.shouldBeNull()
        info.penciller.shouldBeNull()
        info.translator.shouldBeNull()
        info.genre.shouldBeNull()
        info.categories.shouldBeNull()
        getComicInfo(Manga.create(), chapter.copy(chapterNumber = -1.0), emptyList(), null, "S").number.shouldBeNull()
    }

    @Test
    fun padsProtectedDownloads() {
        mockkObject(CbzCrypto)
        every { CbzCrypto.createComicInfoPadding() } returns "pad"
        val info = getComicInfo(Manga.create(), Chapter.create(), emptyList(), null, "Source")
        info.padding!!.value shouldBe "pad"
    }
}
