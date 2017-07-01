import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@Suppress("RemoveRedundantCallsOfConversionMethods")
class TestPrimitives {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    private fun <TFrom: Any, TTo: Any> doTest(x: TFrom, expected: TTo, klassFrom: Class<TFrom>, klassTo: Class<TTo>) {
        val data = serializer.serialize(x, klassFrom)
        val readX = serializer.deserialize(data, klassTo)
        when (expected) {
            is Float -> assertEquals(expected, readX as Float, 1e-8f)
            is Double -> assertEquals(expected, readX as Double, 1e-10)
            else -> assertEquals(expected, readX)
        }
    }

    private val b = 100.toByte()
    private val s = 10_000.toShort()
    private val i = 1_000_000_000
    private val l = 1_000_000_000_000
    private val z = true
    private val c = 'z'
    private val f = 3.14159f
    private val d = 2.718281828459045

    private fun Boolean.toByte()   = (if (this) 1 else 0).toByte()
    private fun Boolean.toShort()  = (if (this) 1 else 0).toShort()
    private fun Boolean.toInt()    = (if (this) 1 else 0).toInt()
    private fun Boolean.toLong()   = (if (this) 1 else 0).toLong()
    private fun Boolean.toChar()   = (if (this) 1 else 0).toChar()
    private fun Boolean.toFloat()  = (if (this) 1 else 0).toFloat()
    private fun Boolean.toDouble() = (if (this) 1 else 0).toDouble()

    @Test
    fun testByte() {
        doTest(b, b.toByte(), Byte::class.java   , Byte::class.java)
        doTest(s, s.toByte(), Short::class.java  , Byte::class.java)
        doTest(i, i.toByte(), Int::class.java    , Byte::class.java)
        doTest(l, l.toByte(), Long::class.java   , Byte::class.java)
        doTest(z, z.toByte(), Boolean::class.java, Byte::class.java)
        doTest(c, c.toByte(), Char::class.java   , Byte::class.java)
        doTest(f, f.toByte(), Float::class.java  , Byte::class.java)
        doTest(d, d.toByte(), Double::class.java , Byte::class.java)
    }

    @Test
    fun testShort() {
        doTest(b, b.toShort(), Byte::class.java   , Short::class.java)
        doTest(s, s.toShort(), Short::class.java  , Short::class.java)
        doTest(i, i.toShort(), Int::class.java    , Short::class.java)
        doTest(l, l.toShort(), Long::class.java   , Short::class.java)
        doTest(z, z.toShort(), Boolean::class.java, Short::class.java)
        doTest(c, c.toShort(), Char::class.java   , Short::class.java)
        doTest(f, f.toShort(), Float::class.java  , Short::class.java)
        doTest(d, d.toShort(), Double::class.java , Short::class.java)
    }

    @Test
    fun testInt() {
        doTest(b, b.toInt(), Byte::class.java   , Int::class.java)
        doTest(s, s.toInt(), Short::class.java  , Int::class.java)
        doTest(i, i.toInt(), Int::class.java    , Int::class.java)
        doTest(l, l.toInt(), Long::class.java   , Int::class.java)
        doTest(z, z.toInt(), Boolean::class.java, Int::class.java)
        doTest(c, c.toInt(), Char::class.java   , Int::class.java)
        doTest(f, f.toInt(), Float::class.java  , Int::class.java)
        doTest(d, d.toInt(), Double::class.java , Int::class.java)
    }

    @Test
    fun testLong() {
        doTest(b, b.toLong(), Byte::class.java   , Long::class.java)
        doTest(s, s.toLong(), Short::class.java  , Long::class.java)
        doTest(i, i.toLong(), Int::class.java    , Long::class.java)
        doTest(l, l.toLong(), Long::class.java   , Long::class.java)
        doTest(z, z.toLong(), Boolean::class.java, Long::class.java)
        doTest(c, c.toLong(), Char::class.java   , Long::class.java)
        doTest(f, f.toLong(), Float::class.java  , Long::class.java)
        doTest(d, d.toLong(), Double::class.java , Long::class.java)
    }

    @Test
    fun testBoolean() {
        doTest(b, true, Byte::class.java   , Boolean::class.java)
        doTest(s, true, Short::class.java  , Boolean::class.java)
        doTest(i, true, Int::class.java    , Boolean::class.java)
        doTest(l, true, Long::class.java   , Boolean::class.java)
        doTest(z, true, Boolean::class.java, Boolean::class.java)
        doTest(c, true, Char::class.java   , Boolean::class.java)
        doTest(f, true, Float::class.java  , Boolean::class.java)
        doTest(d, true, Double::class.java , Boolean::class.java)

        doTest(0.toByte() , false, Byte::class.java   , Boolean::class.java)
        doTest(0.toShort(), false, Short::class.java  , Boolean::class.java)
        doTest(0          , false, Int::class.java    , Boolean::class.java)
        doTest(0L         , false, Long::class.java   , Boolean::class.java)
        doTest(false      , false, Boolean::class.java, Boolean::class.java)
        doTest(0.toChar() , false, Char::class.java   , Boolean::class.java)
        doTest(0.0f       , false, Float::class.java  , Boolean::class.java)
        doTest(0.0        , false, Double::class.java , Boolean::class.java)
    }

    @Test
    fun testChar() {
        doTest(b, b.toChar(), Byte::class.java   , Char::class.java)
        doTest(s, s.toChar(), Short::class.java  , Char::class.java)
        doTest(i, i.toChar(), Int::class.java    , Char::class.java)
        doTest(l, l.toChar(), Long::class.java   , Char::class.java)
        doTest(z, z.toChar(), Boolean::class.java, Char::class.java)
        doTest(c, c.toChar(), Char::class.java   , Char::class.java)
        doTest(f, f.toChar(), Float::class.java  , Char::class.java)
        doTest(d, d.toChar(), Double::class.java , Char::class.java)
    }

    @Test
    fun testFloat() {
        doTest(b, b.toFloat(), Byte::class.java   , Float::class.java)
        doTest(s, s.toFloat(), Short::class.java  , Float::class.java)
        doTest(i, i.toFloat(), Int::class.java    , Float::class.java)
        doTest(l, l.toFloat(), Long::class.java   , Float::class.java)
        doTest(z, z.toFloat(), Boolean::class.java, Float::class.java)
        doTest(c, c.toFloat(), Char::class.java   , Float::class.java)
        doTest(f, f.toFloat(), Float::class.java  , Float::class.java)
        doTest(d, d.toFloat(), Double::class.java , Float::class.java)
    }

    @Test
    fun testDouble() {
        doTest(b, b.toDouble(), Byte::class.java   , Double::class.java)
        doTest(s, s.toDouble(), Short::class.java  , Double::class.java)
        doTest(i, i.toDouble(), Int::class.java    , Double::class.java)
        doTest(l, l.toDouble(), Long::class.java   , Double::class.java)
        doTest(z, z.toDouble(), Boolean::class.java, Double::class.java)
        doTest(c, c.toDouble(), Char::class.java   , Double::class.java)
        doTest(f, f.toDouble(), Float::class.java  , Double::class.java)
        doTest(d, d.toDouble(), Double::class.java , Double::class.java)
    }
}