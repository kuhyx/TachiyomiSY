package eu.kanade.presentation.more.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.widget.EditTextPreferenceWidget
import eu.kanade.presentation.more.settings.widget.InfoWidget
import eu.kanade.presentation.more.settings.widget.ListPreferenceWidget
import eu.kanade.presentation.more.settings.widget.MultiSelectListPrefWidget
import eu.kanade.presentation.more.settings.widget.PrefsHorizontalPadding
import eu.kanade.presentation.more.settings.widget.PrefsVerticalPadding
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TitleFontSize
import eu.kanade.presentation.more.settings.widget.TrackingPreferenceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tachiyomi.presentation.core.components.BaseSliderItem
import tachiyomi.presentation.core.util.collectAsState

internal val LocalPreferenceHighlighted = compositionLocalOf(structuralEqualityPolicy()) { false }
internal val LocalPreferenceMinHeight = compositionLocalOf(structuralEqualityPolicy()) { 56.dp }

@Composable
internal fun StatusWrapper(
    item: Preference.PreferenceItem<*, *>,
    highlightKey: String?,
    content: @Composable () -> Unit,
) {
    val enabled = item.enabled
    val highlighted = item.title == highlightKey
    AnimatedVisibility(
        visible = enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        content = {
            CompositionLocalProvider(
                LocalPreferenceHighlighted provides highlighted,
                content = content,
            )
        },
    )
}

@Composable
internal fun PreferenceItem(
    item: Preference.PreferenceItem<*, *>,
    highlightKey: String?,
) {
    val scope = rememberCoroutineScope()
    StatusWrapper(
        item = item,
        highlightKey = highlightKey,
    ) {
        when (item) {
            is Preference.PreferenceItem.SwitchPreference -> {
                SwitchItem(item, scope)
            }
            is Preference.PreferenceItem.SliderPreference -> {
                SliderItem(item, scope)
            }
            is Preference.PreferenceItem.ListPreference<*> -> {
                ListItem(item, scope)
            }
            is Preference.PreferenceItem.BasicListPreference -> {
                BasicListItem(item, scope)
            }
            is Preference.PreferenceItem.MultiSelectListPreference<*> -> {
                MultiSelectItem(item, scope)
            }
            is Preference.PreferenceItem.TextPreference -> {
                TextPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    content = item.widget,
                    onPreferenceClick = item.onClick,
                )
            }
            is Preference.PreferenceItem.EditTextPreference -> {
                EditTextItem(item)
            }
            is Preference.PreferenceItem.TrackerPreference -> {
                TrackerItem(item)
            }
            is Preference.PreferenceItem.InfoPreference -> {
                InfoWidget(text = item.title)
            }
            is Preference.PreferenceItem.CustomPreference -> {
                item.content()
            }
        }
    }
}

@Composable
private fun BasicListItem(item: Preference.PreferenceItem.BasicListPreference, scope: CoroutineScope) {
    ListPreferenceWidget(
        value = item.value,
        title = item.title,
        subtitle = item.subtitleProvider(item.value, item.entries),
        icon = item.icon,
        entries = item.entries,
        onValueChange = { scope.launch { item.onValueChanged(it) } },
    )
}

@Composable
private fun TrackerItem(item: Preference.PreferenceItem.TrackerPreference) {
    val isLoggedIn by item.tracker.let { tracker ->
        tracker.isLoggedInFlow.collectAsState(tracker.isLoggedIn)
    }
    TrackingPreferenceWidget(
        tracker = item.tracker,
        isLoggedIn = isLoggedIn,
        onClick = { if (isLoggedIn) item.logout() else item.login() },
    )
}

// The preference-backed items: the widget shows the stored value and a change is vetoable by the item.

@Composable
private fun SwitchItem(item: Preference.PreferenceItem.SwitchPreference, scope: CoroutineScope) {
    val value by item.preference.collectAsState()
    SwitchPreferenceWidget(
        title = item.title,
        subtitle = item.subtitle,
        icon = item.icon,
        checked = value,
        onCheckedChanged = { newValue ->
            scope.launch {
                if (item.onValueChanged(newValue)) {
                    item.preference.set(newValue)
                }
            }
        },
    )
}

@Composable
private fun SliderItem(item: Preference.PreferenceItem.SliderPreference, scope: CoroutineScope) {
    BaseSliderItem(
        value = item.value,
        valueRange = item.valueRange,
        steps = item.steps,
        title = item.title,
        subtitle = item.subtitle,
        valueString = item.valueString.takeUnless { it.isNullOrEmpty() } ?: item.value.toString(),
        onChange = {
            scope.launch {
                item.onValueChanged(it)
            }
        },
        titleStyle = MaterialTheme.typography.titleLarge.copy(fontSize = TitleFontSize),
        modifier = Modifier.padding(
            horizontal = PrefsHorizontalPadding,
            vertical = PrefsVerticalPadding,
        ),
    )
}

@Composable
private fun ListItem(item: Preference.PreferenceItem.ListPreference<*>, scope: CoroutineScope) {
    val value by item.preference.collectAsState()
    ListPreferenceWidget(
        value = value,
        title = item.title,
        subtitle = item.internalSubtitleProvider(value, item.entries),
        icon = item.icon,
        entries = item.entries,
        onValueChange = { newValue ->
            scope.launch {
                if (item.internalOnValueChanged(newValue!!)) {
                    item.internalSet(newValue)
                }
            }
        },
    )
}

@Composable
private fun MultiSelectItem(item: Preference.PreferenceItem.MultiSelectListPreference<*>, scope: CoroutineScope) {
    val values by item.preference.collectAsState()
    MultiSelectListPrefWidget(
        values = values,
        title = item.title,
        subtitle = item.internalSubtitleProvider(values, item.entries),
        icon = item.icon,
        entries = item.entries,
        onValuesChange = { newValues ->
            scope.launch {
                if (item.internalOnValueChanged(newValues)) {
                    item.internalSet(newValues)
                }
            }
        },
    )
}

@Composable
private fun EditTextItem(item: Preference.PreferenceItem.EditTextPreference) {
    val values by item.preference.collectAsState()
    EditTextPreferenceWidget(
        title = item.title,
        subtitle = item.subtitle,
        icon = item.icon,
        value = values,
        onConfirm = {
            val accepted = item.onValueChanged(it)
            if (accepted) item.preference.set(it)
            accepted
        },
    )
}
