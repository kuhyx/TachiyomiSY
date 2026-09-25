package eu.kanade.presentation.reader.appbars

import eu.kanade.tachiyomi.ui.reader.setting.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode

/** Callbacks that record their name in [events]; `withLinks` decides whether the nullable links exist. */
internal class ReaderBarCallbacks(private val withLinks: Boolean = true) {
    val events: MutableList<String> = mutableListOf()

    fun settings(crop: Boolean = true) = ReaderSettingButtons(
        readingMode = ReadingMode.LEFT_TO_RIGHT,
        onClickReadingMode = { events += "mode" },
        orientation = ReaderOrientation.FREE,
        onClickOrientation = { events += "orientation" },
        cropEnabled = crop,
        onClickCropBorder = { events += "crop" },
    )

    fun actions() = SyBottomBarActions(
        onClickChapterList = { events += "chapters" },
        onClickWebView = { events += "webview" }.takeIf { withLinks },
        onClickBrowser = { events += "browser" }.takeIf { withLinks },
        onClickShare = { events += "share" }.takeIf { withLinks },
        onClickPageLayout = { events += "layout" },
        onClickShiftPage = { events += "shift" },
    )

    fun autoScroll(enabled: Boolean = true) = AutoScrollControls(
        isAutoScroll = false,
        isAutoScrollEnabled = enabled,
        onToggleAutoscroll = { events += "autoscroll $it" },
        autoScrollFrequency = "3.0",
        onSetAutoScrollFrequency = { events += "frequency $it" },
        onClickHelp = { events += "help" },
    )

    fun pageActions() = ExhPageActions(
        onClickRetryAll = { events += "retry" },
        onClickRetryAllHelp = { events += "retry help" },
        onClickBoostPage = { events += "boost" },
        onClickBoostPageHelp = { events += "boost help" },
    )
}

internal val allButtons: Set<String> = ReaderBottomButton.entries.map { it.value }.toSet()

internal fun syState(
    mode: ReadingMode = ReadingMode.LEFT_TO_RIGHT,
    buttons: Set<String> = allButtons,
    split: Boolean = false,
    doublePages: Boolean = true,
) = SyBottomBarState(
    enabledButtons = buttons,
    currentReadingMode = mode,
    dualPageSplitEnabled = split,
    doublePages = doublePages,
)
