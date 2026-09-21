package eu.kanade.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SuggestionChipDefaults as SuggestionChipDefaultsM3

@ExperimentalMaterial3Api
@Composable
internal fun SuggestionChip(
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ChipElevation? = SuggestionChipDefaults.suggestionChipElevation(),
    shape: Shape = MaterialTheme.shapes.small,
    border: ChipBorder? = SuggestionChipDefaults.suggestionChipBorder(),
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(),
) = Chip(
    modifier = modifier,
    label = label,
    labelTextStyle = MaterialTheme.typography.labelLarge,
    labelColor = colors.labelColor(enabled).value,
    leadingIcon = icon,
    avatar = null,
    trailingIcon = null,
    leadingIconColor = colors.leadingIconContentColor(enabled).value,
    trailingIconColor = colors.trailingIconContentColor(enabled).value,
    containerColor = colors.containerColor(enabled).value,
    tonalElevation = elevation?.tonalElevation(enabled, interactionSource)?.value ?: 0.dp,
    shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp,
    minHeight = SuggestionChipDefaultsM3.Height,
    paddingValues = SuggestionChipPadding,
    shape = shape,
    border = border?.borderStroke(enabled)?.value,
)

@ExperimentalMaterial3Api
@Composable
internal fun SuggestionChip(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ChipElevation? = SuggestionChipDefaults.elevatedChipElevation(),
    shape: Shape = MaterialTheme.shapes.small,
    border: ChipBorder? = SuggestionChipDefaults.suggestionChipBorder(),
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(),
) = Chip(
    modifier = modifier,
    onClick = onClick,
    onLongClick = onLongClick,
    enabled = enabled,
    label = label,
    labelTextStyle = MaterialTheme.typography.labelLarge,
    labelColor = colors.labelColor(enabled).value,
    leadingIcon = icon,
    avatar = null,
    trailingIcon = null,
    leadingIconColor = colors.leadingIconContentColor(enabled).value,
    trailingIconColor = colors.trailingIconContentColor(enabled).value,
    containerColor = colors.containerColor(enabled).value,
    tonalElevation = elevation?.tonalElevation(enabled, interactionSource)?.value ?: 0.dp,
    shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp,
    minHeight = SuggestionChipDefaultsM3.Height,
    paddingValues = SuggestionChipPadding,
    shape = shape,
    border = border?.borderStroke(enabled)?.value,
    interactionSource = interactionSource,
)

// The padding between the elements in the chip.
internal val HorizontalElementsPadding = 8.dp

// Returns the [PaddingValues] for the suggestion chip.
private val SuggestionChipPadding = PaddingValues(horizontal = HorizontalElementsPadding)
