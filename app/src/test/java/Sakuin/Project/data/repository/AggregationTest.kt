package Sakuin.Project.data.repository

import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.entity.WalletEntity
import Sakuin.Project.data.local.entity.WalletType
import Sakuin.Project.data.local.model.AmountPoint
import Sakuin.Project.data.local.model.WalletWithBalance
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class AggregationTest {
    private val jakarta = ZoneId.of("Asia/Jakarta")

    private fun at(y: Int, m: Int, d: Int, h: Int = 12) =
        ZonedDateTime.of(y, m, d, h, 0, 0, 0, jakarta).toInstant().toEpochMilli()

    @Test
    fun bucketByMonthGroupsAndFillsEmptyMonths() {
        val points = listOf(
            AmountPoint(at(2026, 9, 3), TransactionType.INCOME, 4_500_000),
            AmountPoint(at(2026, 9, 10), TransactionType.EXPENSE, 18_000),
            AmountPoint(at(2026, 9, 11), TransactionType.EXPENSE, 5_000),
            AmountPoint(at(2026, 10, 1), TransactionType.EXPENSE, 1_500_000),
        )
        val months = listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10))

        val result = bucketByMonth(points, months, jakarta)

        assertEquals(
            listOf(
                MonthTotal(YearMonth.of(2026, 8), income = 0, expense = 0),
                MonthTotal(YearMonth.of(2026, 9), income = 4_500_000, expense = 23_000),
                MonthTotal(YearMonth.of(2026, 10), income = 0, expense = 1_500_000),
            ),
            result,
        )
    }

    @Test
    fun bucketByMonthUsesLocalTimeAtMonthBoundary() {
        // 00:30 on 1 Oct in Jakarta is still 30 Sep in UTC; it must count for October.
        val points = listOf(AmountPoint(at(2026, 10, 1, 0) + 30 * 60_000, TransactionType.EXPENSE, 10_000))
        val result = bucketByMonth(points, listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 10)), jakarta)
        assertEquals(0L, result[0].expense)
        assertEquals(10_000L, result[1].expense)
    }

    @Test
    fun balanceSummarySumsPerWalletType() {
        fun wallet(id: Long, type: WalletType, balance: Long) =
            WalletWithBalance(WalletEntity(id, "w$id", type, initialBalance = 0, createdAt = 0), balance)

        val summary = BalanceSummary.from(
            listOf(
                wallet(1, WalletType.CASH, 350_000),
                wallet(2, WalletType.CARD, 2_150_000),
                wallet(3, WalletType.EWALLET, 125_500),
                wallet(4, WalletType.EWALLET, 0),
            )
        )

        assertEquals(BalanceSummary(total = 2_625_500, cash = 350_000, card = 2_150_000, ewallet = 125_500), summary)
    }
}
