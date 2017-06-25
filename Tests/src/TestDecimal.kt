import grobuf.Decimal
import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TestDecimal {

    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

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

    @Test
    fun testReadWrite() {
        val x = Decimal.valueOf(12345678987654321) / -1_000_000_000
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, Decimal::class.java)
        assertEquals(x, readX)
    }

    @Test
    fun testReadFromByte() {
        val data = serializer.serialize(100.toByte())
        val x = serializer.deserialize(data, Decimal::class.java)
        assertEquals(Decimal.valueOf(100), x)
    }

    @Test
    fun testReadFromShort() {
        val data = serializer.serialize(10_000.toShort())
        val x = serializer.deserialize(data, Decimal::class.java)
        assertEquals(Decimal.valueOf(10_000), x)
    }

    @Test
    fun testReadFromInt() {
        val data = serializer.serialize(1_000_000)
        val x = serializer.deserialize(data, Decimal::class.java)
        assertEquals(Decimal.valueOf(1_000_000), x)
    }

    @Test
    fun testReadFromLong() {
        val data = serializer.serialize(1_000_000_000_000)
        val x = serializer.deserialize(data, Decimal::class.java)
        assertEquals(Decimal.valueOf(1_000_000_000_000), x)
    }

    @Test
    fun testReadFromBoolean() {
        val data = serializer.serialize(true)
        val x = serializer.deserialize(data, Decimal::class.java)
        assertEquals(Decimal.ONE, x)
    }

    @Test
    fun testReadFromFloat() {
        val data = serializer.serialize(3.1415f)
        val x = serializer.deserialize(data, Decimal::class.java)
        assertEquals(Decimal.valueOf(3.1415f), x)
    }

    @Test
    fun testReadFromDouble() {
        val data = serializer.serialize(3.1415926535)
        val x = serializer.deserialize(data, Decimal::class.java)
        assertEquals(Decimal.valueOf(3.1415926535), x)
    }
}