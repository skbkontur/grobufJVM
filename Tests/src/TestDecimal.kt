import grobuf.Decimal
import org.junit.Test
import org.junit.Assert.assertEquals

class TestDecimal {

    @Test
    fun testMultiplication() {
        val x = Decimal.ONE
        assertEquals(Decimal.valueOf(10), x * 10)
        assertEquals(Decimal.valueOf(-10), x * -10)
    }

    @Test
    fun testAddition() {
        val x = Decimal.ONE
        assertEquals(Decimal.valueOf(11), x + 10)
        assertEquals(Decimal.valueOf(-9), x + -10)
    }

    @Test
    fun testSubtraction() {
        val x = Decimal.ONE
        assertEquals(Decimal.valueOf(-9), x - 10)
        assertEquals(Decimal.valueOf(11), x - -10)
    }

    @Test
    fun testDivision() {
        val x = Decimal.valueOf(100)
        assertEquals(Decimal.valueOf(10), x / 10)
        assertEquals(Decimal.valueOf(-10), x / -10)
    }

    @Test
    fun testFloatingPoint() {
        val x = Decimal.valueOf(3.141592653)
        assertEquals(Decimal.valueOf(3141592653), x * 1_000_000_000)
    }

    @Test
    fun testOverflow() {
        assertEquals(Decimal.MAX_VALUE, Decimal.MAX_VALUE + 1)
        assertEquals(Decimal.MAX_VALUE, Decimal.MAX_VALUE * 10)
        assertEquals(Decimal.MIN_VALUE, Decimal.MIN_VALUE - 1)
        assertEquals(Decimal.MIN_VALUE, Decimal.MIN_VALUE * 10)
    }

    @Test
    fun testUnderflow() {
        assertEquals(Decimal.ZERO, Decimal.NEAR_NEGATIVE_ZERO / 10)
        assertEquals(Decimal.ZERO, Decimal.NEAR_POSITIVE_ZERO / 10)
    }
}