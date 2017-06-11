import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class TestBoxes {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testByte() {
        val x = java.lang.Byte.valueOf(100.toByte()) as java.lang.Byte
        val data = serializer.serialize(java.lang.Byte::class.java, x)
        val readX = serializer.deserialize(java.lang.Byte::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testShort() {
        val x = java.lang.Short.valueOf(10_000.toShort()) as java.lang.Short
        val data = serializer.serialize(java.lang.Short::class.java, x)
        val readX = serializer.deserialize(java.lang.Short::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testInt() {
        val x = java.lang.Integer.valueOf(1000_000_000) as java.lang.Integer
        val data = serializer.serialize(java.lang.Integer::class.java, x)
        val readX = serializer.deserialize(java.lang.Integer::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testLong() {
        val x = java.lang.Long.valueOf(10_000_000_000) as java.lang.Long
        val data = serializer.serialize(java.lang.Long::class.java, x)
        val readX = serializer.deserialize(java.lang.Long::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testBoolean() {
        val x = java.lang.Boolean.valueOf(true) as java.lang.Boolean
        val data = serializer.serialize(java.lang.Boolean::class.java, x)
        val readX = serializer.deserialize(java.lang.Boolean::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testChar() {
        val x = java.lang.Character.valueOf('z') as java.lang.Character
        val data = serializer.serialize(java.lang.Character::class.java, x)
        val readX = serializer.deserialize(java.lang.Character::class.java, data)
        assertEquals(x, readX)
    }

    @Test
    fun testFloat() {
        val x = java.lang.Float.valueOf(3.14159f) as java.lang.Float
        val data = serializer.serialize(java.lang.Float::class.java, x)
        val readX = serializer.deserialize(java.lang.Float::class.java, data)
        assertEquals(x.toFloat(), readX.toFloat(), 1e-8f)
    }

    @Test
    fun testDouble() {
        val x = java.lang.Double.valueOf(2.718281828459045) as java.lang.Double
        val data = serializer.serialize(java.lang.Double::class.java, x)
        val readX = serializer.deserialize(java.lang.Double::class.java, data)
        assertEquals(x.toDouble(), readX.toDouble(), 1e-10)
    }
}