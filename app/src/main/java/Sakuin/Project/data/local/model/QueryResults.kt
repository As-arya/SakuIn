package Sakuin.Project.data.local.model

import androidx.room3.Embedded
import Sakuin.Project.data.local.entity.TransactionEntity
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.entity.WalletEntity

/** A wallet together with its derived current balance. */
data class WalletWithBalance(
    @Embedded val wallet: WalletEntity,
    val balance: Long,
)

/** A transaction joined with the names needed to show it in a list. */
data class TransactionWithDetails(
    @Embedded val transaction: TransactionEntity,
    val categoryName: String?,
    val categoryIconKey: String?,
    val walletName: String,
    val toWalletName: String?,
)

data class PeriodTotals(
    val income: Long,
    val expense: Long,
) {
    val net: Long get() = income - expense
}

/** Expense total for one category; [categoryId] is null for uncategorized expenses. */
data class CategoryTotal(
    val categoryId: Long?,
    val categoryName: String?,
    val categoryIconKey: String?,
    val total: Long,
)

/** Minimal income/expense row; month bucketing happens in Kotlin so it follows the device time zone. */
data class AmountPoint(
    val occurredAt: Long,
    val type: TransactionType,
    val amount: Long,
)
