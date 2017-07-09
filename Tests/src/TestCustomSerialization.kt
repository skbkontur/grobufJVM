import grobuf.*
import grobuf.serializers.FragmentSerializer
import grobuf.serializers.ReadContext
import grobuf.serializers.WriteContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Type

abstract class CBase

class C1(@JvmField var x: Int, @JvmField var y: CBase? = null): CBase()
class C2(@JvmField var x: String): CBase()

class A6(@JvmField var c: CBase?)

class TestCustomSerializerCollection: CustomSerializerCollection {

    override fun get(type: Type, factory: FragmentSerializerFactory): FragmentSerializer<Any>? {
        if (type == CBase::class.java)
            return CBaseSerializer(factory)
        return null
    }

}

class CBaseSerializer(val factory: FragmentSerializerFactory): FragmentSerializer<Any>() {

    private val stringSerializer by lazy { factory.getSerializer(String::class.java) }
    private val c1Serializer by lazy { factory.getSerializer(C1::class.java) }
    private val c2Serializer by lazy { factory.getSerializer(C2::class.java) }

    private val Any.serializer get() = this::class.java.let {
        when (it) {
            C1::class.java -> c1Serializer
            C2::class.java -> c2Serializer
            else -> error("Unknown class: $it")
        }
    }

    private val Any.name get() = this::class.java.let {
        when (it) {
            C1::class.java -> "C1"
            C2::class.java -> "C2"
            else -> error("Unknown class: $it")
        }
    }

    private val String.serializer get() = when (this) {
        "C1" -> c1Serializer
        "C2" -> c2Serializer
        else -> error("Unknown name: $this")
    }

    override fun countSize(context: WriteContext, obj: Any): Int {
        return stringSerializer.countSize(context, obj.name) + obj.serializer.countSize(context, obj)
    }

    override fun write(context: WriteContext, obj: Any) {
        stringSerializer.write(context, obj.name)
        obj.serializer.write(context, obj)
    }

    override fun read(context: ReadContext): Any {
        val name = stringSerializer.read(context) as String
        return name.serializer.read(context)!!
    }

    override fun initialize(serializers: Array<Any?>) { }
}

class TestCustomSerialization {

    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl(PublicFieldsExtractor(true), TestCustomSerializerCollection())
    }

    @Test
    fun test1() {
        val x = A6(C1(42))
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, A6::class.java)
        assertNotNull(readX.c)
        assertEquals(42, (readX.c as C1).x)
    }

    @Test
    fun test2() {
        val x = A6(C2("zzz"))
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, A6::class.java)
        assertNotNull(readX.c)
        assertEquals("zzz", (readX.c as C2).x)
    }

    @Test
    fun testNested() {
        val x = A6(C1(42, C2("zzz")))
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, A6::class.java)
        assertNotNull(readX.c)
        assertEquals(42, (readX.c as C1).x)
        assertNotNull((readX.c as C1).y)
        assertEquals("zzz", ((readX.c as C1).y as C2).x)
    }
}