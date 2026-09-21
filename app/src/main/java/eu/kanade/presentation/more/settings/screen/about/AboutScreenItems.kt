package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.ui.more.NewUpdateScreen
import kotlinx.coroutines.launch
import tachiyomi.domain.release.model.getDownloadLink
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LinkIcon
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.icons.CustomIcons
import uy.kohesive.injekt.api.get

@Composable
internal fun CheckForUpdatesItem() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    var isCheckingUpdates by remember { mutableStateOf(false) }
    TextPreferenceWidget(
        title = stringResource(MR.strings.check_for_updates),
        content = {
            AnimatedVisibility(visible = isCheckingUpdates) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                )
            }
        },
        onPreferenceClick = {
            if (!isCheckingUpdates) {
                scope.launch {
                    isCheckingUpdates = true
                    AboutScreen.checkVersion(
                        context = context,
                        onAvailableUpdate = { result ->
                            val updateScreen = NewUpdateScreen(
                                versionName = result.release.version,
                                changelogInfo = result.release.info,
                                releaseLink = result.release.releaseLink,
                                downloadLink = result.release.getDownloadLink(),
                            )
                            navigator.push(updateScreen)
                        },
                        onFinish = { isCheckingUpdates = false },
                    )
                }
            }
        },
    )
}

@Composable
internal fun SocialLinks() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        LinkIcon(
            label =
            stringResource(MR.strings.website),
            icon = Icons.Outlined.Public, url = "https://mihon.app",
        )
        LinkIcon(label = "Discord", icon = CustomIcons.Discord, url = "https://discord.gg/mihon")
        LinkIcon(label = "X", icon = CustomIcons.X, url = "https://x.com/mihonapp")
        LinkIcon(label = "Facebook", icon = CustomIcons.Facebook, url = "https://facebook.com/mihonapp")
        LinkIcon(label = "Reddit", icon = CustomIcons.Reddit, url = "https://www.reddit.com/r/mihonapp")
        LinkIcon(
            label = "GitHub",
            icon = CustomIcons.Github,
            // SY -->
            url = "https://github.com/jobobby04/tachiyomisy",
            // SY <--
        )
    }
}

internal fun LazyListScope.linkItems(links: List<Pair<StringResource, () -> Unit>>) {
    links.forEach { (title, onClick) ->
        item {
            TextPreferenceWidget(
                title = stringResource(title),
                onPreferenceClick = onClick,
            )
        }
    }
}
