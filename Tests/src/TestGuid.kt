import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

class TestGuid {

    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testSimple() {
        val uuid = UUID(12345678987654321, -98765432123456789)
        val data = serializer.serialize(uuid)
        val readUuid = serializer.deserialize(data, UUID::class.java)
        assertEquals(uuid, readUuid)
    }
}