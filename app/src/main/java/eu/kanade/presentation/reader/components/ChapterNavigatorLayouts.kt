package eu.kanade.presentation.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

private const val QUARTER_TURN_DEGREES = 90f

// Slider state plus the colours/padding derived once in ChapterNavigator.
internal data class NavigatorStyle(
    val state: SliderState,
    val interactionSource: MutableInteractionSource,
    val mainAxisPadding: Dp,
    val backgroundColor: Color,
    val buttonColor: IconButtonColors,
)

// Enabled state, callback and accessibility label of one chapter-skip button.
internal data class ChapterButtonSpec(val enabled: Boolean, val onClick: () -> Unit, val description: StringResource)

internal val ChapterNavigation.previousButton: ChapterButtonSpec
    get() = ChapterButtonSpec(enabledPrevious, onPreviousChapter, MR.strings.action_previous_chapter)

internal val ChapterNavigation.nextButton: ChapterButtonSpec
    get() = ChapterButtonSpec(enabledNext, onNextChapter, MR.strings.action_next_chapter)

@Composable
internal fun HorizontalChapterNavigator(
    isRtl: Boolean,
    navigation: ChapterNavigation,
    style: NavigatorStyle,
    // SY -->
    currentPageText: String,
    // SY <--
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    // In RTL the left button is "next"; both buttons swap roles together.
    val (leftButton, rightButton) = if (isRtl) {
        navigation.nextButton to navigation.previousButton
    } else {
        navigation.previousButton to navigation.nextButton
    }

    // We explicitly handle direction based on the reader viewer rather than the system direction
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = style.mainAxisPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChapterButton(
                spec = leftButton,
                colors = style.buttonColor,
                icon = Icons.Outlined.SkipPrevious,
            )

            if (totalPages > 1) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(style.backgroundColor)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // SY -->
                        Text(text = currentPageText)
                        // SY <--

                        Slider(
                            state = style.state,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            onValueChange = { navigation.onPageIndexChange(it.roundToInt() - 1) },
                            onValueChangeFinished = navigation.onPageIndexChangeFinished,
                            interactionSource = style.interactionSource,
                        )

                        Text(text = totalPages.toString())
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }

            ChapterButton(
                spec = rightButton,
                colors = style.buttonColor,
                icon = Icons.Outlined.SkipNext,
            )
        }
    }
}

@Composable
internal fun VerticalChapterNavigator(
    navigation: ChapterNavigation,
    style: NavigatorStyle,
    // SY -->
    currentPageText: String,
    // SY <--
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = style.mainAxisPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ChapterButton(
            spec = navigation.previousButton,
            colors = style.buttonColor,
            icon = Icons.Outlined.SkipPrevious,
            modifier = Modifier.rotate(QUARTER_TURN_DEGREES),
        )

        if (totalPages > 1) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(style.backgroundColor)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // SY -->
                Text(text = currentPageText)
                // SY <--

                VerticalSlider(
                    state = style.state,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    onValueChange = { navigation.onPageIndexChange(it.roundToInt() - 1) },
                    onValueChangeFinished = navigation.onPageIndexChangeFinished,
                    interactionSource = style.interactionSource,
                )

                Text(text = totalPages.toString())
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        ChapterButton(
            spec = navigation.nextButton,
            colors = style.buttonColor,
            icon = Icons.Outlined.SkipNext,
            modifier = Modifier.rotate(QUARTER_TURN_DEGREES),
        )
    }
}

@Composable
internal fun ChapterButton(
    spec: ChapterButtonSpec,
    colors: IconButtonColors,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        enabled = spec.enabled,
        onClick = spec.onClick,
        colors = colors,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(spec.description),
            modifier = modifier,
        )
    }
}
