package grobuf.serializers

import grobuf.*
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.lang.reflect.Type

internal class PrimitivesArraySerializerBuilder(classLoader: DynamicClassesLoader,
                                                fragmentSerializerCollection: FragmentSerializerCollection,
                                                type: Type)
    : FragmentSerializerBuilderBase(classLoader, fragmentSerializerCollection, type) {

    private val elementJvmPrimitiveType = klass.componentType.jvmPrimitiveType!!

    override fun MethodVisitor.countSizeNotNull(): Int {
        visitLdcInsn(1 /* typeCode */ + 4 /* length */) // stack: [5]
        loadObj()                                       // stack: [5, obj]
        visitInsn(Opcodes.ARRAYLENGTH)                  // stack: [5, obj.length]
        visitLdcInsn(elementJvmPrimitiveType.size)      // stack: [5, obj.length, elementSize]
        visitInsn(Opcodes.IMUL)                         // stack: [5, obj.length * elementSize]
        visitInsn(Opcodes.IADD)                         // stack: [5 + obj.length * elementSize]
        ret<Int>()                                      // return 5 + obj.length * elementSize; stack: []
        return 3
    }

    private val JVMPrimitive.writeArraySafeMethodName get() = "write${javaName.capitalize()}ArraySafe"

    override fun MethodVisitor.writeNotNull(): Int {
        writeSafe<Byte> { visitLdcInsn(klass.groBufTypeCode.value) }                     // this.writeByteSafe(result, index, typeCode]; stack: []

        loadObj()                                                                        // stack: [obj]
        visitInsn(Opcodes.ARRAYLENGTH)                                                   // stack: [obj.length]
        visitLdcInsn(elementJvmPrimitiveType.size)                                       // stack: [obj.length, elementSize]
        visitInsn(Opcodes.IMUL)                                                          // stack: [obj.length * elementSize]
        val length = declareLocal<Int>()
        saveToLocal(length)                                                              // length = obj.length * elementSize
        writeSafe<Int> { loadLocal(length) }                                             // this.writeIntSafe(result, index, length]; stack: []

        loadThis()                                                                       // stack: [this]
        loadResult()                                                                     // stack: [this, result]
        loadIndex<WriteContext>()                                                        // stack: [this, result, index]
        loadObj()                                                                        // stack: [this, result, index, obj]
        callVirtual(classType, elementJvmPrimitiveType.writeArraySafeMethodName,
                listOf(ByteArray::class.java, Int::class.java, klass), Void::class.java) // this.writeArray(result, index, obj); stack: []
        increaseIndexBy<WriteContext> { loadLocal(length) }                              // index += length; stack: []

        ret<Void>()
        return 4
    }

    private val JVMPrimitive.readArraySafeMethodName get() = "read${javaName.capitalize()}ArraySafe"

    override fun MethodVisitor.readNotNull(): Int {
        val unsignedArrayTypeCode = getUnsignedArrayTypeCode(klass.groBufTypeCode)
        if (unsignedArrayTypeCode == null)
            assertTypeCode(klass.groBufTypeCode)
        else
            assertTypeCode(klass.groBufTypeCode, unsignedArrayTypeCode)
        readSafe<Int>()                                                                  // stack: [*(int*)data[index] => length]
        visitInsn(Opcodes.DUP)                                                           // stack: [length, length]
        val length = declareLocal<Int>()
        saveToLocal(length)                                                              // stack: [length]
        visitLdcInsn(elementJvmPrimitiveType.size)                                       // stack: [length, elementSize]
        visitInsn(Opcodes.IDIV)                                                          // stack: [length / elementSize => arrayLength]
        visitIntInsn(Opcodes.NEWARRAY, elementJvmPrimitiveType.typeAsArrayElement)       // stack: [new <elementType>Array[arrayLength] => result]
        val result = declareLocal<Any>()
        saveToLocal(result)                                                              // stack: []

        loadThis()                                                                       // stack: [this]
        loadLocal(result)                                                                // stack: [this, result]
        loadIndex<ReadContext>()                                                         // stack: [this, result, index]
        loadData()                                                                       // stack: [this, result, index, data]
        callVirtual(classType, elementJvmPrimitiveType.readArraySafeMethodName,
                listOf(klass, Int::class.java, ByteArray::class.java), Void::class.java) // this.readArray(result, index, obj); stack: []
        increaseIndexBy<ReadContext> { loadLocal(length) }                               // index += length; stack: []

        loadLocal(result)
        ret(klass)
        return 4
    }

    private fun getUnsignedArrayTypeCode(typeCode: GroBufTypeCode) = when (typeCode) {
        GroBufTypeCode.Int8Array -> GroBufTypeCode.UInt8Array
        GroBufTypeCode.Int16Array -> GroBufTypeCode.UInt16Array
        GroBufTypeCode.Int32Array -> GroBufTypeCode.UInt32Array
        GroBufTypeCode.Int64Array -> GroBufTypeCode.UInt64Array
        else -> null
    }
}