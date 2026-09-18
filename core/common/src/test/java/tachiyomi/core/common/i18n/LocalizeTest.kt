package tachiyomi.core.common.i18n

import android.content.Context
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class LocalizeTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun plainString() {
        context.stringResource(MR.strings.pref_update_only_non_completed) shouldBe
            "Skip entries with \"Completed\" status"
    }

    @Test
    fun formattedString() {
        context.stringResource(MR.strings.pref_relative_format_summary, "a", "b") shouldBe """"a" instead of "b""""
    }

    @Test
    fun plural() {
        context.pluralStringResource(MR.plurals.relative_time, 1) shouldBe "Yesterday"
    }

    @Test
    fun formattedPlural() {
        context.pluralStringResource(MR.plurals.relative_time, 3, 3) shouldBe "3 days ago"
        context.pluralStringResource(MR.plurals.num_categories, 2, 2) shouldContain "2 categories"
    }
}
