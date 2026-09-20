package tachiyomi.presentation.core.screens

import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource

/** A button under an [EmptyScreen]'s message. */
public data class EmptyScreenAction(
    /** The button's label. */
    val stringRes: StringResource,
    /** The button's icon. */
    val icon: ImageVector,
    /** What the button does. */
    val onClick: () -> Unit,
)
