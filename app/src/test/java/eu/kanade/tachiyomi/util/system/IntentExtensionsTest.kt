package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IntentExtensionsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun Intent.inner(): Intent = getParcelableExtraCompat<Intent>(Intent.EXTRA_INTENT)!!

    @Test
    fun httpUrisAreSharedAsText() {
        val chooser = "https://example.test".toUri().toShareIntent(context)
        chooser.action shouldBe Intent.ACTION_CHOOSER
        chooser.flags shouldBe Intent.FLAG_ACTIVITY_NEW_TASK
        val shared = chooser.inner()
        shared.getStringExtra(Intent.EXTRA_TEXT) shouldBe "https://example.test"
        shared.type shouldBe "image/*"
        shared.flags shouldBe Intent.FLAG_GRANT_READ_URI_PERMISSION
        "http://example.test".toUri().toShareIntent(context).inner()
            .getStringExtra(Intent.EXTRA_TEXT) shouldBe "http://example.test"
    }

    @Test
    fun contentUrisAreSharedAsStreams() {
        val uri = "content://provider/1".toUri()
        val shared = uri.toShareIntent(context, type = "text/plain", message = "note").inner()
        shared.getStringExtra(Intent.EXTRA_TEXT) shouldBe "note"
        shared.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM) shouldBe uri
        shared.type shouldBe "text/plain"
        val quiet = uri.toShareIntent(context).inner()
        quiet.getStringExtra(Intent.EXTRA_TEXT).shouldBeNull()
    }

    @Test
    fun anUnknownSchemeCarriesOnlyThe() {
        val shared = "file:///tmp/x".toUri().toShareIntent(context).inner()
        shared.getStringExtra(Intent.EXTRA_TEXT).shouldBeNull()
        shared.clipData!!.getItemAt(0).uri.toString() shouldBe "file:///tmp/x"
    }

    @Test
    fun serializableExtrasComeBack() {
        val intent = Intent().putExtra("bundle", Bundle().apply { putString("k", "v") })
        intent.getParcelableExtraCompat<Bundle>("bundle")!!.getString("k") shouldBe "v"
        val serialized = Intent().putExtra("text", "value")
        serialized.getSerializableExtraCompat<String>("text") shouldBe "value"
        serialized.getSerializableExtraCompat<String>("missing").shouldBeNull()
    }
}
