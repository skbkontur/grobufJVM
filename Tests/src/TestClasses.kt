import grobuf.GroboMember
import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class A(@JvmField var x: Int, @JvmField var y: Int)
class A2(@JvmField var y: Int, @JvmField var x: Int)
class A3(@JvmField var y: Int)
class A4(@JvmField var x: Int, @JvmField var z: Int, @JvmField var y: Int)

class Tree(@JvmField var id: Int, @JvmField var left: Tree?, @JvmField var right: Tree?)

class X(@JvmField var id: Int, @JvmField var y: Y?)
class Y(@JvmField var c: Char, @JvmField var x: X?)

class G1<T>(@JvmField var x: T)

open class G2<T1, T2>(@JvmField var y1: T1, @JvmField var y2: T2)

class G2_(x: Int, y: Int, z: Int): G2<Int, G1<A>>(x, G1(A(y, z)))

class B1(@JvmField @GroboMember(name = "Y") var x: Int)
class B2(@JvmField var y: Int)
class B3(@JvmField @GroboMember(id = 7483750539695275791) var z: Int)

class TestClasses {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testSimple() {
        val a = A(42, 117)
        val data = serializer.serialize(a)
        val readA = serializer.deserialize(data, A::class.java)
        assertEquals(a.x, readA.x)
        assertEquals(a.y, readA.y)
    }

    @Test
    fun testDifferentOrder() {
        val a = A(42, 117)
        val data = serializer.serialize(a)
        val readA2 = serializer.deserialize(data, A2::class.java)
        assertEquals(a.x, readA2.x)
        assertEquals(a.y, readA2.y)
    }

    @Test
    fun testFieldRemoved() {
        val a = A(42, 117)
        val data = serializer.serialize(a)
        val readA3 = serializer.deserialize(data, A3::class.java)
        assertEquals(a.y, readA3.y)
    }

    @Test
    fun testFieldAdded() {
        val a = A(42, 117)
        val data = serializer.serialize(a)
        val readA4 = serializer.deserialize(data, A4::class.java)
        assertEquals(a.x, readA4.x)
        assertEquals(a.y, readA4.y)
    }

    @Test
    fun testTypeLoop() {
        val tree = Tree(1, Tree(2, null, null), null)
        val data = serializer.serialize(tree)
        val readTree = serializer.deserialize(data, Tree::class.java)
        assertEquals(1, readTree.id)
        assertNotNull(readTree.left)
        assertEquals(2, readTree.left!!.id)
        assertNull(readTree.right)
        assertNull(readTree.left!!.left)
        assertNull(readTree.left!!.right)
    }

    @Test
    fun testTypeCycle() {
        val x = X(1, Y('z', X(2, null)))
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, X::class.java)
        assertEquals(1, readX.id)
        assertNotNull(readX.y)
        assertEquals('z', readX.y!!.c)
        assertNotNull(readX.y!!.x)
        assertEquals(2, readX.y!!.x!!.id)
        assertNull(readX.y!!.x!!.y)
    }

    @Test
    fun testGeneric1() {
        val x = G2_(42, 117, -1)
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, G2_::class.java)
        assertEquals(42, readX.y1)
        assertNotNull(readX.y2)
        assertNotNull(readX.y2.x)
        assertEquals(117, readX.y2.x.x)
        assertEquals(-1, readX.y2.x.y)
    }

    @Test
    fun testGeneric2() {
        val x = G2(42, A(117, -1))
        val data = serializer.serialize(x, G2::class.java, java.lang.Integer::class.java, A::class.java)
        val readX = serializer.deserialize(data, G2::class.java, java.lang.Integer::class.java, A::class.java) as G2<Int, A>
        assertEquals(42, readX.y1)
        assertNotNull(readX.y2)
        assertEquals(117, readX.y2.x)
        assertEquals(-1, readX.y2.y)
    }

    @Test
    fun testGroboMember1() {
        val x = B1(42)
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, B2::class.java)
        assertEquals(42, readX.y)
    }

    @Test
    fun testGroboMember2() {
        val x = B3(42)
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, B2::class.java)
        assertEquals(42, readX.y)
    }
}