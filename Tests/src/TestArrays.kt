import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

open class G3<T>(@JvmField var arr: Array<T>)

class G3_(arr: Array<A>): G3<A>(arr)

class TestArrays {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testSimple() {
        val arr = arrayOf(A(42, 117), A(-1, 1000_000_000))
        val data = serializer.serialize(arr)
        val readArr = serializer.deserialize(data, Array<A>::class.java)
        assertEquals(arr.size, readArr.size)
        for (i in arr.indices) {
            assertEquals(arr[i].x, readArr[i].x)
            assertEquals(arr[i].y, readArr[i].y)
        }
    }

    @Test
    fun testGeneric() {
        val x = G3_(arrayOf(A(42, 117), A(-1, 1000_000_000)))
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, G3_::class.java)
        assertEquals(x.arr.size, readX.arr.size)
        for (i in x.arr.indices) {
            assertEquals(x.arr[i].x, readX.arr[i].x)
            assertEquals(x.arr[i].y, readX.arr[i].y)
        }
    }
}