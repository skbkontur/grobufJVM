package grobuf

import grobuf.serializers.*
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinProperty

interface FragmentSerializerFactory {
    val baseSerializer: FragmentSerializer<Any?>
    fun getSerializer(type: Type): FragmentSerializer<Any?>
}

interface CustomSerializerCollection {
    fun get(type: Type, factory: FragmentSerializerFactory): FragmentSerializer<Any>?
}

class DataMember(val id: Long, val name: String, val field: Field)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class GroboMember(val name: String = "", val id: Long = 0)

interface DataMembersExtractor {
    fun getMembers(klass: Class<*>): List<DataMember>
}

/**
 * Selects all public non-final non-static fields.
 */
class PublicFieldsExtractor(val capitalizeNames: Boolean): DataMembersExtractor {

    override fun getMembers(klass: Class<*>) =
            klass.fields
                    .filter { Modifier.isPublic(it.modifiers) }
                    .filterNot { Modifier.isStatic(it.modifiers) }
                    .filterNot { Modifier.isFinal(it.modifiers) }
                    .map {
                        val groboMember = it.getDeclaredAnnotation(GroboMember::class.java)
                                ?: it.kotlinProperty?.findAnnotation<GroboMember>()
                        val id = groboMember?.id ?: 0
                        val name = groboMember?.name ?: (if (capitalizeNames) it.name.capitalize() else it.name)
                        DataMember(id, name, it)
                    }
}

interface Serializer {
    fun <T> getSize(obj: T, klass: Class<T>, vararg typeArguments: Type): Int
    fun <T: Any> getSize(obj: T) = getSize(obj, obj.javaClass)
    fun <T> serialize(obj: T, klass: Class<T>, vararg typeArguments: Type): ByteArray
    fun <T: Any> serialize(obj: T) = serialize(obj, obj.javaClass)
    fun <T: Any> deserialize(data: ByteArray, klass: Class<T>, vararg typeArguments: Type): T
}

open class SerializerImpl(dataMembersExtractor: DataMembersExtractor,
                          customSerializerCollection: CustomSerializerCollection?): Serializer {

    constructor(): this(PublicFieldsExtractor(true), null)

    private val collection = FragmentSerializerCollection(DynamicClassesLoader(), dataMembersExtractor, customSerializerCollection)

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