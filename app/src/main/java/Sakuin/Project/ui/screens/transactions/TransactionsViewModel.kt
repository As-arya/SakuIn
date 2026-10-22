package Sakuin.Project.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.local.entity.CategoryKind
import Sakuin.Project.data.local.entity.TransactionEntity
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.model.TransactionWithDetails
import Sakuin.Project.data.local.model.WalletWithBalance
import Sakuin.Project.data.repository.CategoryRepository
import Sakuin.Project.data.repository.TransactionFilter
import Sakuin.Project.data.repository.TransactionRepository
import Sakuin.Project.data.repository.WalletRepository
import Sakuin.Project.util.toTimeRange
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One day in the list; [net] is income minus expense for that day (transfers are neutral). */
data class DayGroup(
    val date: LocalDate,
    val items: List<TransactionWithDetails>,
    val net: Long,
)

fun groupByDay(items: List<TransactionWithDetails>, zone: ZoneId): List<DayGroup> =
    items.groupBy { Instant.ofEpochMilli(it.transaction.occurredAt).atZone(zone).toLocalDate() }
        .map { (date, rows) ->
            val net = rows.sumOf {
                when (it.transaction.type) {
                    TransactionType.INCOME -> it.transaction.amount
                    TransactionType.EXPENSE -> -it.transaction.amount
                    TransactionType.TRANSFER -> 0
                }
            }
            DayGroup(date, rows, net)
        }
        .sortedByDescending { it.date }

data class TransactionsState(
    val loading: Boolean = true,
    val groups: List<DayGroup> = emptyList(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    wallets: WalletRepository,
    categories: CategoryRepository,
    private val transactions: TransactionRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val _month = MutableStateFlow(YearMonth.now(zone))
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _filter = MutableStateFlow(TransactionFilter())
    /** Type / wallet / category filters; the search text is kept separately so typing stays instant. */
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val wallets: StateFlow<List<WalletWithBalance>> = wallets.observeWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = combine(
        categories.observeCategories(CategoryKind.EXPENSE),
        categories.observeCategories(CategoryKind.INCOME),
    ) { expense, income -> expense + income }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<TransactionsState> = combine(_month, _filter, _query.debounce(250)) { month, filter, query ->
        Triple(month, filter, query)
    }.flatMapLatest { (month, filter, query) ->
        transactions.observeTransactions(month.toTimeRange(zone), filter.copy(query = query))
            .map { TransactionsState(loading = false, groups = groupByDay(it, zone)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsState())

    val canGoNextMonth: Boolean get() = _month.value < YearMonth.now(zone)

    fun previousMonth() = _month.update { it.minusMonths(1) }

    fun nextMonth() {
        if (canGoNextMonth) _month.update { it.plusMonths(1) }
    }

    fun setType(type: TransactionType?) = _filter.update { it.copy(type = type) }

    fun setWallet(id: Long?) = _filter.update { it.copy(walletId = id) }

    fun setCategory(id: Long?) = _filter.update { it.copy(categoryId = id) }

    fun clearExtraFilters() = _filter.update { it.copy(walletId = null, categoryId = null) }

    /** Search text, type and the sheet filters all at once (from the "no matches" state). */
    fun clearAll() {
        _query.value = ""
        _filter.value = TransactionFilter()
    }

    fun setQuery(text: String) {
        _query.value = text.take(50)
    }

    fun delete(id: Long, onDeleted: (TransactionEntity) -> Unit) {
        viewModelScope.launch { transactions.deleteTransaction(id)?.let(onDeleted) }
    }

    fun restore(transaction: TransactionEntity) {
        viewModelScope.launch { transactions.restoreTransaction(transaction) }
    }
}
