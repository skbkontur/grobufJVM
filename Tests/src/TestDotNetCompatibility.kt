@file:Suppress("unused")

import grobuf.Decimal
import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*

class ClassW(@JvmField var intArrayVal: IntArray?,
             @JvmField val stringVal: String?,
             @JvmField val doubleVal: Double,
             @JvmField val floatVal: Float,
             @JvmField val nullableShortArrayVal: Array<Short?>?)

class ClassX(@JvmField var classWArrayVal: Array<ClassW?>,
             @JvmField var booleanVal: Boolean,
             @JvmField var longVal: Long)

class ClassY(@JvmField var guidVal: UUID,
             @JvmField var classXVal: ClassX,
             @JvmField var stringArrayVal: Array<String?>)

class ClassZ(@JvmField var intVal: Int,
             @JvmField var stringVal: String,
             @JvmField var dateVal: Date,
             @JvmField var decimalVal: Decimal,
             @JvmField var byteArrayVal: ByteArray,
             @JvmField var classYVal: ClassY)

class TestDotNetCompatibility {

    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun test() {
        val data = javax.xml.bind.DatatypeConverter.parseBase64Binary("AaYBAAA44G17SYVHVwcVzVsHMlTMm/Qj3OIOBgAAAHEAegB6ACKqMAlqpcApHwBAVkjdwNRIP2h+fyU/G7ANAAANADYb5CeNJrYGfW0P9oBk3B+hStxSFAMAAAABAgO2nWhEtqlzswE/AQAAIqa+VCSelD8Pj72BUjbbbE+tdGTUKievcP1JhZqGC2ttAewAAAB0Hz9g9+e0ZgLEAAAAAwAAAAFPAAAAelPfaY3jVL8XDAAAAP/////+/////f///x2Mzq5rPxpmDG6GG/D5IQlAjtWGiaRy2x4LTfgtQB0FqE3QiLfmAgsAAAADAAAABf//AAWc/wEzAAAAelPfaY3jVL8XCAAAAOgDAAARJwAAHYzOrms/GmYMAAAAAAAAAACO1YaJpHLbHgsAAAAAAS8AAAAdjM6uaz8aZgwAAAAAAAAAAI7VhomkctseCwAAAAAdBahN0Ii35gIEAAAAAAAAAPuE2v+TUH4VEQGPzKRTjASyXwmx9JFiVNwrACQ88kMaYN9eAiAAAAAEAAAADgYAAAB6AHoAegAADgAAAAAOBgAAADcENQQ0BA==")
        val readValue = serializer.deserialize(data, ClassZ::class.java)
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        c.set(2017, 6, 2, 0, 0, 0)

        val expectedValue = ClassZ(
                123456789,
                "qzz",
                c.time,
                Decimal.valueOf(1234567898765) / 10000000000000 + Decimal.valueOf(1234567898765432),
                byteArrayOf(1, 2, 3),
                ClassY(
                        UUID(0x70af272ad46474ad, 0x4f6cdb365281bd8f),
                        ClassX(
                                arrayOf(
                                        ClassW(
                                                intArrayOf(-1, -2, -3),
                                                null,
                                                3.14159,
                                                2.71828f,
                                                arrayOf((-1).toShort(), null, (-100).toShort())
                                        ),
                                        ClassW(
                                                intArrayOf(1000, 10001),
                                                null,
                                                0.0,
                                                0.0f,
                                                null
                                        ),
                                        ClassW(
                                                null,
                                                null,
                                                0.0,
                                                0.0f,
                                                arrayOf<Short?>(0)
                                        )
                                ),
                                true,
                                12345678987654321
                        ),
                        arrayOf("zzz", null, "", "зед")
                )
        )
        assertEquals(deepPrint(expectedValue), deepPrint(readValue))
    }
}