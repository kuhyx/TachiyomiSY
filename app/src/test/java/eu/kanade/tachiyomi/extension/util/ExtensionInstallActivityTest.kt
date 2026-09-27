package eu.kanade.tachiyomi.extension.util

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.InstallStep
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowToast
import java.lang.reflect.InvocationTargetException

@RunWith(RobolectricTestRunner::class)
internal class ExtensionInstallActivityTest {

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val installer = mockk<ExtensionInstaller>(relaxed = true)
    private val manager = mockk<ExtensionManager>(relaxed = true)
    private val apk = Uri.parse("content://apk/9")

    @Before
    fun setUp() {
        every { manager.installer } returns installer
        startKoin { modules(module { single { manager } }) }
    }

    @After
    fun tearDown() = stopKoin()

    private fun launch(data: Uri? = apk, withId: Boolean = true): ActivityController<ExtensionInstallActivity> {
        val intent = Intent(app, ExtensionInstallActivity::class.java).setDataAndType(data, ExtensionInstaller.APK_MIME)
        if (withId) intent.putExtra(ExtensionInstaller.EXTRA_DOWNLOAD_ID, ID)
        return Robolectric.buildActivity(ExtensionInstallActivity::class.java, intent).create()
    }

    // onActivityResult is protected; Robolectric's own caller for it is deprecated.
    private fun ActivityController<ExtensionInstallActivity>.answer(code: Int, request: Int = REQUEST) {
        val method = ExtensionInstallActivity::class.java.getDeclaredMethod(
            "onActivityResult",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Intent::class.java,
        )
        method.isAccessible = true
        try {
            method.invoke(get(), request, code, null)
        } catch (wrapped: InvocationTargetException) {
            throw wrapped.targetException
        }
    }

    @Test
    fun startsThePlatformInstaller() {
        val started = shadowOf(launch().get()).nextStartedActivityForResult
        started.intent.action shouldBe "android.intent.action.INSTALL_PACKAGE"
        started.intent.data shouldBe apk
        started.requestCode shouldBe REQUEST
    }

    @Test
    fun missingInstallerToasts() {
        shadowOf(app).checkActivities(true)
        launch()
        ShadowToast.shownToastCount() shouldBe 1
    }

    @Test
    fun resultsMapToSteps() {
        launch().answer(Activity.RESULT_OK)
        launch().answer(Activity.RESULT_CANCELED)
        launch().answer(Activity.RESULT_FIRST_USER)
        verify { installer.updateInstallStep(ID, InstallStep.Installed) }
        verify { installer.updateInstallStep(ID, InstallStep.Idle) }
        verify { installer.updateInstallStep(ID, InstallStep.Error) }
    }

    @Test
    fun otherRequestsOnlyFinish() {
        val controller = launch()
        controller.answer(Activity.RESULT_OK, request = 1)
        controller.get().isFinishing shouldBe true
        verify(exactly = 0) { installer.updateInstallStep(any(), any()) }
    }

    @Test
    fun missingExtrasCannotReport() {
        shouldThrow<NullPointerException> { launch(withId = false).answer(Activity.RESULT_OK) }
    }

    @Test
    fun miuiResultIsDeferred() {
        shadowOf(app.packageManager).installPackage(PackageInfo().apply { packageName = MIUI })
        val controller = launch()
        controller.answer(Activity.RESULT_OK)
        verify(exactly = 0) { installer.updateInstallStep(any(), any()) }
        controller.start()
        verify { installer.updateInstallStep(ID, InstallStep.Idle) }
    }

    @Test
    fun lateMiuiResultCounts() {
        shadowOf(app.packageManager).installPackage(PackageInfo().apply { packageName = MIUI })
        val controller = launch()
        Thread.sleep(MIUI_WINDOW_MS)
        controller.answer(Activity.RESULT_OK)
        controller.start()
        verify(exactly = 1) { installer.updateInstallStep(ID, InstallStep.Installed) }
    }

    @Test
    fun destroyDropsTheApk() {
        launch().start().destroy()
        launch(data = null).destroy()
    }

    private companion object {
        const val ID = 42L
        const val REQUEST = 500
        const val MIUI = "com.miui.packageinstaller"
        const val MIUI_WINDOW_MS = 1_100L
    }
}
