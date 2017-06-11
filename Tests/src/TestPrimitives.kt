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
        val data = serializer.serialize(Byte::class.java, x)
        val readX = serializer.deserialize(Byte::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testShort() {
        val x = 10_000.toShort()
        val data = serializer.serialize(Short::class.java, x)
        val readX = serializer.deserialize(Short::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testInt() {
        val x = 1000_000_000
        val data = serializer.serialize(Int::class.java, x)
        val readX = serializer.deserialize(Int::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testLong() {
        val x = 10_000_000_000
        val data = serializer.serialize(Long::class.java, x)
        val readX = serializer.deserialize(Long::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testBoolean() {
        val x = true
        val data = serializer.serialize(Boolean::class.java, x)
        val readX = serializer.deserialize(Boolean::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testChar() {
        val x = 'z'
        val data = serializer.serialize(Char::class.java, x)
        val readX = serializer.deserialize(Char::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testFloat() {
        val x = 3.14159f
        val data = serializer.serialize(Float::class.java, x)
        val readX = serializer.deserialize(Float::class.java, data)
        assertEquals(x, readX, 1e-8f)
    }

    @Test
    fun testDouble() {
        val x = 2.718281828459045
        val data = serializer.serialize(Double::class.java, x)
        val readX = serializer.deserialize(Double::class.java, data)
        assertEquals(x, readX, 1e-10)
    }
}