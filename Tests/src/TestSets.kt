import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.TreeSet

open class G3_3<T>(@JvmField var set: HashSet<T>)
class G3_3_(set: HashSet<A>): G3_3<A>(set)

open class G3_4<T>(@JvmField var set: TreeSet<T>)
class G3_4_(set: TreeSet<A>): G3_4<A>(set)

class TestSets {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testHashSet() {
        val arr = hashSetOf(A(42, 117), null, A(-1, 1000_000_000))
        val data = serializer.serialize(arr, HashSet::class.java, A::class.java)
        @Suppress("UNCHECKED_CAST")
        val readArr = serializer.deserialize(data, HashSet::class.java, A::class.java) as HashSet<A?>
        assertEquals(arr.size, readArr.size)
        for (x in arr)
            assertTrue(readArr.contains(x))
    }

    @Test
    fun testWriteArrayReadHashSet() {
        val arr = arrayOf(A(42, 117), null, A(-1, 1000_000_000))
        val data = serializer.serialize(arr)
        @Suppress("UNCHECKED_CAST")
        val readArr = serializer.deserialize(data, HashSet::class.java, A::class.java) as HashSet<A?>
        assertEquals(arr.size, readArr.size)
        for (x in arr)
            assertTrue(readArr.contains(x))
    }

    @Test
    fun testHashSetOfInts() {
        val arr = hashSetOf(42, null, -1)
        val data = serializer.serialize(arr, HashSet::class.java, java.lang.Integer::class.java)
        @Suppress("UNCHECKED_CAST")
        val readArr = serializer.deserialize(data, HashSet::class.java, java.lang.Integer::class.java) as HashSet<Int?>
        assertEquals(arr.size, readArr.size)
        for (x in arr)
            assertTrue(readArr.contains(x))
    }

    @Test
    fun testHashSetGeneric() {
        val x = G3_3_(hashSetOf(A(42, 117), A(-1, 1000_000_000)))
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, G3_3_::class.java)
        assertEquals(x.set.size, readX.set.size)
        for (o in x.set) {
            assertTrue(readX.set.contains(o))
        }
    }

    @Test
    fun testTreeSet() {
        val arr = TreeSet(listOf(A(42, 117), A(-1, 1000_000_000)))
        val data = serializer.serialize(arr, TreeSet::class.java, A::class.java)
        @Suppress("UNCHECKED_CAST")
        val readArr = serializer.deserialize(data, TreeSet::class.java, A::class.java) as TreeSet<A?>
        assertEquals(arr.size, readArr.size)
        for (x in arr)
            assertTrue(readArr.contains(x))
    }

    @Test
    fun testWriteArrayReadTreeSet() {
        val arr = arrayOf(A(42, 117), A(-1, 1000_000_000))
        val data = serializer.serialize(arr)
        @Suppress("UNCHECKED_CAST")
        val readArr = serializer.deserialize(data, TreeSet::class.java, A::class.java) as TreeSet<A?>
        assertEquals(arr.size, readArr.size)
        for (x in arr)
            assertTrue(readArr.contains(x))
    }

    @Test
    fun testTreeSetOfInts() {
        val arr = TreeSet(listOf(42, -1))
        val data = serializer.serialize(arr, TreeSet::class.java, java.lang.Integer::class.java)
        @Suppress("UNCHECKED_CAST")
        val readArr = serializer.deserialize(data, TreeSet::class.java, java.lang.Integer::class.java) as TreeSet<Int?>
        assertEquals(arr.size, readArr.size)
        for (x in arr)
            assertTrue(readArr.contains(x))
    }

    @Test
    fun testTreeSetGeneric() {
        val x = G3_4_(TreeSet(listOf(A(42, 117), A(-1, 1000_000_000))))
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, G3_4_::class.java)
        assertEquals(x.set.size, readX.set.size)
        for (o in x.set) {
            assertTrue(readX.set.contains(o))
        }
    }
}