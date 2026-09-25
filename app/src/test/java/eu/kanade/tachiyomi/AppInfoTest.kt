package eu.kanade.tachiyomi

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class AppInfoTest {
    @Test
    fun reportsTheBuild() {
        AppInfo.getVersionCode() shouldBe BuildConfig.VERSION_CODE
        AppInfo.getVersionName() shouldBe BuildConfig.VERSION_NAME
        AppInfo.getSupportedImageMimeTypes() shouldContain "image/jpeg"
    }
}
