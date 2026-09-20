package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.tachiyomi.util.system.isTabletUi

@Composable
@ReadOnlyComposable
internal fun isTabletUi(): Boolean = LocalConfiguration.current.isTabletUi()
