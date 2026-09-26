package eu.kanade.tachiyomi

import android.os.Build
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.notification.Notifications
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import mihon.core.firebase.FirebaseConfig
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.security.Security

@RunWith(RobolectricTestRunner::class)
internal class AppTest {
    private val boot = AppBoot()
    private val sdk = Build.VERSION.SDK_INT

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        Security.removeProvider("Conscrypt")
        boot.close()
    }

    @Test
    fun createBootsTheGraph() {
        val app = boot.create()
        Injekt.get<BasePreferences>().context shouldBe app
        boot.migrationsFinished shouldBe true
        val lastVersion = Injekt.get<PreferenceStore>().getInt(Preference.appStateKey("eh_last_version_code"), 0)
        lastVersion.get() shouldBe BuildConfig.VERSION_CODE
        boot.workManagerReady shouldBe true
    }

    @Test
    fun readyWorkManagerIsKept() {
        boot.workManagerReady = true
        boot.create()
        boot.migrationsFinished shouldBe true
    }

    @Test
    fun failuresAreSurvived() {
        mockkObject(FirebaseConfig, Notifications)
        every { FirebaseConfig.init(any()) } throws IllegalStateException("no firebase")
        every { Notifications.createChannels(any()) } throws IllegalStateException("no channels")
        boot.create()
        boot.migrationsFinished shouldBe true
    }

    @Test
    fun oldAndroidGetsConscrypt() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.O_MR1)
        boot.create()
        Security.getProviders()[0].name shouldBe "Conscrypt"
    }

    @Test
    fun secondProcessGetsSuffix() {
        boot.processName = "eu.kanade.tachiyomi:other"
        boot.create().shouldNotBeNull()
    }
}
