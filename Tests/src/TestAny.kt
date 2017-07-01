import grobuf.Decimal
import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

class TestAny {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testByte() {
        val x: Byte = 100
        assertEquals(x, serializer.deserialize(serializer.serialize(x), Any::class.java))
    }

    @Test
    fun testShort() {
        val x: Short = 10_000
        assertEquals(x, serializer.deserialize(serializer.serialize(x), Any::class.java))
    }

    @Test
    fun testInt() {
        val x = 1_000_000_000
        assertEquals(x, serializer.deserialize(serializer.serialize(x), Any::class.java))
    }

    @Test
    fun testLong() {
        val x = 1_000_000_000_000
        assertEquals(x, serializer.deserialize(serializer.serialize(x), Any::class.java))
    }

    @Test
    fun testBoolean() {
        val x = true
        assertEquals(x, serializer.deserialize(serializer.serialize(x), Any::class.java))
    }

    @Test
    fun testFloat() {
        val x = 3.14159f
        assertEquals(x, serializer.deserialize(serializer.serialize(x), Any::class.java))
    }

    @Test
    fun testDouble() {
        val x = 3.1415926535
        assertEquals(x, serializer.deserialize(serializer.serialize(x), Any::class.java))
    }

    @Test
    fun testString() {
        val x = "zzz"
        assertEquals(x, serializer.deserialize(serializer.serialize(x), Any::class.java))
    }

    @Test
    fun testDecimal() {
        val x = Decimal.valueOf(1_000_000_000_000)
        assertEquals(x, serializer.deserialize(serializer.serialize(x), Any::class.java))
    }

    @Test
    fun testDate() {
        val x = Date(1_000_000)
        assertEquals(x, serializer.deserialize(serializer.serialize(x), Any::class.java))
    }

    @Test
    fun testGuid() {
        val x = UUID(12345678987654321, 98765432123456789)
        assertEquals(x, serializer.deserialize(serializer.serialize(x), Any::class.java))
    }

    @Test
    fun testByteArray() {
        val x = byteArrayOf(100, -100)
        val readX = serializer.deserialize(serializer.serialize(x), Any::class.java) as ByteArray
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testShortArray() {
        val x = shortArrayOf(10_000, -10_000)
        val readX = serializer.deserialize(serializer.serialize(x), Any::class.java) as ShortArray
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testIntArray() {
        val x = intArrayOf(1_000_000_000, -1_000_000_000)
        val readX = serializer.deserialize(serializer.serialize(x), Any::class.java) as IntArray
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testLongArray() {
        val x = longArrayOf(12345678987654321, -98765432123456789)
        val readX = serializer.deserialize(serializer.serialize(x), Any::class.java) as LongArray
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testBooleanArray() {
        val x = booleanArrayOf(true, false)
        val readX = serializer.deserialize(serializer.serialize(x), Any::class.java) as BooleanArray
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testFloatArray() {
        val x = floatArrayOf(3.14159f, 2.71828f)
        val readX = serializer.deserialize(serializer.serialize(x), Any::class.java) as FloatArray
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i], 1e-8f)
    }

    @Test
    fun testDoubleArray() {
        val x = doubleArrayOf(3.1415926535, 2.71828182846)
        val readX = serializer.deserialize(serializer.serialize(x), Any::class.java) as DoubleArray
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i], 1e-10)
    }

    @Test
    fun testArrayOfAny() {
        val x = arrayOf(100.toByte(), 10_000.toShort(), 123456789, 12345678987654321, true, null,
                Decimal.MINUS_ONE, Date(123456789), "qzz", UUID(12345678987654321, 98765432123456789))
        val data = serializer.serialize(x, Any::class.java)
        val readX = serializer.deserialize(data, Any::class.java) as Array<Any?>
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testMapOfAny() {
        val x = mapOf(100.toByte() to 10_000.toShort(), 123456789 to 12345678987654321, true to null,
                Decimal.MINUS_ONE to Date(123456789), "qzz" to UUID(12345678987654321, 98765432123456789))
        val data = serializer.serialize(x, Any::class.java)
        val readX = serializer.deserialize(data, Any::class.java) as LinkedHashMap<Any?, Any?>
        assertEquals(x.size, readX.size)
        x.forEach { key, value -> assertEquals(value, readX[key]) }
    }
}