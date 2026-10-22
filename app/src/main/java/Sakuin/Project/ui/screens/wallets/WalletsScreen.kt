package Sakuin.Project.ui.screens.wallets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import Sakuin.Project.R
import Sakuin.Project.data.local.entity.WalletEntity
import Sakuin.Project.data.local.entity.WalletType
import Sakuin.Project.data.local.model.WalletWithBalance
import Sakuin.Project.data.repository.BalanceSummary
import Sakuin.Project.ui.components.CircleIconButton
import Sakuin.Project.ui.components.WalletIconBox
import Sakuin.Project.ui.components.color
import Sakuin.Project.ui.components.label
import Sakuin.Project.ui.sakuinViewModel
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah

/** Figma 07: total balance with composition bar, then every active wallet. */
@Composable
fun WalletsScreen(onAddWallet: () -> Unit, onOpenWallet: (Long) -> Unit) {
    val viewModel = sakuinViewModel { WalletsViewModel(it.walletRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    WalletsContent(state, onAddWallet, onOpenWallet)
}

@Composable
private fun WalletsContent(state: WalletsState, onAddWallet: () -> Unit, onOpenWallet: (Long) -> Unit) {
    val colors = SakuinTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SakuinTheme.spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.title_wallets), style = SakuinTheme.typography.headlineLarge, color = colors.textPrimary)
                CircleIconButton(
                    icon = R.drawable.ic_plus,
                    contentDescription = stringResource(R.string.wallet_add),
                    onClick = onAddWallet,
                    background = colors.primary,
                    tint = colors.onPrimary,
                )
            }
        }
        item { TotalCard(state.summary, modifier = Modifier.padding(bottom = SakuinTheme.spacing.sm)) }
        if (!state.loading && state.wallets.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.wallets_empty),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SakuinTheme.spacing.xxl),
                    style = SakuinTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        items(state.wallets, key = { it.wallet.id }) { item ->
            WalletRow(item, onClick = { onOpenWallet(item.wallet.id) })
        }
    }
}

@Composable
private fun TotalCard(summary: BalanceSummary, modifier: Modifier = Modifier) {
    val colors = SakuinTheme.colors
    val parts = listOf(
        WalletType.CASH to summary.cash,
        WalletType.CARD to summary.card,
        WalletType.EWALLET to summary.ewallet,
    )
    val positive = parts.filter { it.second > 0 }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface, RoundedCornerShape(SakuinTheme.radius.xl))
            .padding(SakuinTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
    ) {
        Text(stringResource(R.string.wallets_total_balance), style = SakuinTheme.typography.labelMedium, color = colors.textSecondary)
        Text(formatRupiah(summary.total), style = SakuinTheme.typography.headlineLarge, color = colors.textPrimary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            if (positive.isEmpty()) {
                Box(Modifier.fillMaxSize().background(colors.bgSurfaceVariant, CircleShape))
            } else {
                positive.forEach { (type, amount) ->
                    Box(
                        Modifier
                            .weight(amount.toFloat())
                            .fillMaxSize()
                            .background(type.color, CircleShape),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.lg)) {
            parts.forEach { (type, _) ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(type.color, CircleShape))
                    Text(stringResource(type.label), style = SakuinTheme.typography.caption, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun WalletRow(item: WalletWithBalance, onClick: () -> Unit) {
    val colors = SakuinTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SakuinTheme.radius.lg))
            .background(colors.bgSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(SakuinTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletIconBox(item.wallet.type)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.wallet.name, style = SakuinTheme.typography.bodyStrong, color = colors.textPrimary)
            Text(stringResource(item.wallet.type.label), style = SakuinTheme.typography.caption, color = colors.textSecondary)
        }
        Text(
            formatRupiah(item.balance),
            style = SakuinTheme.typography.bodyStrong,
            color = if (item.balance < 0) colors.expense else colors.textPrimary,
        )
        Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(20.dp), tint = colors.textSecondary)
    }
}

@Preview(showBackground = true)
@Composable
private fun WalletsContentPreview() {
    val wallets = listOf(
        WalletWithBalance(WalletEntity(1, "My Wallet", WalletType.CASH, 0, createdAt = 0), 350_000),
        WalletWithBalance(WalletEntity(2, "BCA", WalletType.CARD, 0, createdAt = 0), 2_150_000),
        WalletWithBalance(WalletEntity(3, "GoPay", WalletType.EWALLET, 0, createdAt = 0), 125_500),
    )
    SakuinTheme {
        WalletsContent(
            WalletsState(loading = false, wallets = wallets, summary = BalanceSummary.from(wallets)),
            onAddWallet = {},
            onOpenWallet = {},
        )
    }
}
