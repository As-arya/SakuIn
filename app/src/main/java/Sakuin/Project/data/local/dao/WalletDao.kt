package Sakuin.Project.data.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import Sakuin.Project.data.local.entity.WalletEntity
import Sakuin.Project.data.local.model.WalletWithBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Insert
    suspend fun insert(wallet: WalletEntity): Long

    @Update
    suspend fun update(wallet: WalletEntity)

    @Delete
    suspend fun delete(wallet: WalletEntity)

    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun getById(id: Long): WalletEntity?

    @Query("SELECT COUNT(*) FROM wallets WHERE isArchived = 0")
    suspend fun countActive(): Int

    @Query("UPDATE wallets SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("SELECT COUNT(*) FROM transactions WHERE walletId = :id OR toWalletId = :id")
    suspend fun countTransactions(id: Long): Int

    /**
     * Balance = initial balance + income − expenses − transfers out (with their fees) + transfers in.
     */
    @Query(
        """
        SELECT wallets.*,
            wallets.initialBalance
            + COALESCE((SELECT SUM(t.amount) FROM transactions t
                WHERE t.walletId = wallets.id AND t.type = 'INCOME'), 0)
            - COALESCE((SELECT SUM(t.amount + t.fee) FROM transactions t
                WHERE t.walletId = wallets.id AND t.type IN ('EXPENSE', 'TRANSFER')), 0)
            + COALESCE((SELECT SUM(t.amount) FROM transactions t
                WHERE t.toWalletId = wallets.id AND t.type = 'TRANSFER'), 0)
            AS balance
        FROM wallets
        WHERE wallets.isArchived = 0
        ORDER BY wallets.createdAt, wallets.id
        """
    )
    fun observeWithBalance(): Flow<List<WalletWithBalance>>
}
