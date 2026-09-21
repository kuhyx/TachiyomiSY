package eu.kanade.presentation.reader.appbars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

// Proportions of the utility rows: label vs. control, and how much of the bar each row fills.
private const val ROW_WIDTH_FRACTION = 0.9f
private const val HALF_ROW_FRACTION = 0.5f
private const val BUTTON_WIDTH_FRACTION = 0.75f
private const val LABEL_WEIGHT = 3f

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
private fun AutoScrollToggle(controls: AutoScrollControls) {
    Row(
        Modifier
            .fillMaxWidth(HALF_ROW_FRACTION)
            .fillMaxHeight()
            .padding(5.dp)
            .clickable(enabled = controls.isAutoScrollEnabled) { controls.onToggleAutoscroll(!controls.isAutoScroll) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(LABEL_WEIGHT),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(SYMR.strings.eh_autoscroll),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.fillMaxWidth(BUTTON_WIDTH_FRACTION),
                textAlign = TextAlign.Center,
            )
        }
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Switch(
                checked = controls.isAutoScroll,
                onCheckedChange = null,
                enabled = controls.isAutoScrollEnabled,
            )
        }
    }
}

@Composable
private fun AutoScrollFrequencyField(controls: AutoScrollControls) {
    Row(
        Modifier.fillMaxWidth(ROW_WIDTH_FRACTION).padding(5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(LABEL_WEIGHT),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var autoScrollFrequencyState by remember {
                mutableStateOf(controls.autoScrollFrequency)
            }
            TextField(
                value = autoScrollFrequencyState,
                onValueChange = {
                    autoScrollFrequencyState = it
                    controls.onSetAutoScrollFrequency(it)
                },
                isError = !controls.isAutoScrollEnabled,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(BUTTON_WIDTH_FRACTION),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                ),
            )
            AnimatedVisibility(!controls.isAutoScrollEnabled) {
                Text(
                    text = stringResource(SYMR.strings.eh_autoscroll_freq_invalid),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        HelpButton(onClick = controls.onClickHelp, modifier = Modifier.weight(1f))
    }
}

// A labelled action button followed by its "?" help button, sharing one padded row.
@Composable
private fun ActionWithHelp(
    label: String,
    onClick: () -> Unit,
    onClickHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.padding(5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.weight(LABEL_WEIGHT),
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
            )
        }
        HelpButton(onClick = onClickHelp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            text = "?",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
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
