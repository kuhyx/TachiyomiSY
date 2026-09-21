package eu.kanade.presentation.reader.appbars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewLightDark
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

// Proportions of the utility rows: label vs. control, and how much of the bar each row fills.
internal const val ROW_WIDTH_FRACTION = 0.9f
internal const val HALF_ROW_FRACTION = 0.5f

// The E-Hentai auto-scroll toggle, its frequency field and the help button next to it.
internal data class AutoScrollControls(
    val isAutoScroll: Boolean,
    val isAutoScrollEnabled: Boolean,
    val onToggleAutoscroll: (Boolean) -> Unit,
    val autoScrollFrequency: String,
    val onSetAutoScrollFrequency: (String) -> Unit,
    val onClickHelp: () -> Unit,
)

// The E-Hentai "retry all" / "boost page" actions with their help buttons.
internal data class ExhPageActions(
    val onClickRetryAll: () -> Unit,
    val onClickRetryAllHelp: () -> Unit,
    val onClickBoostPage: () -> Unit,
    val onClickBoostPageHelp: () -> Unit,
)

@Composable
internal fun ExhUtils(
    isVisible: Boolean,
    onSetExhUtilsVisibility: (Boolean) -> Unit,
    backgroundColor: Color,
    autoScroll: AutoScrollControls,
    pageActions: ExhPageActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(backgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(visible = isVisible) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth(ROW_WIDTH_FRACTION)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AutoScrollToggle(autoScroll)
                    AutoScrollFrequencyField(autoScroll)
                }
                Row(
                    Modifier.fillMaxWidth(ROW_WIDTH_FRACTION),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionWithHelp(
                        label = stringResource(SYMR.strings.eh_retry_all),
                        onClick = pageActions.onClickRetryAll,
                        onClickHelp = pageActions.onClickRetryAllHelp,
                        modifier = Modifier.fillMaxWidth(HALF_ROW_FRACTION),
                    )
                    ActionWithHelp(
                        label = stringResource(SYMR.strings.eh_boost_page),
                        onClick = pageActions.onClickBoostPage,
                        onClickHelp = pageActions.onClickBoostPageHelp,
                        modifier = Modifier.fillMaxWidth(ROW_WIDTH_FRACTION),
                    )
                }
            }
        }

        IconButton(
            onClick = { onSetExhUtilsVisibility(!isVisible) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = if (isVisible) {
                    Icons.Outlined.KeyboardArrowUp
                } else {
                    Icons.Outlined.KeyboardArrowDown
                },
                contentDescription = null,
            )
        }
    }
}

@Composable
@PreviewLightDark
internal fun ExhUtilsPreview() {
    Surface {
        ExhUtils(
            isVisible = true,
            onSetExhUtilsVisibility = {},
            backgroundColor = Color.Black,
            autoScroll = AutoScrollControls(
                isAutoScroll = true,
                isAutoScrollEnabled = true,
                onToggleAutoscroll = {},
                autoScrollFrequency = "3.0",
                onSetAutoScrollFrequency = {},
                onClickHelp = {},
            ),
            pageActions = ExhPageActions(
                onClickRetryAll = {},
                onClickRetryAllHelp = {},
                onClickBoostPage = {},
                onClickBoostPageHelp = {},
            ),
        )
    }
}
