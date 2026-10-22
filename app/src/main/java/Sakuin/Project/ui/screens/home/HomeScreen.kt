package Sakuin.Project.ui.screens.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import Sakuin.Project.R
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.model.PeriodTotals
import Sakuin.Project.data.local.model.TransactionWithDetails
import Sakuin.Project.data.prefs.SettingsRepository
import Sakuin.Project.data.repository.BalanceSummary
import Sakuin.Project.data.repository.TransactionRepository
import Sakuin.Project.data.repository.WalletRepository
import Sakuin.Project.ui.components.BalanceStack
import Sakuin.Project.ui.components.CircleIconButton
import Sakuin.Project.ui.components.TransactionRow
import Sakuin.Project.ui.sakuinViewModel
import Sakuin.Project.ui.theme.SakuinTheme
import Sakuin.Project.util.formatRupiah
import Sakuin.Project.util.toTimeRange
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeState(
    val userName: String? = null,
    val balance: BalanceSummary = BalanceSummary(0, 0, 0, 0),
    val month: PeriodTotals = PeriodTotals(0, 0),
    val recent: List<TransactionWithDetails> = emptyList(),
    val loading: Boolean = true,
)

class HomeViewModel(
    settings: SettingsRepository,
    wallets: WalletRepository,
    transactions: TransactionRepository,
    zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    val state: StateFlow<HomeState> = combine(
        settings.userName,
        wallets.observeBalanceSummary(),
        transactions.observeTotals(YearMonth.now(zone).toTimeRange(zone)),
        transactions.observeRecent(limit = 5),
    ) { name, balance, month, recent ->
        HomeState(userName = name, balance = balance, month = month, recent = recent, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}

private val RecentDateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val RecentTimeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/** Figma 01: greeting, swipeable balance stack, this month's totals, quick actions and recent activity. */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onAdd: (TransactionType) -> Unit,
    onScan: () -> Unit,
    onSeeAll: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
) {
    val viewModel = sakuinViewModel { HomeViewModel(it.settingsRepository, it.walletRepository, it.transactionRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var hidden by rememberSaveable { mutableStateOf(false) }
    val colors = SakuinTheme.colors
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = SakuinTheme.spacing.lg, vertical = SakuinTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.greeting, state.userName ?: stringResource(R.string.greeting_fallback)),
                    style = SakuinTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                CircleIconButton(R.drawable.ic_settings, stringResource(R.string.action_settings), onOpenSettings)
            }
        }
        item {
            BalanceStack(
                summary = state.balance,
                hidden = hidden,
                onToggleHidden = { hidden = !hidden },
                modifier = Modifier.padding(top = SakuinTheme.spacing.sm),
            )
        }
        item {
            Row(
                modifier = Modifier.padding(top = SakuinTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
            ) {
                SummaryCard(R.string.tab_income, R.drawable.ic_arrow_down, state.month.income, colors.income, hidden, Modifier.weight(1f))
                SummaryCard(R.string.tab_expense, R.drawable.ic_arrow_up, state.month.expense, colors.expense, hidden, Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.padding(vertical = SakuinTheme.spacing.md),
                horizontalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.md),
            ) {
                QuickAction(R.string.tab_expense, R.drawable.ic_arrow_up, { onAdd(TransactionType.EXPENSE) }, Modifier.weight(1f))
                QuickAction(R.string.tab_income, R.drawable.ic_arrow_down, { onAdd(TransactionType.INCOME) }, Modifier.weight(1f))
                QuickAction(R.string.tab_transfer, R.drawable.ic_transfer, { onAdd(TransactionType.TRANSFER) }, Modifier.weight(1f))
                QuickAction(R.string.home_scan, R.drawable.ic_scan, onScan, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.home_recent),
                    modifier = Modifier.weight(1f),
                    style = SakuinTheme.typography.titleSmall,
                    color = colors.textPrimary,
                )
                Text(
                    stringResource(R.string.home_see_all),
                    modifier = Modifier
                        .clip(RoundedCornerShape(SakuinTheme.radius.sm))
                        .clickable(role = Role.Button, onClick = onSeeAll)
                        .padding(SakuinTheme.spacing.xs),
                    style = SakuinTheme.typography.labelMedium,
                    color = colors.textBrand,
                )
            }
        }
        if (!state.loading && state.recent.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_recent_empty),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SakuinTheme.spacing.xl),
                    style = SakuinTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        items(state.recent, key = { "${it.transaction.id}-${it.transaction.updatedAt}" }) { row ->
            val at = Instant.ofEpochMilli(row.transaction.occurredAt).atZone(zone)
            val caption = when (at.toLocalDate()) {
                today -> RecentTimeFormat.format(at)
                today.minusDays(1) -> stringResource(R.string.day_yesterday)
                else -> RecentDateFormat.format(at)
            }
            TransactionRow(
                item = row,
                trailingCaption = caption,
                onClick = { onOpenTransaction(row.transaction.id) },
            )
        }
    }
}

@Composable
private fun SummaryCard(
    @StringRes label: Int,
    @DrawableRes icon: Int,
    amount: Long,
    color: Color,
    hidden: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = SakuinTheme.colors
    Column(
        modifier = modifier
            .background(colors.bgSurface, RoundedCornerShape(SakuinTheme.radius.lg))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(SakuinTheme.spacing.sm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, Modifier.size(16.dp), tint = colors.textSecondary)
            Text(stringResource(label), style = SakuinTheme.typography.caption, color = colors.textSecondary)
        }
        Text(
            if (hidden) "Rp ••••" else formatRupiah(amount),
            style = SakuinTheme.typography.bodyStrong,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuickAction(
    @StringRes label: Int,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SakuinTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(SakuinTheme.radius.lg))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(colors.bgSurface, RoundedCornerShape(SakuinTheme.radius.lg)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), null, Modifier.size(22.dp), tint = colors.textPrimary)
        }
        Text(stringResource(label), style = SakuinTheme.typography.caption, color = colors.textSecondary)
    }
}
