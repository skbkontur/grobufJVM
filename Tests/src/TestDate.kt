import grobuf.GroBufTypeCode
import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class TestDate {

    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testOrigin() {
        val date = Date(0)
        val data = serializer.serialize(date)
        val readDate = serializer.deserialize(data, Date::class.java)
        assertEquals(date.time, readDate.time)
    }

    @Test
    fun testNegative() {
        val date = Date(-1_000_000_000_000)
        val data = serializer.serialize(date)
        val readDate = serializer.deserialize(data, Date::class.java)
        assertEquals(date.time, readDate.time)
    }

    @Test
    fun testPositive() {
        val date = Date(1_000_000_000_000)
        val data = serializer.serialize(date)
        val readDate = serializer.deserialize(data, Date::class.java)
        assertEquals(date.time, readDate.time)
    }

    @Test
    fun testCSharpLocal() {
        val buf = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(0, GroBufTypeCode.DateTimeNew.value)
        buf.putLong(1, -8587031900529380925)
        val readDate = serializer.deserialize(buf.array(), Date::class.java)
        assertEquals(1498416832539, readDate.time)
    }

    @Test
    fun testCSharpUtc() {
        val buf = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(0, GroBufTypeCode.DateTimeNew.value)
        buf.putLong(1, 5248026157433427832)
        val readDate = serializer.deserialize(buf.array(), Date::class.java)
        assertEquals(1498417100603, readDate.time)
    }
}