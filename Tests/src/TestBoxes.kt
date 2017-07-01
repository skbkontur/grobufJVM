import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "RemoveRedundantCallsOfConversionMethods")
class TestBoxes {
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
    private val i = 1000_000_000
    private val l = 10_000_000_000
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

    private fun Byte.boxed() = java.lang.Byte.valueOf(this) as java.lang.Byte
    private fun Short.boxed() = java.lang.Short.valueOf(this) as java.lang.Short
    private fun Int.boxed() = java.lang.Integer.valueOf(this) as java.lang.Integer
    private fun Long.boxed() = java.lang.Long.valueOf(this) as java.lang.Long
    private fun Boolean.boxed() = java.lang.Boolean.valueOf(this) as java.lang.Boolean
    private fun Char.boxed() = java.lang.Character.valueOf(this) as java.lang.Character
    private fun Float.boxed() = java.lang.Float.valueOf(this) as java.lang.Float
    private fun Double.boxed() = java.lang.Double.valueOf(this) as java.lang.Double

    @Test
    fun testByte() {
        doTest(b.boxed(), b.toByte().boxed(), java.lang.Byte::class.java     , java.lang.Byte::class.java)
        doTest(s.boxed(), s.toByte().boxed(), java.lang.Short::class.java    , java.lang.Byte::class.java)
        doTest(i.boxed(), i.toByte().boxed(), java.lang.Integer::class.java  , java.lang.Byte::class.java)
        doTest(l.boxed(), l.toByte().boxed(), java.lang.Long::class.java     , java.lang.Byte::class.java)
        doTest(z.boxed(), z.toByte().boxed(), java.lang.Boolean::class.java  , java.lang.Byte::class.java)
        doTest(c.boxed(), c.toByte().boxed(), java.lang.Character::class.java, java.lang.Byte::class.java)
        doTest(f.boxed(), f.toByte().boxed(), java.lang.Float::class.java    , java.lang.Byte::class.java)
        doTest(d.boxed(), d.toByte().boxed(), java.lang.Double::class.java   , java.lang.Byte::class.java)
    }

    @Test
    fun testShort() {
        doTest(b.boxed(), b.toShort().boxed(), java.lang.Byte::class.java     , java.lang.Short::class.java)
        doTest(s.boxed(), s.toShort().boxed(), java.lang.Short::class.java    , java.lang.Short::class.java)
        doTest(i.boxed(), i.toShort().boxed(), java.lang.Integer::class.java  , java.lang.Short::class.java)
        doTest(l.boxed(), l.toShort().boxed(), java.lang.Long::class.java     , java.lang.Short::class.java)
        doTest(z.boxed(), z.toShort().boxed(), java.lang.Boolean::class.java  , java.lang.Short::class.java)
        doTest(c.boxed(), c.toShort().boxed(), java.lang.Character::class.java, java.lang.Short::class.java)
        doTest(f.boxed(), f.toShort().boxed(), java.lang.Float::class.java    , java.lang.Short::class.java)
        doTest(d.boxed(), d.toShort().boxed(), java.lang.Double::class.java   , java.lang.Short::class.java)
    }

    @Test
    fun testInt() {
        doTest(b.boxed(), b.toInt().boxed(), java.lang.Byte::class.java     , java.lang.Integer::class.java)
        doTest(s.boxed(), s.toInt().boxed(), java.lang.Short::class.java    , java.lang.Integer::class.java)
        doTest(i.boxed(), i.toInt().boxed(), java.lang.Integer::class.java  , java.lang.Integer::class.java)
        doTest(l.boxed(), l.toInt().boxed(), java.lang.Long::class.java     , java.lang.Integer::class.java)
        doTest(z.boxed(), z.toInt().boxed(), java.lang.Boolean::class.java  , java.lang.Integer::class.java)
        doTest(c.boxed(), c.toInt().boxed(), java.lang.Character::class.java, java.lang.Integer::class.java)
        doTest(f.boxed(), f.toInt().boxed(), java.lang.Float::class.java    , java.lang.Integer::class.java)
        doTest(d.boxed(), d.toInt().boxed(), java.lang.Double::class.java   , java.lang.Integer::class.java)
    }

    @Test
    fun testLong() {
        doTest(b.boxed(), b.toLong().boxed(), java.lang.Byte::class.java     , java.lang.Long::class.java)
        doTest(s.boxed(), s.toLong().boxed(), java.lang.Short::class.java    , java.lang.Long::class.java)
        doTest(i.boxed(), i.toLong().boxed(), java.lang.Integer::class.java  , java.lang.Long::class.java)
        doTest(l.boxed(), l.toLong().boxed(), java.lang.Long::class.java     , java.lang.Long::class.java)
        doTest(z.boxed(), z.toLong().boxed(), java.lang.Boolean::class.java  , java.lang.Long::class.java)
        doTest(c.boxed(), c.toLong().boxed(), java.lang.Character::class.java, java.lang.Long::class.java)
        doTest(f.boxed(), f.toLong().boxed(), java.lang.Float::class.java    , java.lang.Long::class.java)
        doTest(d.boxed(), d.toLong().boxed(), java.lang.Double::class.java   , java.lang.Long::class.java)
    }

    @Test
    fun testBoolean() {
        doTest(b.boxed(), true.boxed(), java.lang.Byte::class.java     , java.lang.Boolean::class.java)
        doTest(s.boxed(), true.boxed(), java.lang.Short::class.java    , java.lang.Boolean::class.java)
        doTest(i.boxed(), true.boxed(), java.lang.Integer::class.java  , java.lang.Boolean::class.java)
        doTest(l.boxed(), true.boxed(), java.lang.Long::class.java     , java.lang.Boolean::class.java)
        doTest(z.boxed(), true.boxed(), java.lang.Boolean::class.java  , java.lang.Boolean::class.java)
        doTest(c.boxed(), true.boxed(), java.lang.Character::class.java, java.lang.Boolean::class.java)
        doTest(f.boxed(), true.boxed(), java.lang.Float::class.java    , java.lang.Boolean::class.java)
        doTest(d.boxed(), true.boxed(), java.lang.Double::class.java   , java.lang.Boolean::class.java)

        doTest(0.toByte() .boxed(), false.boxed(), java.lang.Byte::class.java     , java.lang.Boolean::class.java)
        doTest(0.toShort().boxed(), false.boxed(), java.lang.Short::class.java    , java.lang.Boolean::class.java)
        doTest(0          .boxed(), false.boxed(), java.lang.Integer::class.java  , java.lang.Boolean::class.java)
        doTest(0L         .boxed(), false.boxed(), java.lang.Long::class.java     , java.lang.Boolean::class.java)
        doTest(false      .boxed(), false.boxed(), java.lang.Boolean::class.java  , java.lang.Boolean::class.java)
        doTest(0.toChar() .boxed(), false.boxed(), java.lang.Character::class.java, java.lang.Boolean::class.java)
        doTest(0.0f       .boxed(), false.boxed(), java.lang.Float::class.java    , java.lang.Boolean::class.java)
        doTest(0.0        .boxed(), false.boxed(), java.lang.Double::class.java   , java.lang.Boolean::class.java)
    }

    @Test
    fun testChar() {
        doTest(b.boxed(), b.toChar().boxed(), java.lang.Byte::class.java     , java.lang.Character::class.java)
        doTest(s.boxed(), s.toChar().boxed(), java.lang.Short::class.java    , java.lang.Character::class.java)
        doTest(i.boxed(), i.toChar().boxed(), java.lang.Integer::class.java  , java.lang.Character::class.java)
        doTest(l.boxed(), l.toChar().boxed(), java.lang.Long::class.java     , java.lang.Character::class.java)
        doTest(z.boxed(), z.toChar().boxed(), java.lang.Boolean::class.java  , java.lang.Character::class.java)
        doTest(c.boxed(), c.toChar().boxed(), java.lang.Character::class.java, java.lang.Character::class.java)
        doTest(f.boxed(), f.toChar().boxed(), java.lang.Float::class.java    , java.lang.Character::class.java)
        doTest(d.boxed(), d.toChar().boxed(), java.lang.Double::class.java   , java.lang.Character::class.java)
    }

    @Test
    fun testFloat() {
        doTest(b.boxed(), b.toFloat().boxed(), java.lang.Byte::class.java     , java.lang.Float::class.java)
        doTest(s.boxed(), s.toFloat().boxed(), java.lang.Short::class.java    , java.lang.Float::class.java)
        doTest(i.boxed(), i.toFloat().boxed(), java.lang.Integer::class.java  , java.lang.Float::class.java)
        doTest(l.boxed(), l.toFloat().boxed(), java.lang.Long::class.java     , java.lang.Float::class.java)
        doTest(z.boxed(), z.toFloat().boxed(), java.lang.Boolean::class.java  , java.lang.Float::class.java)
        doTest(c.boxed(), c.toFloat().boxed(), java.lang.Character::class.java, java.lang.Float::class.java)
        doTest(f.boxed(), f.toFloat().boxed(), java.lang.Float::class.java    , java.lang.Float::class.java)
        doTest(d.boxed(), d.toFloat().boxed(), java.lang.Double::class.java   , java.lang.Float::class.java)
    }

    @Test
    fun testDouble() {
        doTest(b.boxed(), b.toDouble().boxed(), java.lang.Byte::class.java     , java.lang.Double::class.java)
        doTest(s.boxed(), s.toDouble().boxed(), java.lang.Short::class.java    , java.lang.Double::class.java)
        doTest(i.boxed(), i.toDouble().boxed(), java.lang.Integer::class.java  , java.lang.Double::class.java)
        doTest(l.boxed(), l.toDouble().boxed(), java.lang.Long::class.java     , java.lang.Double::class.java)
        doTest(z.boxed(), z.toDouble().boxed(), java.lang.Boolean::class.java  , java.lang.Double::class.java)
        doTest(c.boxed(), c.toDouble().boxed(), java.lang.Character::class.java, java.lang.Double::class.java)
        doTest(f.boxed(), f.toDouble().boxed(), java.lang.Float::class.java    , java.lang.Double::class.java)
        doTest(d.boxed(), d.toDouble().boxed(), java.lang.Double::class.java   , java.lang.Double::class.java)
    }
}