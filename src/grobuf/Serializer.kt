package grobuf

import grobuf.serializers.*
import sun.misc.Unsafe
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.Field
import java.lang.reflect.Type
import java.nio.ByteBuffer
import kotlin.reflect.KClass

interface Serializer {
    fun <T> getSize(obj: T, klass: Class<T>, vararg typeArguments: Type): Int
    fun <T: Any> getSize(obj: T) = getSize(obj, obj.javaClass)
    fun <T> serialize(obj: T, klass: Class<T>, vararg typeArguments: Type): ByteArray
    fun <T: Any> serialize(obj: T) = serialize(obj, obj.javaClass)
    fun <T: Any> deserialize(data: ByteArray, klass: Class<T>, vararg typeArguments: Type): T
}

open class SerializerImpl: Serializer {

    private val collection = FragmentSerializerCollection(DynamicClassesLoader())

    override fun <T> getSize(obj: T, klass: Class<T>, vararg typeArguments: Type): Int {
        val type = if (typeArguments.isEmpty()) klass else ParameterizedTypeImpl.make(klass, typeArguments, null)
        @Suppress("UNCHECKED_CAST")
        val fragmentSerializer = collection.getFragmentSerializer(type) as FragmentSerializer<T>
        return fragmentSerializer.countSize(WriteContext(), obj)
    }

    override fun <T> serialize(obj: T, klass: Class<T>, vararg typeArguments: Type): ByteArray {
        val type = if (typeArguments.isEmpty()) klass else ParameterizedTypeImpl.make(klass, typeArguments, null)
        @Suppress("UNCHECKED_CAST")
        val fragmentSerializer = collection.getFragmentSerializer(type) as FragmentSerializer<T>
        val context = WriteContext()
        val size = fragmentSerializer.countSize(context, obj)
        context.result = ByteArray(size)
        fragmentSerializer.write(context, obj)
        return context.result
    }

    override fun <T : Any> deserialize(data: ByteArray, klass: Class<T>, vararg typeArguments: Type): T {
        val type = if (typeArguments.isEmpty()) klass else ParameterizedTypeImpl.make(klass, typeArguments, null)
        @Suppress("UNCHECKED_CAST")
        val fragmentSerializer = collection.getFragmentSerializer(type) as FragmentSerializer<T>
        return fragmentSerializer.read(ReadContext().also { it.data = data; it.index = 0 })
    }
}