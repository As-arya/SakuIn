package Sakuin.Project.data.remote

import Sakuin.Project.data.local.entity.CategoryEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.serialization.Serializable

/** Why a scan did not produce a usable result; each maps to its own message on the scan screen. */
enum class ScanError {
    /** No API key in local.properties, or the key was rejected. */
    NotConfigured,
    /** The photo could not be opened or decoded. */
    Image,
    Network,
    Timeout,
    /** Rate limit or quota reached (HTTP 429). */
    Busy,
    /** Gemini failed on its side (5xx). */
    Server,
    /** The photo is not a receipt. */
    NotReceipt,
    /** A receipt, but nothing usable came back (empty, blocked or malformed answer). */
    Unreadable,
}

/** [detail] is for logs only; the screen shows a message for [error]. */
class ScanException(val error: ScanError, detail: String? = null, cause: Throwable? = null) :
    Exception("$error${detail?.let { ": $it" }.orEmpty()}", cause)

/** One line on the receipt; [price] is the line total in rupiah. */
@Serializable
data class ReceiptItemDto(
    val name: String = "",
    val quantity: Int? = null,
    val price: Long? = null,
)

/** The JSON Gemini returns, shaped by [GeminiReceiptClient]'s response schema. */
@Serializable
data class ReceiptDto(
    val isReceipt: Boolean = true,
    val merchant: String? = null,
    val total: Long? = null,
    /** yyyy-MM-dd as printed on the receipt. */
    val date: String? = null,
    /** HH:mm (24h) as printed on the receipt. */
    val time: String? = null,
    /** One of the category names sent with the request. */
    val category: String? = null,
    val suggestedName: String? = null,
    val items: List<ReceiptItemDto> = emptyList(),
)

data class ReceiptItem(val name: String, val quantity: Int, val price: Long?)

/** A scan result ready for the review screen: everything here can still be changed by the user. */
data class ReceiptDraft(
    val amount: Long?,
    val title: String,
    val merchant: String?,
    val categoryId: Long?,
    val occurredAt: Long,
    val items: List<ReceiptItem>,
)

private const val MAX_TITLE = 60

/**
 * Turns Gemini's answer into a [ReceiptDraft]: matches the category by name (falling back to
 * "Others"), uses the item sum when no total was read, and never dates an expense in the future.
 */
fun ReceiptDto.toDraft(
    categories: List<CategoryEntity>,
    now: LocalDateTime,
    zone: ZoneId,
): ReceiptDraft {
    val cleanItems = items
        .filter { it.name.isNotBlank() }
        .map { ReceiptItem(it.name.trim(), (it.quantity ?: 1).coerceAtLeast(1), it.price?.takeIf { p -> p > 0 }) }
    val itemSum = cleanItems.mapNotNull { it.price }.sum().takeIf { it > 0 }
    val cleanMerchant = merchant?.trim()?.takeIf { it.isNotEmpty() }
    val category = categories.firstOrNull { it.name.equals(this.category?.trim(), ignoreCase = true) }
        ?: categories.firstOrNull { it.iconKey == "more" }
        ?: categories.firstOrNull()
    return ReceiptDraft(
        amount = total?.takeIf { it > 0 } ?: itemSum,
        title = (suggestedName?.trim()?.takeIf { it.isNotEmpty() } ?: cleanMerchant.orEmpty()).take(MAX_TITLE),
        merchant = cleanMerchant,
        categoryId = category?.id,
        occurredAt = receiptDateTime(date, time, now).atZone(zone).toInstant().toEpochMilli(),
        items = cleanItems,
    )
}

/** The receipt's date and time when they are readable and not in the future; otherwise [now]. */
internal fun receiptDateTime(date: String?, time: String?, now: LocalDateTime): LocalDateTime {
    val day = date?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() } ?: return now
    val clock = time?.let { runCatching { LocalTime.parse(it.trim()) }.getOrNull() }
    val dateTime = when {
        clock != null -> day.atTime(clock)
        // Same day without a printed time: the receipt was most likely just paid.
        day == now.toLocalDate() -> now
        else -> day.atTime(12, 0)
    }
    return if (dateTime.isAfter(now)) now else dateTime
}
