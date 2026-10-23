package Sakuin.Project.data.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import Sakuin.Project.data.local.entity.TransactionEntity
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.model.AmountPoint
import Sakuin.Project.data.local.model.CategoryTotal
import Sakuin.Project.data.local.model.PeriodTotals
import Sakuin.Project.data.local.model.TransactionWithDetails
import kotlinx.coroutines.flow.Flow

private const val DETAILS_SELECT = """
    SELECT t.*,
        c.name AS categoryName,
        c.iconKey AS categoryIconKey,
        w.name AS walletName,
        tw.name AS toWalletName
    FROM transactions t
    JOIN wallets w ON w.id = t.walletId
    LEFT JOIN wallets tw ON tw.id = t.toWalletId
    LEFT JOIN categories c ON c.id = t.categoryId
"""

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("$DETAILS_SELECT ORDER BY t.occurredAt DESC, t.id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionWithDetails>>

    /** Null filter arguments mean "any". [query] matches the title or the note. */
    @Query(
        """
        $DETAILS_SELECT
        WHERE t.occurredAt >= :start AND t.occurredAt < :end
            AND (:type IS NULL OR t.type = :type)
            AND (:walletId IS NULL OR t.walletId = :walletId OR t.toWalletId = :walletId)
            AND (:categoryId IS NULL OR t.categoryId = :categoryId)
            AND (:query IS NULL OR t.title LIKE '%' || :query || '%' OR t.note LIKE '%' || :query || '%')
        ORDER BY t.occurredAt DESC, t.id DESC
        """
    )
    fun observeBetween(
        start: Long,
        end: Long,
        type: TransactionType?,
        walletId: Long?,
        categoryId: Long?,
        query: String?,
    ): Flow<List<TransactionWithDetails>>

    /** Transfer fees count as expenses of the wallet they were paid from. */
    @Query(
        """
        SELECT
            COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount END), 0) AS income,
            COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount WHEN type = 'TRANSFER' THEN fee END), 0) AS expense
        FROM transactions
        WHERE occurredAt >= :start AND occurredAt < :end
            AND (:walletId IS NULL OR walletId = :walletId)
        """
    )
    fun observeTotals(start: Long, end: Long, walletId: Long?): Flow<PeriodTotals>

    /** Expenses per category, plus one row for transfer fees ([CategoryTotal.isTransferFee]) when there are any. */
    @Query(
        """
        SELECT t.categoryId AS categoryId,
            c.name AS categoryName,
            c.iconKey AS categoryIconKey,
            SUM(t.amount) AS total,
            0 AS isTransferFee
        FROM transactions t
        LEFT JOIN categories c ON c.id = t.categoryId
        WHERE t.type = 'EXPENSE'
            AND t.occurredAt >= :start AND t.occurredAt < :end
            AND (:walletId IS NULL OR t.walletId = :walletId)
        GROUP BY t.categoryId
        UNION ALL
        SELECT NULL, NULL, NULL, SUM(fee), 1
        FROM transactions
        WHERE type = 'TRANSFER' AND fee > 0
            AND occurredAt >= :start AND occurredAt < :end
            AND (:walletId IS NULL OR walletId = :walletId)
        GROUP BY type
        ORDER BY total DESC
        """
    )
    fun observeExpenseByCategory(start: Long, end: Long, walletId: Long?): Flow<List<CategoryTotal>>

    /** Transfer fees come back as expense points. */
    @Query(
        """
        SELECT occurredAt, type, amount FROM transactions
        WHERE type IN ('INCOME', 'EXPENSE')
            AND occurredAt >= :start AND occurredAt < :end
            AND (:walletId IS NULL OR walletId = :walletId)
        UNION ALL
        SELECT occurredAt, 'EXPENSE', fee FROM transactions
        WHERE type = 'TRANSFER' AND fee > 0
            AND occurredAt >= :start AND occurredAt < :end
            AND (:walletId IS NULL OR walletId = :walletId)
        """
    )
    fun observeIncomeExpensePoints(start: Long, end: Long, walletId: Long?): Flow<List<AmountPoint>>

    /** Recent item names used in a category, most recent first, for autocomplete. */
    @Query(
        """
        SELECT title FROM transactions
        WHERE type = 'EXPENSE' AND categoryId = :categoryId
            AND title != '' AND title LIKE :prefix || '%'
        GROUP BY title
        ORDER BY MAX(occurredAt) DESC
        LIMIT :limit
        """
    )
    suspend fun suggestTitles(categoryId: Long, prefix: String, limit: Int): List<String>

    @Query("SELECT receiptImagePath FROM transactions WHERE receiptImagePath IS NOT NULL")
    suspend fun receiptImagePaths(): List<String>
}
