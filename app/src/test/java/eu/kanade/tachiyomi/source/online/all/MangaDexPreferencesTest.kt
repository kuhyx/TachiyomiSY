package eu.kanade.tachiyomi.source.online.all

import android.content.SharedPreferences
import exh.md.utils.MdLang
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A preferences backend may answer null for a string; the MangaDex settings read that as empty. */
@RunWith(RobolectricTestRunner::class)
internal class MangaDexPreferencesTest {

    private val preferences = mockk<SharedPreferences> {
        every { getString(any(), any()) } returns null
    }
    private val mangaDex = mockk<MangaDex> {
        every { sourcePreferences } returns preferences
        every { mdLang } returns MdLang.ENGLISH
    }

    @Test
    fun nullStringsReadAsEmpty() {
        mangaDex.blockedGroups() shouldBe ""
        mangaDex.blockedUploaders() shouldBe ""
        mangaDex.coverQuality() shouldBe ""
    }
}
