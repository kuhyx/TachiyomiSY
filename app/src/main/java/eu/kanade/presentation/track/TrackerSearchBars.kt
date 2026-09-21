package eu.kanade.presentation.track

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.runOnEnterKeyPressed

@Composable
internal fun TrackerSearchTopBar(
    state: TextFieldState,
    focusRequester: FocusRequester,
    onDispatchQuery: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Column {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            title = {
                BasicTextField(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .runOnEnterKeyPressed(action = onDispatchQuery),
                    textStyle = MaterialTheme.typography.bodyLarge
                        .copy(color = MaterialTheme.colorScheme.onSurface),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    onKeyboardAction = { onDispatchQuery() },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorator = {
                        if (state.text.isEmpty()) {
                            Text(
                                text = stringResource(MR.strings.action_search_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        it()
                    },
                )
            },
            actions = {
                if (state.text.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            state.clearText()
                            focusRequester.requestFocus()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
        HorizontalDivider()
    }
}

@Composable
internal fun TrackerSearchBottomBar(
    visible: Boolean,
    supportsPrivateTracking: Boolean,
    onConfirmSelection: (private: Boolean) -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = slideOutVertically { it / 2 } + fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .padding(MaterialTheme.padding.small)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            Button(
                onClick = { onConfirmSelection(false) },
                modifier = Modifier.weight(1f),
                elevation = ButtonDefaults.elevatedButtonElevation(),
            ) {
                Text(text = stringResource(MR.strings.action_track))
            }
            if (supportsPrivateTracking) {
                Button(
                    onClick = { onConfirmSelection(true) },
                    elevation = ButtonDefaults.elevatedButtonElevation(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.VisibilityOff,
                        contentDescription = stringResource(MR.strings.action_toggle_private_on),
                    )
                }
            }
        }
    }
}
