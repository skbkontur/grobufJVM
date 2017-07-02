import data.invoic.Invoic
import data.orders.Orders
import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.util.*
import kotlin.system.measureNanoTime

@Ignore
class PerformanceTest {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testGrobuf() {
        println("Grobuf big data: all types")
        doTestBig<Orders>(100)
        println()
        println("Grobuf big data: strings")
        doTestBig<Invoic>(100)
        println()

        println("Grobuf small data: all types")
        doTestSmall<Orders>(1000)
        println()
        println("Grobuf small data: strings")
        doTestSmall<Invoic>(1000)
        println()

        println("Grobuf tiny data: all types")
        doTestTiny<Orders>(10000)
        println()
        println("Grobuf tiny data: strings")
        doTestTiny<Invoic>(10000)
        println()
    }

    private inline fun <reified T: Any> doTestBig(iterations: Int) =
            doTest<T>(iterations, 80, 100, 10)

    private inline fun <reified T: Any> doTestSmall(iterations: Int) =
            doTest<T>(iterations, 60, 10, 5)

    private inline fun <reified T: Any> doTestTiny(iterations: Int) =
            doTest<T>(iterations, 30, 5, 2)

    private inline fun <reified T: Any> doTest(iterations: Int, fillRate: Int,
                                               stringsLength: Int, arraysSize: Int) {
        val numberOfObjects = 1000
        val random = Random(31415926535)
        val objects = Array(numberOfObjects) {
            generateRandomTrash<T>(random, fillRate, stringsLength, arraysSize)
        }

        print("Warming up")
        for (j in 0..10) {
            for (i in 0..1000) {
                serializer.deserialize(serializer.serialize(objects[0]), T::class.java)
            }
            print(".")
        }
        println()

        val datas = arrayOfNulls<ByteArray>(numberOfObjects)
        var size = 0L
        var elapsed = measureNanoTime {
            for (i in objects.indices) {
                var cur: ByteArray? = null
                for (j in 0 until iterations) {
                    cur = serializer.serialize(objects[(i + j) % objects.size])
                    size += cur.size
                }
                datas[i] = cur
            }
        }
        println("Serializing: ${elapsed * 1.0 / 1000 / numberOfObjects / iterations} microseconds (${Math.round(1_000_000_000.0 * numberOfObjects * iterations / elapsed)} serializations per second")
        println("Size: ${size.toDouble() / numberOfObjects / iterations} bytes")

        val deserializedObjects = arrayOfNulls<T>(numberOfObjects)
        elapsed = measureNanoTime {
            for (i in datas.indices) {
                var cur: T? = null
                for (j in 0 until iterations)
                    cur = serializer.deserialize(datas[(i + j) % datas.size]!!, T::class.java)
                deserializedObjects[i] = cur
            }
        }
        println("Deserializing: ${elapsed * 1.0 / 1000 / numberOfObjects / iterations} microseconds (${Math.round(1_000_000_000.0 * numberOfObjects * iterations / elapsed)} deserializations per second")
    }
}