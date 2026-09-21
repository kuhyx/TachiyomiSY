package eu.kanade.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.runOnEnterKeyPressed
import tachiyomi.presentation.core.util.secondaryItemAlpha
import tachiyomi.presentation.core.util.showSoftKeyboard

/**
 * Toolbar that switches into a search field. A null [searchQuery] shows the normal toolbar;
 * [searchEnabled] false hides the search action; a null [placeholderText] falls back to
 * [MR.strings.action_search_hint].
 */
@Composable
internal fun SearchToolbar(
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    modifier: Modifier = Modifier,
    titleContent: @Composable () -> Unit = {},
    navigateUp: (() -> Unit)? = null,
    searchEnabled: Boolean = true,
    placeholderText: String? = null,
    onSearch: (String) -> Unit = {},
    onClickCloseSearch: () -> Unit = { onChangeSearchQuery(null) },
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focusRequester = remember { FocusRequester() }

    AppBar(
        modifier = modifier,
        titleContent = {
            if (searchQuery == null) {
                titleContent()
            } else {
                SearchField(
                    searchQuery,
                    onChangeSearchQuery,
                    onSearch,
                    placeholderText,
                    focusRequester,
                    interactionSource,
                )
            }
        },
        navigateUp = if (searchQuery == null) navigateUp else onClickCloseSearch,
        actions = {
            key("search") {
                if (searchEnabled) SearchActions(searchQuery, onChangeSearchQuery, focusRequester)
            }
            key("actions") { actions() }
        },
        isActionMode = false,
        scrollBehavior = scrollBehavior,
    )
}

// The search action when the field is closed; a reset action once it holds text.
@Composable
internal fun SearchActions(
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    focusRequester: FocusRequester,
) {
    val onClick = { onChangeSearchQuery("") }
    if (searchQuery == null) {
        TooltipIconButton(
            title =
            stringResource(MR.strings.action_search),
            icon = Icons.Outlined.Search, onClick = onClick,
        )
    } else if (searchQuery.isNotEmpty()) {
        TooltipIconButton(
            title = stringResource(MR.strings.action_reset),
            icon = Icons.Outlined.Close,
            onClick = {
                onClick()
                focusRequester.requestFocus()
            },
        )
    }
}

@Composable
internal fun SearchField(
    searchQuery: String,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    placeholderText: String?,
    focusRequester: FocusRequester,
    interactionSource: MutableInteractionSource,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val searchAndClearFocus: () -> Unit = {
        if (searchQuery.isNotBlank()) {
            onSearch(searchQuery)
            focusManager.clearFocus()
            keyboardController?.hide()
            focusManager.moveFocus(FocusDirection.Next)
        }
    }
    // Callers own the query as a String; the field's own state is bridged both ways
    // through SearchQueryBridge, which keeps the caller's stale echoes out of the field.
    val textFieldState = rememberTextFieldState(searchQuery)
    val bridge = remember(textFieldState) { SearchQueryBridge(searchQuery) }
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { if (bridge.fieldChanged(it)) onChangeSearchQuery(it) }
    }
    LaunchedEffect(searchQuery) {
        if (bridge.callerChanged(searchQuery)) {
            textFieldState.setTextAndPlaceCursorAtEnd(searchQuery)
        }
    }
    val textStyle = MaterialTheme.typography.titleMedium.copy(
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
    )
    BasicTextField(
        state = textFieldState,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .runOnEnterKeyPressed(action = searchAndClearFocus)
            .showSoftKeyboard(remember { searchQuery.isEmpty() })
            .clearFocusOnSoftKeyboardHide(),
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { searchAndClearFocus() },
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        interactionSource = interactionSource,
        decorator = TextFieldDefaults.decorator(
            state = textFieldState,
            enabled = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            outputTransformation = null,
            interactionSource = interactionSource,
            placeholder = { SearchPlaceholder(placeholderText) },
            container = {},
        ),
    )
}

@Composable
internal fun SearchPlaceholder(placeholderText: String?) {
    Text(
        modifier = Modifier.secondaryItemAlpha(),
        text = placeholderText ?: stringResource(MR.strings.action_search_hint),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
        ),
    )
}
