package Sakuin.Project.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import Sakuin.Project.BuildConfig
import Sakuin.Project.R
import Sakuin.Project.data.prefs.SettingsRepository
import Sakuin.Project.data.prefs.ThemeMode
import Sakuin.Project.ui.components.SakuinTopBar
import Sakuin.Project.ui.components.asHeading
import Sakuin.Project.ui.sakuinViewModel
import Sakuin.Project.ui.theme.Palette
import Sakuin.Project.ui.theme.SakuinTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.System,
    val receiptScan: Boolean = true,
    val keepReceiptPhotos: Boolean = true,
)

class SettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<SettingsState> = combine(
        settings.themeMode,
        settings.receiptScanEnabled,
        settings.keepReceiptPhotos,
        ::SettingsState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    /** The switch picks an explicit theme; until then the app follows the phone. */
    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch { settings.setThemeMode(if (dark) ThemeMode.Dark else ThemeMode.Light) }
    }

    fun setReceiptScan(enabled: Boolean) {
        viewModelScope.launch { settings.setReceiptScanEnabled(enabled) }
    }

    fun setKeepReceiptPhotos(keep: Boolean) {
        viewModelScope.launch { settings.setKeepReceiptPhotos(keep) }
    }
}

/** Figma 11: data, appearance and receipt-scan options. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenCategories: () -> Unit, onOpenWallets: () -> Unit) {
    val viewModel = sakuinViewModel { SettingsViewModel(it.settingsRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = SakuinTheme.colors
    val dark = when (state.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    Column(Modifier.fillMaxSize()) {
        SakuinTopBar(title = stringResource(R.string.title_settings), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
        ) {
            SettingsGroup(R.string.settings_group_data) {
                SettingsRow(R.drawable.ic_tag, R.string.title_categories, onOpenCategories)
                GroupDivider()
                SettingsRow(R.drawable.ic_wallet, R.string.title_wallets, onOpenWallets)
            }
            SettingsGroup(R.string.settings_group_appearance) {
                SwitchRow(R.drawable.ic_eye, R.string.settings_dark_mode, dark, viewModel::setDarkMode)
                GroupDivider()
                ValueRow(R.drawable.ic_coins, R.string.settings_currency, stringResource(R.string.settings_currency_idr))
            }
            SettingsGroup(R.string.settings_group_scan) {
                SwitchRow(R.drawable.ic_sparkles, R.string.settings_ai_scan, state.receiptScan, viewModel::setReceiptScan)
                GroupDivider()
                SwitchRow(R.drawable.ic_image, R.string.settings_keep_photos, state.keepReceiptPhotos, viewModel::setKeepReceiptPhotos)
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
private fun SettingsGroup(@StringRes title: Int, rows: @Composable ColumnScope.() -> Unit) {
    val colors = SakuinTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
        Text(stringResource(title), modifier = Modifier.asHeading(), style = SakuinTheme.typography.labelMedium, color = colors.textSecondary)
        Column(
            Modifier
                .clip(RoundedCornerShape(SakuinTheme.radius.lg))
                .background(colors.bgSurface),
            content = rows,
        )
    }
}

@Composable
private fun GroupDivider() = HorizontalDivider(thickness = 1.dp, color = SakuinTheme.colors.border)

@Composable
private fun RowContent(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    val colors = SakuinTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = SakuinTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, Modifier.size(22.dp), tint = colors.textPrimary)
        Text(stringResource(label), modifier = Modifier.weight(1f), style = SakuinTheme.typography.bodyStrong, color = colors.textPrimary)
        trailing()
    }
}

@Composable
private fun SettingsRow(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    RowContent(icon, label, Modifier.clickable(role = Role.Button, onClick = onClick)) {
        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(20.dp), tint = SakuinTheme.colors.textSecondary)
    }
}

/** The whole row toggles, so TalkBack reads it as one switch with its label. */
@Composable
private fun SwitchRow(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = SakuinTheme.colors
    RowContent(icon, label, Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.Gray0,
                checkedTrackColor = colors.primary,
                checkedBorderColor = colors.primary,
                uncheckedThumbColor = Palette.Gray0,
                uncheckedTrackColor = colors.border,
                uncheckedBorderColor = colors.border,
            ),
        )
    }
}

@Composable
private fun ValueRow(@DrawableRes icon: Int, @StringRes label: Int, value: String) {
    RowContent(icon, label) {
        Text(value, style = SakuinTheme.typography.labelMedium, color = SakuinTheme.colors.textSecondary)
    }
}
