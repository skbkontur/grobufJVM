import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.*
import org.junit.Test

class TestArrays {
    val serializer: Serializer = SerializerImpl()

    @Test
    fun testSimple() {
        val arr = arrayOf(A(42, 117), A(-1, 1000_000_000))
        val data = serializer.serialize(arr)
        val readArr = serializer.deserialize(Array<A>::class.java, data)
        assertEquals(arr.size, readArr.size)
        for (i in arr.indices) {
            assertEquals(arr[i].x, readArr[i].x)
            assertEquals(arr[i].y, readArr[i].y)
        }
    }
}