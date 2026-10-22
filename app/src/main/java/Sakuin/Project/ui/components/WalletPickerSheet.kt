package Sakuin.Project.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import Sakuin.Project.R
import Sakuin.Project.data.local.model.WalletWithBalance
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah

/** "Choose wallet" bottom sheet (Figma screen 06). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletPickerSheet(
    wallets: List<WalletWithBalance>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
    onAddWallet: (() -> Unit)? = null,
) {
    val colors = SakuinTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bgSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SakuinTheme.spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.wallet_picker_title),
                    modifier = Modifier.weight(1f),
                    style = SakuinTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                IconButton(onClick = onDismiss) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close), tint = colors.textPrimary)
                }
            }
            wallets.forEach { item ->
                val selected = item.wallet.id == selectedId
                val shape = RoundedCornerShape(SakuinTheme.radius.lg)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.border, shape)
                        .clickable(role = Role.RadioButton) { onSelect(item.wallet.id) }
                        .padding(SakuinTheme.spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WalletIconBox(item.wallet.type)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.wallet.name, style = SakuinTheme.typography.bodyStrong, color = colors.textPrimary)
                        Text(formatRupiah(item.balance), style = SakuinTheme.typography.caption, color = colors.textSecondary)
                    }
                    if (selected) {
                        Icon(painterResource(R.drawable.ic_check), null, Modifier.size(22.dp), tint = colors.textBrand)
                    }
                }
            }
            if (onAddWallet != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(SakuinTheme.radius.lg))
                        .clickable(role = Role.Button, onClick = onAddWallet)
                        .padding(vertical = SakuinTheme.spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_plus), null, Modifier.size(20.dp), tint = colors.textBrand)
                    Text(stringResource(R.string.wallet_picker_add), style = SakuinTheme.typography.bodyStrong, color = colors.textBrand)
                }
            }
        }
    }
}
