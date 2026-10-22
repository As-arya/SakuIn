package Sakuin.Project.data.remote

import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.local.entity.CategoryKind
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptDraftTest {
    private val zone = ZoneOffset.ofHours(7)
    private val now = LocalDateTime.of(2026, 10, 22, 12, 28)
    private val categories = listOf(
        CategoryEntity(id = 1, name = "Food", kind = CategoryKind.EXPENSE, iconKey = "utensils"),
        CategoryEntity(id = 2, name = "Shopping", kind = CategoryKind.EXPENSE, iconKey = "bag"),
        CategoryEntity(id = 8, name = "Others", kind = CategoryKind.EXPENSE, iconKey = "more"),
    )

    private fun millis(dateTime: LocalDateTime) = dateTime.atZone(zone).toInstant().toEpochMilli()

    @Test
    fun mapsAReadReceipt() {
        val draft = ReceiptDto(
            merchant = " Warung Bu Sri ",
            total = 43_000,
            date = "2026-10-21",
            time = "19:05",
            category = "food",
            suggestedName = "Nasi Goreng Spesial",
            items = listOf(
                ReceiptItemDto("Nasi Goreng Spesial", 1, 25_000),
                ReceiptItemDto("Es Teh Manis", 2, 10_000),
                ReceiptItemDto(" ", 1, 1),
            ),
        ).toDraft(categories, now, zone)

        assertEquals(43_000L, draft.amount)
        assertEquals("Nasi Goreng Spesial", draft.title)
        assertEquals("Warung Bu Sri", draft.merchant)
        assertEquals(1L, draft.categoryId)
        assertEquals(millis(LocalDateTime.of(2026, 10, 21, 19, 5)), draft.occurredAt)
        assertEquals(listOf(ReceiptItem("Nasi Goreng Spesial", 1, 25_000), ReceiptItem("Es Teh Manis", 2, 10_000)), draft.items)
    }

    @Test
    fun unknownCategoryFallsBackToOthers() {
        val draft = ReceiptDto(category = "Groceries").toDraft(categories, now, zone)
        assertEquals(8L, draft.categoryId)
    }

    @Test
    fun missingTotalUsesTheItemSum() {
        val draft = ReceiptDto(
            total = null,
            items = listOf(ReceiptItemDto("Kopi", 1, 18_000), ReceiptItemDto("Roti", 1, 12_000), ReceiptItemDto("Air", 1, null)),
        ).toDraft(categories, now, zone)
        assertEquals(30_000L, draft.amount)
    }

    @Test
    fun noAmountWhenNothingWasRead() {
        val draft = ReceiptDto(total = 0).toDraft(categories, now, zone)
        assertNull(draft.amount)
    }

    @Test
    fun titleFallsBackToTheMerchantAndIsShortened() {
        assertEquals("Indomaret", ReceiptDto(merchant = "Indomaret", suggestedName = "  ").toDraft(categories, now, zone).title)
        assertEquals(60, ReceiptDto(suggestedName = "x".repeat(80)).toDraft(categories, now, zone).title.length)
    }

    @Test
    fun receiptDatesNeverGoIntoTheFuture() {
        assertEquals(now, receiptDateTime("2026-10-23", "08:00", now))
        assertEquals(now, receiptDateTime("2026-10-22", "13:00", now))
        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 15), receiptDateTime("2026-10-22", "09:15", now))
    }

    @Test
    fun missingOrUnreadableDatesUseSensibleTimes() {
        assertEquals(now, receiptDateTime(null, "09:00", now))
        assertEquals(now, receiptDateTime("21/10/2026", null, now))
        // Same day without a time: just paid. An earlier day without a time: midday.
        assertEquals(now, receiptDateTime("2026-10-22", null, now))
        assertEquals(LocalDateTime.of(2026, 10, 20, 12, 0), receiptDateTime("2026-10-20", "late", now))
    }
}
