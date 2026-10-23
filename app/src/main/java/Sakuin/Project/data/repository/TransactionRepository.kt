package Sakuin.Project.data.repository

import Sakuin.Project.data.local.dao.TransactionDao
import Sakuin.Project.data.local.entity.TransactionEntity
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.model.AmountPoint
import Sakuin.Project.data.local.model.CategoryTotal
import Sakuin.Project.data.local.model.PeriodTotals
import Sakuin.Project.data.local.model.TransactionWithDetails
import Sakuin.Project.util.TimeRange
import Sakuin.Project.util.monthsTimeRange
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Optional filters for the transaction list; null means "any". */
data class TransactionFilter(
    val type: TransactionType? = null,
    val walletId: Long? = null,
    val categoryId: Long? = null,
    val query: String? = null,
)

data class MonthTotal(
    val month: YearMonth,
    val income: Long,
    val expense: Long,
)

/** Groups income/expense points into the given months (in order), filling empty months with zero. */
fun bucketByMonth(points: List<AmountPoint>, months: List<YearMonth>, zone: ZoneId): List<MonthTotal> {
    val byMonth = points.groupBy { YearMonth.from(Instant.ofEpochMilli(it.occurredAt).atZone(zone)) }
    return months.map { month ->
        val inMonth = byMonth[month].orEmpty()
        MonthTotal(
            month = month,
            income = inMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            expense = inMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        )
    }
}

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun addExpense(
        amount: Long,
        walletId: Long,
        categoryId: Long,
        title: String,
        note: String? = null,
        occurredAt: Long = clock(),
        receiptImagePath: String? = null,
    ): Long {
        require(title.isNotBlank()) { "Item name is required" }
        return insert(
            type = TransactionType.EXPENSE,
            amount = amount,
            walletId = walletId,
            categoryId = categoryId,
            title = title.trim(),
            note = note,
            occurredAt = occurredAt,
            receiptImagePath = receiptImagePath,
        )
    }

    suspend fun addIncome(
        amount: Long,
        walletId: Long,
        categoryId: Long,
        note: String? = null,
        occurredAt: Long = clock(),
    ): Long = insert(
        type = TransactionType.INCOME,
        amount = amount,
        walletId = walletId,
        categoryId = categoryId,
        note = note,
        occurredAt = occurredAt,
    )

    /** [fee] is the admin fee, taken from [fromWalletId] on top of [amount]. */
    suspend fun addTransfer(
        amount: Long,
        fromWalletId: Long,
        toWalletId: Long,
        fee: Long = 0,
        note: String? = null,
        occurredAt: Long = clock(),
    ): Long {
        require(fromWalletId != toWalletId) { "Choose two different wallets" }
        return insert(
            type = TransactionType.TRANSFER,
            amount = amount,
            fee = fee,
            walletId = fromWalletId,
            toWalletId = toWalletId,
            note = note,
            occurredAt = occurredAt,
        )
    }

    suspend fun getTransaction(id: Long): TransactionEntity? = transactionDao.getById(id)

    suspend fun updateTransaction(transaction: TransactionEntity) {
        validate(transaction)
        transactionDao.update(transaction.copy(updatedAt = clock()))
    }

    /** Deletes and returns the transaction so the UI can offer "undo" via [restoreTransaction]. */
    suspend fun deleteTransaction(id: Long): TransactionEntity? {
        val transaction = transactionDao.getById(id) ?: return null
        transactionDao.delete(transaction)
        return transaction
    }

    /** Re-inserts a deleted transaction with its original id; [TransactionEntity.updatedAt] is refreshed. */
    suspend fun restoreTransaction(transaction: TransactionEntity) {
        transactionDao.insert(transaction.copy(updatedAt = clock()))
    }

    fun observeRecent(limit: Int = 5): Flow<List<TransactionWithDetails>> = transactionDao.observeRecent(limit)

    fun observeTransactions(
        range: TimeRange,
        filter: TransactionFilter = TransactionFilter(),
    ): Flow<List<TransactionWithDetails>> = transactionDao.observeBetween(
        start = range.startMillis,
        end = range.endMillis,
        type = filter.type,
        walletId = filter.walletId,
        categoryId = filter.categoryId,
        query = filter.query?.trim()?.takeIf { it.isNotEmpty() },
    )

    fun observeTotals(range: TimeRange, walletId: Long? = null): Flow<PeriodTotals> =
        transactionDao.observeTotals(range.startMillis, range.endMillis, walletId)

    fun observeExpenseByCategory(range: TimeRange, walletId: Long? = null): Flow<List<CategoryTotal>> =
        transactionDao.observeExpenseByCategory(range.startMillis, range.endMillis, walletId)

    /** Income vs expense for the [count] months ending with [lastMonth], oldest first. */
    fun observeMonthlyTotals(
        lastMonth: YearMonth,
        count: Int = 6,
        walletId: Long? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<MonthTotal>> {
        val months = (count - 1 downTo 0).map { lastMonth.minusMonths(it.toLong()) }
        val range = monthsTimeRange(months.first(), months.last(), zone)
        return transactionDao.observeIncomeExpensePoints(range.startMillis, range.endMillis, walletId)
            .map { bucketByMonth(it, months, zone) }
    }

    suspend fun suggestTitles(categoryId: Long, prefix: String, limit: Int = 5): List<String> =
        transactionDao.suggestTitles(categoryId, prefix.trim(), limit)

    suspend fun receiptImagePaths(): List<String> = transactionDao.receiptImagePaths()

    private suspend fun insert(
        type: TransactionType,
        amount: Long,
        fee: Long = 0,
        walletId: Long,
        toWalletId: Long? = null,
        categoryId: Long? = null,
        title: String = "",
        note: String? = null,
        occurredAt: Long,
        receiptImagePath: String? = null,
    ): Long {
        val now = clock()
        val transaction = TransactionEntity(
            type = type,
            amount = amount,
            fee = fee,
            walletId = walletId,
            toWalletId = toWalletId,
            categoryId = categoryId,
            title = title,
            note = note?.trim()?.takeIf { it.isNotEmpty() },
            occurredAt = occurredAt,
            receiptImagePath = receiptImagePath,
            createdAt = now,
            updatedAt = now,
        )
        validate(transaction)
        return transactionDao.insert(transaction)
    }

    private fun validate(transaction: TransactionEntity) {
        require(transaction.amount > 0) { "Amount must be greater than zero" }
        when (transaction.type) {
            TransactionType.TRANSFER -> {
                requireNotNull(transaction.toWalletId) { "Transfer needs a destination wallet" }
                require(transaction.toWalletId != transaction.walletId) { "Choose two different wallets" }
                require(transaction.fee >= 0) { "Fee cannot be negative" }
            }
            TransactionType.EXPENSE, TransactionType.INCOME -> {
                require(transaction.toWalletId == null) { "Only transfers have a destination wallet" }
                require(transaction.fee == 0L) { "Only transfers have a fee" }
                requireNotNull(transaction.categoryId) { "Category is required" }
            }
        }
    }
}
