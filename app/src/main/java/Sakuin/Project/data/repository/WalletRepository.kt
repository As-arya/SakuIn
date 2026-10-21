package Sakuin.Project.data.repository

import Sakuin.Project.data.local.dao.WalletDao
import Sakuin.Project.data.local.entity.WalletEntity
import Sakuin.Project.data.local.entity.WalletType
import Sakuin.Project.data.local.model.WalletWithBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Totals shown on the Home balance stack: Total → Cash → Card → E-wallet. */
data class BalanceSummary(
    val total: Long,
    val cash: Long,
    val card: Long,
    val ewallet: Long,
) {
    companion object {
        fun from(wallets: List<WalletWithBalance>): BalanceSummary {
            fun sumOf(type: WalletType) = wallets.filter { it.wallet.type == type }.sumOf { it.balance }
            return BalanceSummary(
                total = wallets.sumOf { it.balance },
                cash = sumOf(WalletType.CASH),
                card = sumOf(WalletType.CARD),
                ewallet = sumOf(WalletType.EWALLET),
            )
        }
    }
}

class WalletRepository(
    private val walletDao: WalletDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Active (non-archived) wallets with their current balances. */
    fun observeWallets(): Flow<List<WalletWithBalance>> = walletDao.observeWithBalance()

    fun observeBalanceSummary(): Flow<BalanceSummary> = observeWallets().map(BalanceSummary::from)

    suspend fun hasWallets(): Boolean = walletDao.countActive() > 0

    suspend fun getWallet(id: Long): WalletEntity? = walletDao.getById(id)

    suspend fun addWallet(name: String, type: WalletType, initialBalance: Long): Long {
        require(name.isNotBlank()) { "Wallet name is required" }
        return walletDao.insert(
            WalletEntity(
                name = name.trim(),
                type = type,
                initialBalance = initialBalance,
                createdAt = clock(),
            )
        )
    }

    suspend fun updateWallet(id: Long, name: String, type: WalletType, initialBalance: Long) {
        require(name.isNotBlank()) { "Wallet name is required" }
        val current = requireNotNull(walletDao.getById(id)) { "Wallet $id not found" }
        walletDao.update(current.copy(name = name.trim(), type = type, initialBalance = initialBalance))
    }

    suspend fun archiveWallet(id: Long) = walletDao.setArchived(id, archived = true)

    suspend fun unarchiveWallet(id: Long) = walletDao.setArchived(id, archived = false)

    /** Only wallets without transactions can be deleted; others should be archived. */
    suspend fun deleteWallet(id: Long) {
        val wallet = walletDao.getById(id) ?: return
        check(walletDao.countTransactions(id) == 0) { "Wallet has transactions; archive it instead" }
        walletDao.delete(wallet)
    }
}
