package Sakuin.Project.util

/** "2625500" → "2.625.500" (Indonesian thousands separator). */
fun formatThousands(amount: Long): String {
    val digits = kotlin.math.abs(amount).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return if (amount < 0) "-$grouped" else grouped
}

/** "Rp 2.625.500"; negative amounts render as "−Rp 50.000". */
fun formatRupiah(amount: Long): String =
    if (amount < 0) "−Rp ${formatThousands(-amount)}" else "Rp ${formatThousands(amount)}"

/**
 * Short form for charts and tight cards, Indonesian style: "Rp 850", "Rp 125rb", "Rp 1,87jt", "Rp 2,5M".
 * Keeps up to two decimals (one from ten units up) and drops trailing zeros.
 */
fun formatRupiahCompact(amount: Long): String {
    val sign = if (amount < 0) "−" else ""
    val value = kotlin.math.abs(amount).toDouble()
    val (scaled, suffix) = when {
        value >= 1e12 -> value / 1e12 to "T"
        value >= 1e9 -> value / 1e9 to "M"
        value >= 1e6 -> value / 1e6 to "jt"
        value >= 1e3 -> value / 1e3 to "rb"
        else -> return "${sign}Rp ${value.toLong()}"
    }
    val decimals = if (scaled >= 10) 1 else 2
    val text = java.math.BigDecimal(scaled)
        .setScale(decimals, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
        .replace('.', ',')
    return "${sign}Rp $text$suffix"
}

/** Keeps only digits and parses them, so pasted "Rp 1.250.000" becomes 1250000. Null when empty or too long. */
fun parseRupiahInput(text: String): Long? {
    val digits = text.filter(Char::isDigit).trimStart('0')
    if (digits.isEmpty()) return if (text.any(Char::isDigit)) 0 else null
    return digits.takeIf { it.length <= MAX_AMOUNT_DIGITS }?.toLong()
}

/** Up to 999.999.999.999 (just under a trillion rupiah). */
const val MAX_AMOUNT_DIGITS = 12
