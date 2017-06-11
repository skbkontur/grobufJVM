package grobuf

import grobuf.serializers.*
import sun.misc.Unsafe
import java.lang.reflect.Field
import java.nio.ByteBuffer
import kotlin.reflect.KClass

interface Serializer {
    fun <T> getSize(klass: Class<T>, obj: T): Int
    fun <T: Any> getSize(obj: T) = getSize(obj.javaClass, obj)
    fun <T> serialize(klass: Class<T>, obj: T): ByteArray
    fun <T: Any> serialize(obj: T) = serialize(obj.javaClass, obj)
    fun <T: Any> deserialize(klass: Class<T>, data: ByteArray): T
}

open class SerializerImpl: Serializer {

    private val collection = FragmentSerializerCollection()

    override fun <T> getSize(klass: Class<T>, obj: T): Int {
        @Suppress("UNCHECKED_CAST")
        return (collection.getFragmentSerializer(klass) as FragmentSerializer<T>).countSize(WriteContext(), obj)
    }

    override fun <T> serialize(klass: Class<T>, obj: T): ByteArray {
        @Suppress("UNCHECKED_CAST")
        val fragmentSerializer = collection.getFragmentSerializer(klass) as FragmentSerializer<T>
        val context = WriteContext()
        val size = fragmentSerializer.countSize(context, obj)
        context.result = ByteArray(size)
        fragmentSerializer.write(context, obj)
        return context.result
    }

    // TODO: do not return null.
    override fun <T : Any> deserialize(klass: Class<T>, data: ByteArray): T {
        @Suppress("UNCHECKED_CAST")
        val fragmentSerializer = collection.getFragmentSerializer(klass) as FragmentSerializer<T>
        return fragmentSerializer.read(ReadContext().also { it.data = data; it.index = 0 })
    }
}

fun zzz1(x: Byte) : Any { return x }
fun zzz2(x: Short) : Any { return x }
fun zzz3(x: Char) : Any { return x }
fun zzz4(x: Int) : Any { return x }
fun zzz5(x: Boolean) : Any { return x }
fun zzz6(x: Long) : Any { return x }
fun zzz7(x: Float) : Any { return x }
fun zzz8(x: Double) : Any { return x }

data class A(/*@JvmField val b: B?, */@JvmField var x: Int, @JvmField var y: Int, @JvmField var s: String?)

class B(@JvmField var a: A, @JvmField var x: Int)

class B2(@JvmField var a: A?, @JvmField var x: Int)

fun countSize(x: Any?, context: WriteContext) {

}

class Qzz(@JvmField val x: Int, @JvmField val y: java.lang.Integer)

fun zzz(klass: KClass<*>) {
    println(klass)
}

inline fun <reified T> zzz() {
    zzz(T::class)
}

fun main(args: Array<String>) {
    Qzz::class.java.fields.forEach {
        val clazz = it.type
        println(clazz)
        val kClass = clazz.kotlin
        println(kClass)
        println(kClass.java)
    }
    zzz(Int::class)
    zzz<Int>()
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

class Z(val x: Int)

fun test(arr: Array<Any?>) {
    val x = arr[0] as Int

    println(x)
}

fun test1() {
    val size = 1024 * 1024 * 10
    val buf = ByteBuffer.allocateDirect(size)
    for (j in 0..10000) {
        val time = System.nanoTime()

        for (i in 0..size - 1 step 4)
            buf.putInt(i, i)
        val elapsed = (System.nanoTime() - time).toDouble() / 1000
        //println(buf[7])
        println(elapsed)
    }
}

fun test2() {
    val theUnsafe: Field = Unsafe::class.java.getDeclaredField("theUnsafe")
    theUnsafe.isAccessible = true
    val unsafe = theUnsafe.get(null) as Unsafe
    val size = 1024 * 1024 * 10
    val startIndex = unsafe.allocateMemory(size.toLong())
    for (i in 0..size - 1 step 4)
        unsafe.putInt(startIndex + i, i)
//    val arr = ByteArray(size)
//    val zzz = unsafe.arrayBaseOffset(arr::class.java)
//    unsafe.copyMemory(null, startIndex, arr, zzz.toLong(), size.toLong())
//    for (i in 0..10)
//        println(arr[i])
    for (j in 0..10000) {
        val time = System.nanoTime()

        for (i in 0..size - 1 step 4)
            unsafe.putInt(startIndex + i, i)
        val elapsed = (System.nanoTime() - time).toDouble() / 1000
        //println(buf[7])
        println(elapsed)
    }
}

fun test3() {
//    val arr = ByteArray(size)
//    val zzz = unsafe.arrayBaseOffset(arr::class.java)
//    unsafe.copyMemory(null, startIndex, arr, zzz.toLong(), size.toLong())
//    for (i in 0..10)
//        println(arr[i])
    val str = "as;ldkfjalsdkjf;laskjdfsldfjkals;dkjfla;ksjdflasjdf;lkjasdf;ljasdf;lkjasd;lfjkasdf"
    val serializer = StringSerializer()
    val writeContext = WriteContext()
    val size = serializer.countSize(writeContext, str)
    writeContext.result = kotlin.ByteArray(size)
    serializer.write(writeContext, str)
    val readContext = ReadContext().also { it.data = writeContext.result }
    println(serializer.read(readContext))
    for (j in 0..10000) {
        val time = System.nanoTime()
        for (i in 0..10000) {
            readContext.index = 0
            serializer.read(readContext)
        }
        val elapsed = (System.nanoTime() - time).toDouble() / 1000
        //println(buf[7])
        println(elapsed)
    }
}