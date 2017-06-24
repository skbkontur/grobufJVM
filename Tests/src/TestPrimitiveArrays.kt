import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TestPrimitiveArrays {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testByteArray() {
        val x = byteArrayOf(-1, 2, 100)
        val data = serializer.serialize(x, ByteArray::class.java)
        val readX = serializer.deserialize(data, ByteArray::class.java)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testShortArray() {
        val x = shortArrayOf(-1, 2, 10_000)
        val data = serializer.serialize(x, ShortArray::class.java)
        val readX = serializer.deserialize(data, ShortArray::class.java)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testIntArray() {
        val x = intArrayOf(-1, 2, 1000_000_000)
        val data = serializer.serialize(x, IntArray::class.java)
        val readX = serializer.deserialize(data, IntArray::class.java)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testLongArray() {
        val x = longArrayOf(-1, 2, 10_000_000_000)
        val data = serializer.serialize(x, LongArray::class.java)
        val readX = serializer.deserialize(data, LongArray::class.java)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testBooleanArray() {
        val x = booleanArrayOf(true, false, false)
        val data = serializer.serialize(x, BooleanArray::class.java)
        val readX = serializer.deserialize(data, BooleanArray::class.java)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testCharArray() {
        val x = charArrayOf('z', 'a', 'b')
        val data = serializer.serialize(x, CharArray::class.java)
        val readX = serializer.deserialize(data, CharArray::class.java)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testFloatArray() {
        val x = floatArrayOf(-1.0f, 0.0f, 3.14159f)
        val data = serializer.serialize(x, FloatArray::class.java)
        val readX = serializer.deserialize(data, FloatArray::class.java)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i], 1e-8f)
    }

    @Test
    fun testDoubleArray() {
        val x = doubleArrayOf(-1.0, 0.0, 2.718281828459045)
        val data = serializer.serialize(x, DoubleArray::class.java)
        val readX = serializer.deserialize(data, DoubleArray::class.java)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i], 1e-10)
    }
}