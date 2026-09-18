package tachiyomi.core.common

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

internal class ConstantsTest {
    @Test
    fun helpUrlsPointAtMihonDocs() {
        Constants.URL_HELP shouldStartWith "https://mihon.app/docs/"
        Constants.URL_HELP_UPCOMING shouldStartWith "https://mihon.app/docs/"
    }

    @Test
    fun intentActionsAreNamespaced() {
        Constants.MANGA_EXTRA shouldBe "manga"
        Constants.MAIN_ACTIVITY shouldBe "eu.kanade.tachiyomi.ui.main.MainActivity"
        listOf(
            Constants.SHORTCUT_LIBRARY,
            Constants.SHORTCUT_MANGA,
            Constants.SHORTCUT_UPDATES,
            Constants.SHORTCUT_HISTORY,
            Constants.SHORTCUT_SOURCES,
            Constants.SHORTCUT_EXTENSIONS,
            Constants.SHORTCUT_DOWNLOADS,
        ).forEach { it shouldStartWith "eu.kanade.tachiyomi." }
    }
}
