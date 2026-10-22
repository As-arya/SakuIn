package Sakuin.Project.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import Sakuin.Project.data.local.model.CategoryTotal
import Sakuin.Project.data.local.model.PeriodTotals
import Sakuin.Project.data.local.model.WalletWithBalance
import Sakuin.Project.data.repository.MonthTotal
import Sakuin.Project.data.repository.TransactionRepository
import Sakuin.Project.data.repository.WalletRepository
import Sakuin.Project.util.TimeRange
import Sakuin.Project.util.toTimeRange
import Sakuin.Project.util.weekTimeRange
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class StatsPeriod { Week, Month, Year }

/** The period being looked at: its kind plus any date inside it. */
data class PeriodSelection(val period: StatsPeriod, val anchor: LocalDate) {
    fun range(zone: ZoneId): TimeRange = when (period) {
        StatsPeriod.Week -> anchor.weekTimeRange(zone)
        StatsPeriod.Month -> YearMonth.from(anchor).toTimeRange(zone)
        StatsPeriod.Year -> Year.from(anchor).toTimeRange(zone)
    }

    fun shift(steps: Long): PeriodSelection = copy(
        anchor = when (period) {
            StatsPeriod.Week -> anchor.plusWeeks(steps)
            StatsPeriod.Month -> anchor.plusMonths(steps)
            StatsPeriod.Year -> anchor.plusYears(steps)
        },
    )

    fun contains(date: LocalDate, zone: ZoneId): Boolean =
        range(zone).contains(date.atStartOfDay(zone).toInstant().toEpochMilli())
}

data class StatsState(
    val totals: PeriodTotals = PeriodTotals(0, 0),
    val byCategory: List<CategoryTotal> = emptyList(),
    val months: List<MonthTotal> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    wallets: WalletRepository,
    private val transactions: TransactionRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val _selection = MutableStateFlow(PeriodSelection(StatsPeriod.Month, LocalDate.now(zone)))
    val selection: StateFlow<PeriodSelection> = _selection.asStateFlow()

    private val _walletId = MutableStateFlow<Long?>(null)
    val walletId: StateFlow<Long?> = _walletId.asStateFlow()

    val wallets: StateFlow<List<WalletWithBalance>> = wallets.observeWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<StatsState> = combine(_selection, _walletId) { selection, walletId -> selection to walletId }
        .flatMapLatest { (selection, walletId) ->
            val range = selection.range(zone)
            combine(
                transactions.observeTotals(range, walletId),
                transactions.observeExpenseByCategory(range, walletId),
                // The bar chart always shows the six months up to the selected period.
                transactions.observeMonthlyTotals(YearMonth.from(selection.anchor), count = 6, walletId = walletId, zone = zone),
            ) { totals, byCategory, months -> StatsState(totals, byCategory, months) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())

    fun setPeriod(period: StatsPeriod) = _selection.update { it.copy(period = period) }

    fun previous() = _selection.update { it.shift(-1) }

    fun next() {
        if (!_selection.value.contains(LocalDate.now(zone), zone)) _selection.update { it.shift(1) }
    }

    fun setWallet(id: Long?) {
        _walletId.value = id
    }
}
