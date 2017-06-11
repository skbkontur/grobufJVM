import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.*
import org.junit.Test

class TestStrings {
    val serializer: Serializer = SerializerImpl()

    @Test
    fun testSimple() {
        val str = "zzz"
        val data = serializer.serialize(str)
        val readStr = serializer.deserialize(String::class.java, data)
        assertEquals(str, readStr)
    }

    @Test
    fun testEmpty() {
        val str = ""
        val data = serializer.serialize(str)
        val readStr = serializer.deserialize(String::class.java, data)
        assertEquals(str, readStr)
    }
}