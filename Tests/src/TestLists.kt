import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

open class G3_2<T>(@JvmField var list: ArrayList<T>)
class G3_2_(list: ArrayList<A>): G3_2<A>(list)

class TestLists {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testList() {
        val arr = ArrayList(listOf(A(42, 117), null, A(-1, 1000_000_000)))
        val data = serializer.serialize(arr, ArrayList::class.java, A::class.java)
        val readArr = serializer.deserialize(data, ArrayList::class.java, A::class.java) as ArrayList<A?>
        assertEquals(arr.size, readArr.size)
        for (i in arr.indices) {
            assertEquals(arr[i]?.x, readArr[i]?.x)
            assertEquals(arr[i]?.y, readArr[i]?.y)
        }
    }

    @Test
    fun testWriteArrayReadList() {
        val arr = arrayOf(A(42, 117), null, A(-1, 1000_000_000))
        val data = serializer.serialize(arr)
        val readArr = serializer.deserialize(data, ArrayList::class.java, A::class.java) as ArrayList<A?>
        assertEquals(arr.size, readArr.size)
        for (i in arr.indices) {
            assertEquals(arr[i]?.x, readArr[i]?.x)
            assertEquals(arr[i]?.y, readArr[i]?.y)
        }
    }

    @Test
    fun testListOfInts() {
        val arr = ArrayList(listOf(42, null, -1))
        val data = serializer.serialize(arr, ArrayList::class.java, java.lang.Integer::class.java)
        val readArr = serializer.deserialize(data, ArrayList::class.java, java.lang.Integer::class.java) as ArrayList<Int?>
        assertEquals(arr.size, readArr.size)
        for (i in arr.indices) {
            assertEquals(arr[i], readArr[i])
        }
    }

    @Test
    fun testListGeneric() {
        val x = G3_2_(ArrayList(listOf(A(42, 117), A(-1, 1000_000_000))))
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, G3_2_::class.java)
        assertEquals(x.list.size, readX.list.size)
        for (i in x.list.indices) {
            assertEquals(x.list[i].x, readX.list[i].x)
            assertEquals(x.list[i].y, readX.list[i].y)
        }
    }
}