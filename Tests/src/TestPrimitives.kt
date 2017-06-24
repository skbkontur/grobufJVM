import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TestPrimitives {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testByte() {
        val x = 100.toByte()
        val data = serializer.serialize(x, Byte::class.java)
        val readX = serializer.deserialize(data, Byte::class.java)
        assertEquals(x, readX)
    }

    @Test
    fun testShort() {
        val x = 10_000.toShort()
        val data = serializer.serialize(x, Short::class.java)
        val readX = serializer.deserialize(data, Short::class.java)
        assertEquals(x, readX)
    }

    @Test
    fun testInt() {
        val x = 1000_000_000
        val data = serializer.serialize(x, Int::class.java)
        val readX = serializer.deserialize(data, Int::class.java)
        assertEquals(x, readX)
    }

    @Test
    fun testLong() {
        val x = 10_000_000_000
        val data = serializer.serialize(x, Long::class.java)
        val readX = serializer.deserialize(data, Long::class.java)
        assertEquals(x, readX)
    }

    @Test
    fun testBoolean() {
        val x = true
        val data = serializer.serialize(x, Boolean::class.java)
        val readX = serializer.deserialize(data, Boolean::class.java)
        assertEquals(x, readX)
    }

    @Test
    fun testChar() {
        val x = 'z'
        val data = serializer.serialize(x, Char::class.java)
        val readX = serializer.deserialize(data, Char::class.java)
        assertEquals(x, readX)
    }

    @Test
    fun testFloat() {
        val x = 3.14159f
        val data = serializer.serialize(x, Float::class.java)
        val readX = serializer.deserialize(data, Float::class.java)
        assertEquals(x, readX, 1e-8f)
    }

    @Test
    fun testDouble() {
        val x = 2.718281828459045
        val data = serializer.serialize(x, Double::class.java)
        val readX = serializer.deserialize(data, Double::class.java)
        assertEquals(x, readX, 1e-10)
    }
}