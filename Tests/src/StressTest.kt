import data.orders.Orders
import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

class StressTest {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun test() {
        val numberOfMessages = 10000
        val random = Random(31415926535)
        val messages = Array(numberOfMessages) {
            generateRandomTrash<Orders>(random, 75, 10, 2)
        }

        val datas = Array(numberOfMessages) {
            serializer.serialize(messages[it], Orders::class.java)
        }

        val deserializedMessages = Array(numberOfMessages) {
            serializer.deserialize(datas[it], Orders::class.java)
        }

        messages.forEachIndexed { index, orders ->
            val expected = deepPrint(orders)
            val actual = deepPrint(deserializedMessages[index])
            assertEquals(expected, actual)
        }
    }
}