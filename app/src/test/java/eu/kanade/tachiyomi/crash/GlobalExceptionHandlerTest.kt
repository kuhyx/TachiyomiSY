package eu.kanade.tachiyomi.crash

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class GlobalExceptionHandlerTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private var previous: Thread.UncaughtExceptionHandler? = null

    @Before
    fun saveHandler() {
        previous = Thread.getDefaultUncaughtExceptionHandler()
    }

    @After
    fun restoreHandler() {
        Thread.setDefaultUncaughtExceptionHandler(previous)
    }

    @Test
    fun crashLaunchesTheActivity() {
        val fallback = mockk<Thread.UncaughtExceptionHandler>(relaxed = true)
        Thread.setDefaultUncaughtExceptionHandler(fallback)
        GlobalExceptionHandler.initialize(context, CrashActivity::class.java)
        val handler = Thread.getDefaultUncaughtExceptionHandler().shouldNotBeNull()
        val error = IllegalStateException("boom")

        handler.uncaughtException(Thread.currentThread(), error)

        val started = shadowOf(context).nextStartedActivity.shouldNotBeNull()
        started.component?.className shouldBe CrashActivity::class.java.name
        (started.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK) shouldBe Intent.FLAG_ACTIVITY_CLEAR_TASK
        (started.flags and Intent.FLAG_ACTIVITY_NEW_TASK) shouldBe Intent.FLAG_ACTIVITY_NEW_TASK
        (started.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP) shouldBe Intent.FLAG_ACTIVITY_CLEAR_TOP
        GlobalExceptionHandler.getThrowableFromIntent(started)?.message.shouldNotBeNull() shouldContain "boom"
        verify { fallback.uncaughtException(Thread.currentThread(), error) }
    }

    @Test
    fun requiresAPreviousHandler() {
        // Android always installs a default handler; without one the chain has nowhere to end.
        Thread.setDefaultUncaughtExceptionHandler(null)
        shouldThrow<NullPointerException> { GlobalExceptionHandler.initialize(context, CrashActivity::class.java) }
    }

    @Test
    fun missingExtraGivesNull() {
        GlobalExceptionHandler.getThrowableFromIntent(Intent()).shouldBeNull()
    }

    @Test
    fun serializerRoundTrips() {
        val encoded = Json.encodeToString(GlobalExceptionHandler.ThrowableSerializer, Throwable("why"))
        encoded shouldContain "why"
        val decoded = Json.decodeFromString(GlobalExceptionHandler.ThrowableSerializer, "\"trace\"")
        decoded.message shouldBe "trace"
        GlobalExceptionHandler.ThrowableSerializer.descriptor.serialName shouldBe "Throwable"
    }
}
