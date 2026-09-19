package tachiyomi.source.local

import android.content.Context
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.domain.source.model.Source as DomainSource

/** The constant identity of [LocalSource] and the `isLocal()` helpers built on it. */
@RunWith(RobolectricTestRunner::class)
internal class LocalSourceIdentityTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    private fun source(): LocalSource {
        val fileSystem = fileSystemOver(null)
        return LocalSource(
            context = context,
            fileSystem = fileSystem,
            coverManager = LocalCoverManager(context, fileSystem),
            allowHiddenFiles = { true },
        )
    }

    @Test
    fun describesItself() {
        val source = source()
        source.name shouldBe context.stringResource(MR.strings.local_source)
        source.toString() shouldBe source.name
        source.id shouldBe 0L
        source.lang shouldBe "other"
        source.supportsLatest shouldBe true
    }

    @Test
    fun exposesItsConstants() {
        LocalSource.ID shouldBe 0L
        LocalSource.HELP_URL shouldBe "https://mihon.app/docs/guides/local-source/"
        LocalSource.COMIC_INFO_ARCHIVE shouldBe "ComicInfo.cbm"
    }

    @Test
    fun mangaIsLocalBySourceId() {
        Manga.create().copy(source = LocalSource.ID).isLocal() shouldBe true
        Manga.create().copy(source = 42L).isLocal() shouldBe false
    }

    @Test
    fun sourceIsLocalById() {
        source().isLocal() shouldBe true
        val remote = mockk<Source> { every { id } returns 42L }
        remote.isLocal() shouldBe false
    }

    @Test
    fun domainSourceIsLocalById() {
        domainSource(id = LocalSource.ID).isLocal() shouldBe true
        domainSource(id = 42L).isLocal() shouldBe false
    }

    private fun domainSource(id: Long): DomainSource =
        DomainSource(id = id, lang = "other", name = "Local", supportsLatest = true, isStub = false)
}
