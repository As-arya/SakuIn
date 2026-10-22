package Sakuin.Project.ui.screens.wallets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import Sakuin.Project.R
import Sakuin.Project.data.local.entity.WalletType
import Sakuin.Project.ui.components.ButtonStyle
import Sakuin.Project.ui.components.SakuinButton
import Sakuin.Project.ui.components.SakuinChip
import Sakuin.Project.ui.components.SakuinTextField
import Sakuin.Project.ui.components.SakuinTopBar
import Sakuin.Project.ui.components.ThousandsSeparatorTransformation
import Sakuin.Project.ui.components.WalletCard
import Sakuin.Project.ui.components.icon
import Sakuin.Project.ui.components.label
import Sakuin.Project.ui.components.sanitizeAmountInput
import Sakuin.Project.ui.sakuinViewModel
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah
import Sakuin.Project.util.parseRupiahInput
import androidx.lifecycle.createSavedStateHandle

/** Figma 08: add a wallet, or edit/archive an existing one. Color follows the wallet type. */
@Composable
fun WalletFormScreen(onBack: () -> Unit, onSaved: () -> Unit, onArchived: () -> Unit) {
    val viewModel = sakuinViewModel { WalletFormViewModel(createSavedStateHandle(), it.walletRepository) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SakuinTheme.colors.bgSurface)
            .imePadding(),
    ) {
        SakuinTopBar(
            title = stringResource(if (viewModel.isEdit) R.string.wallet_edit_title else R.string.wallet_add_title),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.xl),
        ) {
            WalletCard(
                type = viewModel.type,
                name = viewModel.name.ifBlank { stringResource(R.string.field_wallet_name) },
                balance = formatRupiah(parseRupiahInput(viewModel.balanceDigits) ?: 0),
                modifier = Modifier
                    .width(240.dp)
                    .align(Alignment.CenterHorizontally),
            )
            SakuinTextField(
                value = viewModel.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.field_wallet_name),
                leadingIcon = R.drawable.ic_wallet,
                isError = viewModel.showNameError,
                helper = if (viewModel.showNameError) stringResource(R.string.error_name_required) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            Column(verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
                Text(stringResource(R.string.field_type), style = SakuinTheme.typography.labelMedium, color = SakuinTheme.colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm)) {
                    WalletType.entries.forEach { type ->
                        SakuinChip(
                            label = stringResource(type.label),
                            selected = viewModel.type == type,
                            onClick = { viewModel.type = type },
                            icon = type.icon,
                        )
                    }
                }
            }
            SakuinTextField(
                value = viewModel.balanceDigits,
                onValueChange = { viewModel.balanceDigits = sanitizeAmountInput(it) },
                label = stringResource(R.string.field_starting_balance),
                leadingIcon = R.drawable.ic_coins,
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                visualTransformation = ThousandsSeparatorTransformation,
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
        ) {
            SakuinButton(
                text = stringResource(R.string.wallet_save),
                onClick = { viewModel.save(onSaved) },
                enabled = !viewModel.saving,
                modifier = Modifier.fillMaxWidth(),
            )
            if (viewModel.isEdit) {
                SakuinButton(
                    text = stringResource(R.string.wallet_archive),
                    onClick = { viewModel.archive(onArchived) },
                    style = ButtonStyle.Outline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
