package Sakuin.Project.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

enum class TransactionType { EXPENSE, INCOME, TRANSFER }

/**
 * One money movement.
 * - EXPENSE: money leaves [walletId]; [categoryId] is the expense category, [title] the item name.
 * - INCOME: money enters [walletId]; [categoryId] is the income source.
 * - TRANSFER: money moves from [walletId] to [toWalletId]; not counted as income or expense, except
 *   its admin [fee], which also leaves [walletId] and counts as an expense.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["walletId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["toWalletId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("walletId"),
        Index("toWalletId"),
        Index("categoryId"),
        Index("occurredAt"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    /** Rupiah, always positive; [type] decides the direction. */
    val amount: Long,
    /** Rupiah charged on top of [amount] by the bank or e-wallet; only transfers have one. */
    @ColumnInfo(defaultValue = "0")
    val fee: Long = 0,
    val walletId: Long,
    val toWalletId: Long? = null,
    val categoryId: Long? = null,
    val title: String = "",
    val note: String? = null,
    /** When the money moved, epoch millis. */
    val occurredAt: Long,
    val receiptImagePath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
