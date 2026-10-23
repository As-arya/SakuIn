package Sakuin.Project.ui.screens.transactions

import Sakuin.Project.data.local.entity.TransactionEntity
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.model.TransactionWithDetails
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupByDayTest {
    private val jakarta = ZoneId.of("Asia/Jakarta")

    private fun row(id: Long, type: TransactionType, amount: Long, day: Int, hour: Int, fee: Long = 0) = TransactionWithDetails(
        transaction = TransactionEntity(
            id = id,
            type = type,
            amount = amount,
            fee = fee,
            walletId = 1,
            toWalletId = if (type == TransactionType.TRANSFER) 2 else null,
            categoryId = if (type == TransactionType.TRANSFER) null else 1,
            occurredAt = ZonedDateTime.of(2026, 10, day, hour, 0, 0, 0, jakarta).toInstant().toEpochMilli(),
            createdAt = 0,
            updatedAt = 0,
        ),
        categoryName = null,
        categoryIconKey = null,
        walletName = "My Wallet",
        toWalletName = null,
    )

    @Test
    fun groupsPerLocalDayNewestFirstWithNetIgnoringTransfers() {
        val groups = groupByDay(
            listOf(
                row(1, TransactionType.EXPENSE, 18_000, day = 22, hour = 12),
                row(2, TransactionType.INCOME, 500_000, day = 22, hour = 9),
                row(3, TransactionType.TRANSFER, 200_000, day = 22, hour = 8),
                row(4, TransactionType.EXPENSE, 5_000, day = 21, hour = 23),
            ),
            jakarta,
        )

        assertEquals(listOf(LocalDate.of(2026, 10, 22), LocalDate.of(2026, 10, 21)), groups.map { it.date })
        assertEquals(listOf(482_000L, -5_000L), groups.map { it.net })
        assertEquals(listOf(1L, 2L, 3L), groups.first().items.map { it.transaction.id })
    }

    @Test
    fun transferFeesCountAgainstTheDay() {
        val groups = groupByDay(
            listOf(
                row(1, TransactionType.TRANSFER, 200_000, day = 22, hour = 8, fee = 2_500),
                row(2, TransactionType.EXPENSE, 18_000, day = 22, hour = 12),
            ),
            jakarta,
        )

        assertEquals(listOf(-20_500L), groups.map { it.net })
    }
}
