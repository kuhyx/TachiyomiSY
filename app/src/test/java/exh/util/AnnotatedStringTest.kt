package exh.util

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AnnotatedStringTest {
    @Test
    fun keepsSupportedSpans() {
        val spanned = SpannableString("0123456789").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.ITALIC), 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD_ITALIC), 2, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.NORMAL), 3, 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(UnderlineSpan(), 4, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(0xFF112233.toInt()), 5, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(RelativeSizeSpan(2f), 6, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val annotated = spanned.toAnnotatedString()
        annotated.text shouldBe "0123456789"
        val styles = annotated.spanStyles.sortedBy { it.start }
        styles.map { it.start to it.end } shouldContainExactly listOf(0 to 1, 1 to 2, 2 to 3, 4 to 5, 5 to 6)
        styles[0].item.fontWeight shouldBe FontWeight.Bold
        styles[1].item.fontStyle shouldBe FontStyle.Italic
        styles[2].item.fontWeight shouldBe FontWeight.Bold
        styles[2].item.fontStyle shouldBe FontStyle.Italic
        styles[3].item.textDecoration shouldBe TextDecoration.Underline
        styles[4].item.color shouldBe Color(0xFF112233)
    }

    @Test
    fun plainTextHasNoStyles() {
        SpannableString("plain").toAnnotatedString().spanStyles.size shouldBe 0
    }
}
