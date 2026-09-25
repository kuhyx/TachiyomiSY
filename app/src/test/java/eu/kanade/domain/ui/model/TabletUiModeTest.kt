package eu.kanade.domain.ui.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class TabletUiModeTest {

    @Test
    fun eachModeHasATitle() {
        TabletUiMode.entries.size shouldBe 4
        TabletUiMode.AUTOMATIC.titleRes shouldBe MR.strings.automatic_background
        TabletUiMode.ALWAYS.titleRes shouldBe MR.strings.lock_always
        TabletUiMode.LANDSCAPE.titleRes shouldBe MR.strings.landscape
        TabletUiMode.NEVER.titleRes shouldBe MR.strings.lock_never
        TabletUiMode.valueOf("NEVER") shouldBe TabletUiMode.NEVER
    }
}
