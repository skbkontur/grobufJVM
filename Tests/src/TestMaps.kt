import grobuf.Serializer
import grobuf.SerializerImpl
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

open class G5<T>(@JvmField var m: HashMap<String, T>)
class G5_(m: HashMap<String, A>): G5<A>(m)

class TestMaps {
    lateinit var serializer: Serializer

    @Before
    fun setUp() {
        serializer = SerializerImpl()
    }

    @Test
    fun testHashMap() {
        val map = hashMapOf("zzz" to 42)
        val data = serializer.serialize(map, HashMap::class.java, String::class.java, java.lang.Integer::class.java)
        val readMap = serializer.deserialize(data, HashMap::class.java, String::class.java, java.lang.Integer::class.java)
        assertEquals(1, readMap.size)
        assertEquals(42, readMap["zzz"])
    }

    @Test
    fun testLinkedHashMap() {
        val map = linkedMapOf("zzz" to 42)
        val data = serializer.serialize(map, LinkedHashMap::class.java, String::class.java, java.lang.Integer::class.java)
        val readMap = serializer.deserialize(data, LinkedHashMap::class.java, String::class.java, java.lang.Integer::class.java)
        assertEquals(1, readMap.size)
        assertEquals(42, readMap["zzz"])
    }

    @Test
    fun testTreeMap() {
        val map = TreeMap(mapOf("zzz" to 42))
        val data = serializer.serialize(map, TreeMap::class.java, String::class.java, java.lang.Integer::class.java)
        val readMap = serializer.deserialize(data, TreeMap::class.java, String::class.java, java.lang.Integer::class.java)
        assertEquals(1, readMap.size)
        assertEquals(42, readMap["zzz"])
    }

    @Test
    fun testGeneric() {
        val map = hashMapOf("zzz" to A(42, 117))
        val x = G5_(map)
        val data = serializer.serialize(x)
        val readX = serializer.deserialize(data, G5_::class.java)
        assertNotNull(readX.m)
        val a = readX.m["zzz"]
        assertNotNull(a)
        assertEquals(42, a?.x)
        assertEquals(117, a?.y)
    }
}
