package Sakuin.Project.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import Sakuin.Project.BuildConfig
import Sakuin.Project.R
import Sakuin.Project.ui.components.SakuinTopBar
import Sakuin.Project.ui.theme.SakuinTheme

/** Figma 11. Appearance and receipt-scan options join this screen with their features. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenCategories: () -> Unit, onOpenWallets: () -> Unit) {
    val colors = SakuinTheme.colors
    Column(Modifier.fillMaxSize()) {
        SakuinTopBar(title = stringResource(R.string.title_settings), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
        ) {
            Text(stringResource(R.string.settings_group_data), style = SakuinTheme.typography.labelMedium, color = colors.textSecondary)
            Column(
                Modifier
                    .clip(RoundedCornerShape(SakuinTheme.radius.lg))
                    .background(colors.bgSurface),
            ) {
                SettingsRow(R.drawable.ic_tag, R.string.title_categories, onOpenCategories)
                HorizontalDivider(thickness = 1.dp, color = colors.border)
                SettingsRow(R.drawable.ic_wallet, R.string.title_wallets, onOpenWallets)
            }
            Text(
                stringResource(R.string.settings_offline_note, BuildConfig.VERSION_NAME),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SakuinTheme.spacing.sm),
                style = SakuinTheme.typography.caption,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SettingsRow(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    val colors = SakuinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = SakuinTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, Modifier.size(22.dp), tint = colors.textPrimary)
        Text(stringResource(label), modifier = Modifier.weight(1f), style = SakuinTheme.typography.bodyStrong, color = colors.textPrimary)
        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(20.dp), tint = colors.textSecondary)
    }
}
