package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A small top inset and nothing else. */
public val topSmallPaddingValues: PaddingValues = PaddingValues(top = MaterialTheme.padding.small)

/** The opacity of disabled content. */
public const val DISABLED_ALPHA: Float = .38f

/** The opacity of secondary content. */
public const val SECONDARY_ALPHA: Float = .78f

/** The spacing scale every screen uses. */
public class Padding {

    /** 32 dp. */
    public val extraLarge: Dp = 32.dp

    /** 24 dp. */
    public val large: Dp = 24.dp

    /** 16 dp. */
    public val medium: Dp = 16.dp

    /** 8 dp. */
    public val small: Dp = 8.dp

    /** 4 dp. */
    public val extraSmall: Dp = 4.dp
}

/** The spacing scale, reachable from the theme like its other tokens. */
public val MaterialTheme.padding: Padding
    get() = Padding()
