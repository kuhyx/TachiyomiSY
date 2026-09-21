package eu.kanade.presentation.browse

import android.util.DisplayMetrics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TrailingWidgetBuffer
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.copyToClipboard
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

// Icon, name and stripped package name; a tap copies the debug summary.
private const val NSFW_LABEL_WEIGHT = 1.5f

@Composable
private fun IdentityBlock(extension: Extension, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.medium)
            .padding(
                top = MaterialTheme.padding.medium,
                bottom = MaterialTheme.padding.small,
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ExtensionIcon(
            modifier = Modifier.size(112.dp),
            extension = extension,
            density = DisplayMetrics.DENSITY_XXXHIGH,
        )
        Text(
            text = extension.name,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = extension.pkgName.substringAfter("eu.kanade.tachiyomi.extension."),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun Extension.debugInfo(): String = buildString {
    append(
        """
            Extension name: $name (lang: $lang; package: $pkgName)
            Extension version: $versionName (lib: $libVersion; version code: $versionCode)
            NSFW: $isNsfw
        """.trimIndent(),
    )
    if (this@debugInfo is Extension.Installed) {
        append("\n\n")
        appendLine(
            """
                Update available: $hasUpdate
                Orphaned: $isObsolete
                Shared: $isShared
            """.trimIndent(),
        )
        val store = store
        if (store != null) {
            append("Repository: ${store.indexUrl}")
        }
    }
}

@Composable
private fun InfoRow(extension: Extension, onClickAgeRating: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.padding.extraLarge,
                vertical = MaterialTheme.padding.small,
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InfoText(
            modifier = Modifier.weight(1f),
            primaryText = extension.versionName,
            secondaryText = stringResource(MR.strings.ext_info_version),
        )
        InfoDivider()
        InfoText(
            modifier = Modifier.weight(if (extension.isNsfw) NSFW_LABEL_WEIGHT else 1f),
            primaryText = LocaleHelper.getSourceDisplayName(extension.lang, context),
            secondaryText = stringResource(MR.strings.ext_info_language),
        )
        if (extension.isNsfw) {
            InfoDivider()
            InfoText(
                modifier = Modifier.weight(1f),
                primaryText = stringResource(MR.strings.ext_nsfw_short),
                primaryTextStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                ),
                secondaryText = stringResource(MR.strings.ext_info_age_rating),
                onClick = onClickAgeRating,
            )
        }
    }
}

@Composable
internal fun DetailsHeader(
    extension: Extension,
    extIncognitoMode: Boolean,
    onClickAgeRating: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickAppInfo: (() -> Unit)?,
    onExtIncognitoChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    Column {
        IdentityBlock(extension) { context.copyToClipboard("Extension Debug information", extension.debugInfo()) }

        InfoRow(extension, onClickAgeRating)

        Row(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .padding(top = MaterialTheme.padding.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onClickUninstall,
            ) {
                Text(stringResource(MR.strings.ext_uninstall))
            }

            if (onClickAppInfo != null) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onClickAppInfo,
                ) {
                    Text(
                        text = stringResource(MR.strings.ext_app_info),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        TextPreferenceWidget(
            modifier = Modifier.padding(horizontal = MaterialTheme.padding.small),
            title = stringResource(MR.strings.pref_incognito_mode),
            subtitle = stringResource(MR.strings.pref_incognito_mode_extension_summary),
            icon = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Switch(
                        checked = extIncognitoMode,
                        onCheckedChange = onExtIncognitoChange,
                        modifier = Modifier.padding(start = TrailingWidgetBuffer),
                    )
                }
            },
        )

        HorizontalDivider()
    }
}

@Composable
private fun InfoText(
    primaryText: String,
    secondaryText: String,
    modifier: Modifier = Modifier,
    primaryTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(interactionSource = null, indication = null, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier.then(clickableModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = primaryText,
            textAlign = TextAlign.Center,
            style = primaryTextStyle,
        )

        Text(
            text = secondaryText + if (onClick != null) " ⓘ" else "",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun InfoDivider() {
    VerticalDivider(
        modifier = Modifier.height(20.dp),
    )
}
