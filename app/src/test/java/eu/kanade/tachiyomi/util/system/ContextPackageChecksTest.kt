package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.hippo.unifile.UniFile
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import rikka.sui.Sui

/** The corners of the context helpers that need a stubbed [UniFile] or Shizuku service. */
@RunWith(RobolectricTestRunner::class)
internal class ContextPackageChecksTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun anUnknownSizeIsNull() {
        mockkStatic(UniFile::class)
        val file = mockk<UniFile>()
        every { UniFile.fromUri(any(), any()) } returns file
        every { file.length() } returns -1
        context.getUriSize("content://any/1".toUri()).shouldBeNull()
        every { file.length() } returns 7
        context.getUriSize("content://any/1".toUri()) shouldBe 7L
    }

    @Test
    fun suiCountsAsShizuku() {
        mockkStatic(Sui::class)
        every { Sui.isSui() } returns true
        context.isShizukuInstalled shouldBe true
    }
}
