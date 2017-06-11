import grobuf.FragmentSerializerCollection
import grobuf.serializers.ReadContext
import grobuf.serializers.WriteContext

data class A(/*@JvmField val b: B?, */@JvmField var x: Int, @JvmField var y: Int, @JvmField var s: String?)

class B(@JvmField var a: A, @JvmField var x: Int)

class B2(@JvmField var a: A?, @JvmField var x: Int)

fun main(args: Array<String>) {
    val fragmentSerializerCollection = FragmentSerializerCollection()
    val serializer = fragmentSerializerCollection.getFragmentSerializer<B>()
    val context = WriteContext()
    val obj = B(A(42, 117, "zzz"), -1)
    val size = serializer.countSize(context, obj)
    println(size)
    val arr = ByteArray(size)
    context.index = 0
    context.result = arr
    serializer.write(context, obj)
    println(arr.contentToString())
    val serializer2 = fragmentSerializerCollection.getFragmentSerializer<B2>()
    val readB2 = serializer2.read(ReadContext().also { it.data = arr; it.index = 0 })
    println(readB2.x)
    println(readB2.a!!.x)
    println(readB2.a!!.y)
    println(readB2.a!!.s)
    val serializer3 = fragmentSerializerCollection.getFragmentSerializer<IntArray>()
    val size3 = serializer3.countSize(context, intArrayOf(1, 2, 3))
    val arr3 = ByteArray(size3)
    context.index = 0
    context.result = arr3
    serializer3.write(context, intArrayOf(1, 2, 3))
    println(arr3.contentToString())
    val readArr = serializer3.read(ReadContext().also { it.data = arr3; it.index = 0 })
    println(readArr.contentToString())

    val serializer4 = fragmentSerializerCollection.getFragmentSerializer<Array<A>>()
    val size4 = serializer4.countSize(context, arrayOf(A(42, 117, "zzz"), A(-1, 314, "qxx")))
    val arr4 = ByteArray(size4)
    context.index = 0
    context.result = arr4
    serializer4.write(context, arrayOf(A(42, 117, "zzz"), A(-1, 314, "qxx")))
    println(arr4.contentToString())
    val readArr4 = serializer4.read(ReadContext().also { it.data = arr4; it.index = 0 })
    println(readArr4.contentToString())

    //test3()
}