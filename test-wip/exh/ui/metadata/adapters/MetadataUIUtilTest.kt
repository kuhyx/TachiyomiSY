package exh.ui.metadata.adapters

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.R
import exh.ui.metadata.adapters.MetadataUIUtil.bindDrawable
import exh.ui.metadata.adapters.MetadataUIUtil.getResourceColor
import exh.util.SourceTagsUtil.GenreColor
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MetadataUIUtilTest {
    private val context: Context = ApplicationProvider.getApplicationContext<Context>().apply {
        setTheme(R.style.Theme_Tachiyomi)
    }

    @Test
    fun ratingLabels() {
        MetadataUIUtil.getRatingString(context) shouldBe "No rating"
        MetadataUIUtil.getRatingString(context, rating = null) shouldBe "No rating"
        MetadataUIUtil.getRatingString(context, rating = 0.2F) shouldBe "Disaster"
        MetadataUIUtil.getRatingString(context, rating = 9.6F) shouldBe "Masterpiece"
        MetadataUIUtil.getRatingString(context, rating = 11F) shouldBe "No rating"
    }

    @Test
    fun knownGenresHaveColours() {
        MetadataUIUtil.getGenreAndColour(context, "artist-cg") shouldBe
            (GenreColor.ARTIST_CG_COLOR.color to "Artist CG")
        MetadataUIUtil.getGenreAndColour(context, "Video")?.first shouldBe GenreColor.WESTERN_COLOR.color
        MetadataUIUtil.getGenreAndColour(context, "nope").shouldBeNull()
    }

    @Test
    fun resourceColorWithAlpha() {
        val opaque = context.getResourceColor(android.R.attr.colorForeground)
        val faded = context.getResourceColor(android.R.attr.colorForeground, alphaFactor = 0.5F)
        Color.alpha(faded) shouldBe Math.round(Color.alpha(opaque) * 0.5F)
        Color.red(faded) shouldBe Color.red(opaque)
    }

    @Test
    fun drawableIsBound() {
        val view = TextView(context)
        view.bindDrawable(context, R.drawable.ic_info_24dp)
        view.compoundDrawables[0].shouldNotBeNull()
    }

    @Test
    fun genreBinding() {
        val view = TextView(context)
        view.bindGenre(context, "misc")
        view.text.toString() shouldBe "Misc"
        (view.background as ColorDrawable).color shouldBe GenreColor.MISC_COLOR.color
        view.bindGenre(context, "odd")
        view.text.toString() shouldBe "odd"
        view.bindGenre(context, null)
        view.text.toString() shouldBe "Unknown"
    }

    @Test
    fun ratingTextRounds() {
        ratingText(context, rating = 1.234F, outOfTen = 2F) shouldBe "1.23 - Painful"
        ratingText(context, rating = null, outOfTen = null) shouldBe "0.0 - No rating"
    }

    @Test
    fun longPressCopies() {
        val view = TextView(context).apply { text = "copied" }
        copyTextOnLongClick(context, view)
        view.performLongClick() shouldBe true
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.primaryClip?.getItemAt(0)?.text.toString() shouldBe "copied"
    }
}
