import grobuf.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class T1(@JvmField var t: Tuple1<Int>)
class T2(@JvmField var t: Tuple2<Int, String>)
class T3(@JvmField var t: Tuple3<Int, String, A>)
class T4(@JvmField var t: Tuple4<Int, String, A, Int>)
class T5(@JvmField var t: Tuple5<Int, String, A, Int, Int>)
class T6(@JvmField var t: Tuple6<Int, String, A, Int, Int, Int>)
class T7(@JvmField var t: Tuple7<Int, String, A, Int, Int, Int, Int>)
class T8(@JvmField var t: Tuple8<Int, String, A, Int, Int, Int, Int, Int>)

class MyTuple(@JvmField val x: Int, @JvmField val y: String) : Tuple()
open class G4<T>(@JvmField var t: Tuple2<Int, T>)
class G4_(x: Int, y: Int, z: Int) : G4<A>(Tuple2(x, A(y, z)))

class TestTuples {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testSimple() {
        val tuple = Tuple1(42)
        val data = serializer.serialize(tuple, Tuple1::class.java, java.lang.Integer::class.java)
        val readTuple = serializer.deserialize(data, Tuple1::class.java, java.lang.Integer::class.java)
        assertEquals(42, readTuple.item1)
    }

    @Test
    fun test1() {
        val tuple = T1(Tuple1(42))
        val data = serializer.serialize(tuple)
        val readTuple = serializer.deserialize(data, T1::class.java)
        assertEquals(42, readTuple.t.item1)
    }

    @Test
    fun test2() {
        val tuple = T2(Tuple2(42, "zzz"))
        val data = serializer.serialize(tuple)
        val readTuple = serializer.deserialize(data, T2::class.java)
        assertEquals(42, readTuple.t.item1)
        assertEquals("zzz", readTuple.t.item2)
    }

    @Test
    fun test3() {
        val tuple = T3(Tuple3(42, "zzz", A(1, 2)))
        val data = serializer.serialize(tuple)
        val readTuple = serializer.deserialize(data, T3::class.java)
        assertEquals(42, readTuple.t.item1)
        assertEquals("zzz", readTuple.t.item2)
        assertNotNull(readTuple.t.item3)
        assertEquals(1, readTuple.t.item3.x)
        assertEquals(2, readTuple.t.item3.y)
    }

    @Test
    fun test4() {
        val tuple = T4(Tuple4(42, "zzz", A(1, 2), 100))
        val data = serializer.serialize(tuple)
        val readTuple = serializer.deserialize(data, T4::class.java)
        assertEquals(42, readTuple.t.item1)
        assertEquals("zzz", readTuple.t.item2)
        assertNotNull(readTuple.t.item3)
        assertEquals(1, readTuple.t.item3.x)
        assertEquals(2, readTuple.t.item3.y)
        assertEquals(100, readTuple.t.item4)
    }

    @Test
    fun test5() {
        val tuple = T5(Tuple5(42, "zzz", A(1, 2), 100, 1000))
        val data = serializer.serialize(tuple)
        val readTuple = serializer.deserialize(data, T5::class.java)
        assertEquals(42, readTuple.t.item1)
        assertEquals("zzz", readTuple.t.item2)
        assertNotNull(readTuple.t.item3)
        assertEquals(1, readTuple.t.item3.x)
        assertEquals(2, readTuple.t.item3.y)
        assertEquals(100, readTuple.t.item4)
        assertEquals(1000, readTuple.t.item5)
    }

    @Test
    fun test6() {
        val tuple = T6(Tuple6(42, "zzz", A(1, 2), 100, 1000, 10000))
        val data = serializer.serialize(tuple)
        val readTuple = serializer.deserialize(data, T6::class.java)
        assertEquals(42, readTuple.t.item1)
        assertEquals("zzz", readTuple.t.item2)
        assertNotNull(readTuple.t.item3)
        assertEquals(1, readTuple.t.item3.x)
        assertEquals(2, readTuple.t.item3.y)
        assertEquals(100, readTuple.t.item4)
        assertEquals(1000, readTuple.t.item5)
        assertEquals(10000, readTuple.t.item6)
    }

    @Test
    fun test7() {
        val tuple = T7(Tuple7(42, "zzz", A(1, 2), 100, 1000, 10000, -100))
        val data = serializer.serialize(tuple)
        val readTuple = serializer.deserialize(data, T7::class.java)
        assertEquals(42, readTuple.t.item1)
        assertEquals("zzz", readTuple.t.item2)
        assertNotNull(readTuple.t.item3)
        assertEquals(1, readTuple.t.item3.x)
        assertEquals(2, readTuple.t.item3.y)
        assertEquals(100, readTuple.t.item4)
        assertEquals(1000, readTuple.t.item5)
        assertEquals(10000, readTuple.t.item6)
        assertEquals(-100, readTuple.t.item7)
    }

    @Test
    fun test8() {
        val tuple = T8(Tuple8(42, "zzz", A(1, 2), 100, 1000, 10000, -100, -1000))
        val data = serializer.serialize(tuple)
        val readTuple = serializer.deserialize(data, T8::class.java)
        assertEquals(42, readTuple.t.item1)
        assertEquals("zzz", readTuple.t.item2)
        assertNotNull(readTuple.t.item3)
        assertEquals(1, readTuple.t.item3.x)
        assertEquals(2, readTuple.t.item3.y)
        assertEquals(100, readTuple.t.item4)
        assertEquals(1000, readTuple.t.item5)
        assertEquals(10000, readTuple.t.item6)
        assertEquals(-100, readTuple.t.item7)
        assertEquals(-1000, readTuple.t.rest)
    }

    @Test
    fun testCustomTuple1() {
        val tuple = MyTuple(42, "zzz")
        val data = serializer.serialize(tuple)
        val readTuple = serializer.deserialize(data, MyTuple::class.java)
        assertEquals(42, readTuple.x)
        assertEquals("zzz", readTuple.y)
    }

    @Test
    fun testCustomTuple2() {
        val tuple = MyTuple(42, "zzz")
        val data = serializer.serialize(tuple)
        val readTuple = serializer.deserialize(data, Tuple2::class.java,
                java.lang.Integer::class.java, String::class.java)
        assertEquals(42, readTuple.item1)
        assertEquals("zzz", readTuple.item2)
    }

    @Test
    fun testGeneric() {
        val x = G4_(-1, 42, 117)
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, G4_::class.java)
        assertNotNull(readX.t)
        assertEquals(-1, readX.t.item1)
        assertNotNull(readX.t.item2)
        assertEquals(42, readX.t.item2.x)
        assertEquals(117, readX.t.item2.y)
    }
}