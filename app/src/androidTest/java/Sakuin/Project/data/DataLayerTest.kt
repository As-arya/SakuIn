package Sakuin.Project.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import Sakuin.Project.data.local.DefaultCategories
import Sakuin.Project.data.local.SakuinDatabase
import Sakuin.Project.data.local.entity.CategoryKind
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.entity.WalletType
import Sakuin.Project.data.repository.BalanceSummary
import Sakuin.Project.data.repository.CategoryRepository
import Sakuin.Project.data.repository.TransactionFilter
import Sakuin.Project.data.repository.TransactionRepository
import Sakuin.Project.data.repository.WalletRepository
import Sakuin.Project.util.toTimeRange
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataLayerTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val october = YearMonth.of(2026, 10)

    private lateinit var db: SakuinDatabase
    private lateinit var wallets: WalletRepository
    private lateinit var categories: CategoryRepository
    private lateinit var transactions: TransactionRepository

    private fun at(m: Int, d: Int, h: Int = 12) =
        ZonedDateTime.of(2026, m, d, h, 0, 0, 0, zone).toInstant().toEpochMilli()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<SakuinDatabase>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        wallets = WalletRepository(db.walletDao(), clock = { at(10, 1) })
        categories = CategoryRepository(db.categoryDao())
        transactions = TransactionRepository(db.transactionDao(), clock = { at(10, 22) })
    }

    @After
    fun closeDb() = db.close()

    private suspend fun categoryId(kind: CategoryKind, name: String) =
        categories.observeCategories(kind).first().first { it.name == name }.id

    private suspend inline fun <reified T : Throwable> assertFails(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) return
            throw e
        }
        throw AssertionError("Expected ${T::class.simpleName}")
    }

    @Test
    fun defaultsAreSeededOnce() = runTest {
        categories.ensureDefaults()
        categories.ensureDefaults()

        val expense = categories.observeCategories(CategoryKind.EXPENSE).first()
        val income = categories.observeCategories(CategoryKind.INCOME).first()
        assertEquals(DefaultCategories.all.size, expense.size + income.size)
        assertEquals("Food", expense.first().name)
        assertEquals("Salary", income.first().name)
    }

    @Test
    fun balancesIncludeIncomeExpenseAndTransfers() = runTest {
        categories.ensureDefaults()
        val cash = wallets.addWallet("My Wallet", WalletType.CASH, initialBalance = 100_000)
        val bca = wallets.addWallet("BCA", WalletType.CARD, initialBalance = 2_000_000)
        val gopay = wallets.addWallet("GoPay", WalletType.EWALLET, initialBalance = 0)

        transactions.addIncome(500_000, bca, categoryId(CategoryKind.INCOME, "Salary"), occurredAt = at(10, 2))
        transactions.addExpense(18_000, cash, categoryId(CategoryKind.EXPENSE, "Food"), "Nasi Goreng", occurredAt = at(10, 3))
        transactions.addTransfer(200_000, fromWalletId = bca, toWalletId = cash, occurredAt = at(10, 4))
        transactions.addTransfer(50_000, fromWalletId = bca, toWalletId = gopay, occurredAt = at(10, 5))

        val balances = wallets.observeWallets().first().associate { it.wallet.id to it.balance }
        assertEquals(100_000L - 18_000 + 200_000, balances[cash])
        assertEquals(2_000_000L + 500_000 - 200_000 - 50_000, balances[bca])
        assertEquals(50_000L, balances[gopay])
        assertEquals(
            BalanceSummary(total = 2_582_000, cash = 282_000, card = 2_250_000, ewallet = 50_000),
            wallets.observeBalanceSummary().first(),
        )
    }

    @Test
    fun archivedWalletsAreHidden() = runTest {
        val cash = wallets.addWallet("My Wallet", WalletType.CASH, 0)
        val ovo = wallets.addWallet("OVO", WalletType.EWALLET, 0)
        wallets.archiveWallet(ovo)

        assertEquals(listOf(cash), wallets.observeWallets().first().map { it.wallet.id })
        assertTrue(wallets.hasWallets())
    }

    @Test
    fun walletWithTransactionsCannotBeDeleted() = runTest {
        categories.ensureDefaults()
        val cash = wallets.addWallet("My Wallet", WalletType.CASH, 0)
        transactions.addExpense(5_000, cash, categoryId(CategoryKind.EXPENSE, "Transport"), "Angkot")

        assertFails<IllegalStateException> { wallets.deleteWallet(cash) }
    }

    @Test
    fun totalsAndCategoryBreakdownIgnoreTransfersAndOtherMonths() = runTest {
        categories.ensureDefaults()
        val cash = wallets.addWallet("My Wallet", WalletType.CASH, 0)
        val bca = wallets.addWallet("BCA", WalletType.CARD, 0)
        val food = categoryId(CategoryKind.EXPENSE, "Food")
        val edu = categoryId(CategoryKind.EXPENSE, "Education")

        transactions.addIncome(4_500_000, bca, categoryId(CategoryKind.INCOME, "Salary"), occurredAt = at(10, 1))
        transactions.addExpense(18_000, cash, food, "Nasi Goreng", occurredAt = at(10, 3))
        transactions.addExpense(25_000, cash, food, "Nasi Padang", occurredAt = at(10, 4))
        transactions.addExpense(1_500_000, bca, edu, "Tuition fee", occurredAt = at(10, 5))
        transactions.addTransfer(200_000, bca, cash, occurredAt = at(10, 6))
        transactions.addExpense(99_000, cash, food, "Last month", occurredAt = at(9, 30))

        val range = october.toTimeRange(zone)
        val totals = transactions.observeTotals(range).first()
        assertEquals(4_500_000L, totals.income)
        assertEquals(1_543_000L, totals.expense)

        val byCategory = transactions.observeExpenseByCategory(range).first()
        assertEquals(listOf("Education" to 1_500_000L, "Food" to 43_000L), byCategory.map { it.categoryName to it.total })

        val cashOnly = transactions.observeTotals(range, walletId = cash).first()
        assertEquals(43_000L, cashOnly.expense)
    }

    @Test
    fun monthlyTotalsCoverRequestedMonths() = runTest {
        categories.ensureDefaults()
        val cash = wallets.addWallet("My Wallet", WalletType.CASH, 0)
        val food = categoryId(CategoryKind.EXPENSE, "Food")
        transactions.addExpense(10_000, cash, food, "A", occurredAt = at(9, 15))
        transactions.addExpense(20_000, cash, food, "B", occurredAt = at(10, 15))

        val months = transactions.observeMonthlyTotals(october, count = 3, zone = zone).first()
        assertEquals(listOf(YearMonth.of(2026, 8), YearMonth.of(2026, 9), october), months.map { it.month })
        assertEquals(listOf(0L, 10_000L, 20_000L), months.map { it.expense })
    }

    @Test
    fun listFiltersSearchAndDetails() = runTest {
        categories.ensureDefaults()
        val cash = wallets.addWallet("My Wallet", WalletType.CASH, 0)
        val bca = wallets.addWallet("BCA", WalletType.CARD, 0)
        val food = categoryId(CategoryKind.EXPENSE, "Food")
        transactions.addExpense(18_000, cash, food, "Nasi Goreng", occurredAt = at(10, 3))
        transactions.addExpense(5_000, cash, categoryId(CategoryKind.EXPENSE, "Transport"), "Angkot", occurredAt = at(10, 4))
        transactions.addTransfer(200_000, bca, cash, note = "ATM withdrawal", occurredAt = at(10, 5))

        val range = october.toTimeRange(zone)
        val all = transactions.observeTransactions(range).first()
        assertEquals(listOf("", "Angkot", "Nasi Goreng"), all.map { it.transaction.title })
        val transfer = all.first()
        assertEquals("BCA", transfer.walletName)
        assertEquals("My Wallet", transfer.toWalletName)
        assertNull(transfer.categoryName)

        val expenses = transactions.observeTransactions(range, TransactionFilter(type = TransactionType.EXPENSE)).first()
        assertEquals(2, expenses.size)
        val search = transactions.observeTransactions(range, TransactionFilter(query = "goreng")).first()
        assertEquals(listOf("Nasi Goreng"), search.map { it.transaction.title })
        val byNote = transactions.observeTransactions(range, TransactionFilter(query = "ATM")).first()
        assertEquals(listOf(TransactionType.TRANSFER), byNote.map { it.transaction.type })
        val bcaTouched = transactions.observeTransactions(range, TransactionFilter(walletId = bca)).first()
        assertEquals(1, bcaTouched.size)
    }

    @Test
    fun titleSuggestionsAreMostRecentFirst() = runTest {
        categories.ensureDefaults()
        val cash = wallets.addWallet("My Wallet", WalletType.CASH, 0)
        val food = categoryId(CategoryKind.EXPENSE, "Food")
        transactions.addExpense(18_000, cash, food, "Nasi Goreng", occurredAt = at(10, 1))
        transactions.addExpense(25_000, cash, food, "Nasi Padang", occurredAt = at(10, 2))
        transactions.addExpense(18_000, cash, food, "Nasi Goreng", occurredAt = at(10, 3))
        transactions.addExpense(8_000, cash, food, "Kopi", occurredAt = at(10, 4))

        assertEquals(listOf("Nasi Goreng", "Nasi Padang"), transactions.suggestTitles(food, "Nasi"))
    }

    @Test
    fun deleteAndRestore() = runTest {
        categories.ensureDefaults()
        val cash = wallets.addWallet("My Wallet", WalletType.CASH, 10_000)
        val id = transactions.addExpense(5_000, cash, categoryId(CategoryKind.EXPENSE, "Food"), "Kopi")

        val deleted = transactions.deleteTransaction(id)!!
        assertEquals(10_000L, wallets.observeWallets().first().single().balance)
        transactions.restoreTransaction(deleted)
        assertEquals(5_000L, wallets.observeWallets().first().single().balance)
    }

    @Test
    fun invalidTransactionsAreRejected() = runTest {
        categories.ensureDefaults()
        val cash = wallets.addWallet("My Wallet", WalletType.CASH, 0)
        val food = categoryId(CategoryKind.EXPENSE, "Food")

        assertFails<IllegalArgumentException> { transactions.addExpense(0, cash, food, "Free lunch") }
        assertFails<IllegalArgumentException> { transactions.addExpense(1_000, cash, food, " ") }
        assertFails<IllegalArgumentException> { transactions.addTransfer(1_000, cash, cash) }
    }
}
