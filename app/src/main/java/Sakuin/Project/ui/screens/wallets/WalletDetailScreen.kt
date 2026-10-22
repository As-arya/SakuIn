package Sakuin.Project.ui.screens.wallets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import Sakuin.Project.R
import Sakuin.Project.ui.components.ButtonStyle
import Sakuin.Project.ui.components.SakuinButton
import Sakuin.Project.ui.components.SakuinTopBar
import Sakuin.Project.ui.components.TransactionRow
import Sakuin.Project.ui.components.WalletCard
import Sakuin.Project.ui.sakuinViewModel
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RowDateFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)

/** Balance card, a transfer shortcut and the full history of one wallet. */
@Composable
fun WalletDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onTransfer: (Long) -> Unit,
    onOpenTransaction: (Long) -> Unit,
) {
    val viewModel = sakuinViewModel {
        WalletDetailViewModel(createSavedStateHandle(), it.walletRepository, it.transactionRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = SakuinTheme.colors
    val zone = ZoneId.systemDefault()

    Column(Modifier.fillMaxSize()) {
        SakuinTopBar(
            title = state.wallet?.wallet?.name.orEmpty(),
            onBack = onBack,
            actions = {
                IconButton(onClick = { onEdit(viewModel.walletId) }) {
                    Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.action_edit), tint = colors.textPrimary)
                }
            },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
        ) {
            state.wallet?.let { item ->
                item {
                    WalletCard(
                        type = item.wallet.type,
                        name = item.wallet.name,
                        caption = stringResource(R.string.wallet_balance),
                        balance = formatRupiah(item.balance),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    SakuinButton(
                        text = stringResource(R.string.tab_transfer),
                        onClick = { onTransfer(item.wallet.id) },
                        style = ButtonStyle.Secondary,
                        icon = R.drawable.ic_transfer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = SakuinTheme.spacing.sm),
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.wallet_history),
                    modifier = Modifier.padding(top = SakuinTheme.spacing.sm),
                    style = SakuinTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
            }
            if (state.history.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.wallet_history_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = SakuinTheme.spacing.xl),
                        style = SakuinTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(state.history, key = { it.transaction.id }) { row ->
                TransactionRow(
                    item = row,
                    trailingCaption = RowDateFormat.format(Instant.ofEpochMilli(row.transaction.occurredAt).atZone(zone)),
                    onClick = { onOpenTransaction(row.transaction.id) },
                )
            }
        }
    }
}
