import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.*
import org.junit.Test

class A(@JvmField var x: Int, @JvmField var y: Int)
class A2(@JvmField var y: Int, @JvmField var x: Int)
class A3(@JvmField var y: Int)
class A4(@JvmField var x: Int, @JvmField var z: Int, @JvmField var y: Int)

class Tree(@JvmField var id: Int, @JvmField var left: Tree?, @JvmField var right: Tree?)

class X(@JvmField var id: Int, @JvmField var y: Y?)
class Y(@JvmField var c: Char, @JvmField var x: X?)

class TestClasses {
    val serializer: Serializer = SerializerImpl()

    @Test
    fun testSimple() {
        val a = A(42, 117)
        val data = serializer.serialize(a)
        val readA = serializer.deserialize(A::class.java, data)
        assertEquals(a.x, readA.x)
        assertEquals(a.y, readA.y)
    }

    @Test
    fun testDifferentOrder() {
        val a = A(42, 117)
        val data = serializer.serialize(a)
        val readA2 = serializer.deserialize(A2::class.java, data)
        assertEquals(a.x, readA2.x)
        assertEquals(a.y, readA2.y)
    }

    @Test
    fun testFieldRemoved() {
        val a = A(42, 117)
        val data = serializer.serialize(a)
        val readA3 = serializer.deserialize(A3::class.java, data)
        assertEquals(a.y, readA3.y)
    }

    @Test
    fun testFieldAdded() {
        val a = A(42, 117)
        val data = serializer.serialize(a)
        val readA4 = serializer.deserialize(A4::class.java, data)
        assertEquals(a.x, readA4.x)
        assertEquals(a.y, readA4.y)
    }

    @Test
    fun testTypeLoop() {
        val tree = Tree(1, Tree(2, null, null), null)
        val data = serializer.serialize(tree)
        val readTree = serializer.deserialize(Tree::class.java, data)
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
        val readX = serializer.deserialize(X::class.java, data)
        assertEquals(1, readX.id)
        assertNotNull(readX.y)
        assertEquals('z', readX.y!!.c)
        assertNotNull(readX.y!!.x)
        assertEquals(2, readX.y!!.x!!.id)
        assertNull(readX.y!!.x!!.y)
    }
}