package Sakuin.Project.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

enum class WalletType { CASH, CARD, EWALLET }

/**
 * A place money is kept (e.g. "My Wallet", "BCA", "GoPay"). The current balance is not
 * stored; it is derived from [initialBalance] and the wallet's transactions.
 * The wallet's color comes from its [type].
 */
@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: WalletType,
    /** Rupiah, no decimals. */
    val initialBalance: Long,
    val isArchived: Boolean = false,
    val createdAt: Long,
)
