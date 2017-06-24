import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TestStrings {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testSimple() {
        val str = "zzz"
        val data = serializer.serialize(str)
        val readStr = serializer.deserialize(data, String::class.java)
        assertEquals(str, readStr)
    }

    @Test
    fun testEmpty() {
        val str = ""
        val data = serializer.serialize(str)
        val readStr = serializer.deserialize(data, String::class.java)
        assertEquals(str, readStr)
    }
}