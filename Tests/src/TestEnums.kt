import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.*
import org.junit.Before
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
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testSimple() {
        val e = E1.X
        val data = serializer.serialize(e)
        val readE = serializer.deserialize(data, E1::class.java)
        assertEquals(e, readE)
    }

    @Test
    fun testChangeOrder() {
        val e = E1.Y
        val data = serializer.serialize(e)
        val readE = serializer.deserialize(data, E2::class.java)
        assertEquals(E2.Y, readE)
    }
}