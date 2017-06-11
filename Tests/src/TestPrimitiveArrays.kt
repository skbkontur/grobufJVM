import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Test

class TestPrimitiveArrays {
    val serializer: Serializer = SerializerImpl()

    @Test
    fun testByteArray() {
        val x = byteArrayOf(-1, 2, 100)
        val data = serializer.serialize(ByteArray::class.java, x)
        val readX = serializer.deserialize(ByteArray::class.java, data)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testShortArray() {
        val x = shortArrayOf(-1, 2, 10_000)
        val data = serializer.serialize(ShortArray::class.java, x)
        val readX = serializer.deserialize(ShortArray::class.java, data)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testIntArray() {
        val x = intArrayOf(-1, 2, 1000_000_000)
        val data = serializer.serialize(IntArray::class.java, x)
        val readX = serializer.deserialize(IntArray::class.java, data)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testLongArray() {
        val x = longArrayOf(-1, 2, 10_000_000_000)
        val data = serializer.serialize(LongArray::class.java, x)
        val readX = serializer.deserialize(LongArray::class.java, data)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testBooleanArray() {
        val x = booleanArrayOf(true, false, false)
        val data = serializer.serialize(BooleanArray::class.java, x)
        val readX = serializer.deserialize(BooleanArray::class.java, data)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testCharArray() {
        val x = charArrayOf('z', 'a', 'b')
        val data = serializer.serialize(CharArray::class.java, x)
        val readX = serializer.deserialize(CharArray::class.java, data)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i])
    }

    @Test
    fun testFloatArray() {
        val x = floatArrayOf(-1.0f, 0.0f, 3.14159f)
        val data = serializer.serialize(FloatArray::class.java, x)
        val readX = serializer.deserialize(FloatArray::class.java, data)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i], 1e-8f)
    }

    @Test
    fun testDoubleArray() {
        val x = doubleArrayOf(-1.0, 0.0, 2.718281828459045)
        val data = serializer.serialize(DoubleArray::class.java, x)
        val readX = serializer.deserialize(DoubleArray::class.java, data)
        assertEquals(x.size, readX.size)
        for (i in x.indices)
            assertEquals(x[i], readX[i], 1e-10)
    }
}