package Sakuin.Project.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun formatsThousands() {
        assertEquals("0", formatThousands(0))
        assertEquals("999", formatThousands(999))
        assertEquals("1.000", formatThousands(1_000))
        assertEquals("2.625.500", formatThousands(2_625_500))
        assertEquals("-50.000", formatThousands(-50_000))
    }

    @Test
    fun formatsRupiah() {
        assertEquals("Rp 18.000", formatRupiah(18_000))
        assertEquals("−Rp 50.000", formatRupiah(-50_000))
    }

    @Test
    fun formatsCompact() {
        assertEquals("Rp 850", formatRupiahCompact(850))
        assertEquals("Rp 125rb", formatRupiahCompact(125_000))
        assertEquals("Rp 54,5rb", formatRupiahCompact(54_500))
        assertEquals("Rp 1,87jt", formatRupiahCompact(1_874_500))
        assertEquals("Rp 5jt", formatRupiahCompact(5_000_000))
        assertEquals("Rp 12,5jt", formatRupiahCompact(12_480_000))
        assertEquals("Rp 2,5M", formatRupiahCompact(2_500_000_000))
        assertEquals("−Rp 1,2jt", formatRupiahCompact(-1_200_000))
    }

    @Test
    fun parsesInput() {
        assertEquals(1_250_000L, parseRupiahInput("Rp 1.250.000"))
        assertEquals(0L, parseRupiahInput("000"))
        assertNull(parseRupiahInput(""))
        assertNull(parseRupiahInput("Rp"))
        assertNull(parseRupiahInput("1234567890123"))
    }
}
