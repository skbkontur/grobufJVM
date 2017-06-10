import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.*
import org.junit.Test

enum class E1 {
    X,
    Y,
    Z
}

enum class E2 {
    Z,
    X,
    Y
}

class TestEnums {
    val serializer: Serializer = SerializerImpl()

    @Test
    fun testSimple() {
        val e = E1.X
        val data = serializer.serialize(e)
        val readE = serializer.deserialize(E1::class.java, data)
        assertEquals(e, readE)
    }

    @Test
    fun testChangeOrder() {
        val e = E1.Y
        val data = serializer.serialize(e)
        val readE = serializer.deserialize(E2::class.java, data)
        assertEquals(E2.Y, readE)
    }
}