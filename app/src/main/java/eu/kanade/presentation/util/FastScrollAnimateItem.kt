package eu.kanade.presentation.util

import androidx.compose.ui.Modifier

// https://issuetracker.google.com/352584409
context(itemScope: androidx.compose.foundation.lazy.LazyItemScope)
internal fun Modifier.animateItemFastScroll() = with(itemScope) {
    this@animateItemFastScroll.animateItem(fadeInSpec = null, fadeOutSpec = null)
}
