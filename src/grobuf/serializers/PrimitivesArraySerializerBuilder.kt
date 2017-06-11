package grobuf.serializers

import grobuf.*
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal class PrimitivesArraySerializerBuilder(classLoader: DynamicClassesLoader,
                                                fragmentSerializerCollection: FragmentSerializerCollection,
                                                klass: Class<*>)
    : FragmentSerializerBuilderBase(classLoader, fragmentSerializerCollection, klass) {

    private val elementJvmPrimitiveType = klass.componentType.jvmPrimitiveType!!

    override fun MethodVisitor.countSizeNotNull() {
        visitLdcInsn(1 /* typeCode */ + 4 /* length */) // stack: [5]
        loadObj()                                       // stack: [5, obj]
        visitInsn(Opcodes.ARRAYLENGTH)                  // stack: [5, obj.length]
        visitLdcInsn(elementJvmPrimitiveType.size)      // stack: [5, obj.length, elementSize]
        visitInsn(Opcodes.IMUL)                         // stack: [5, obj.length * elementSize]
        visitInsn(Opcodes.IADD)                         // stack: [5 + obj.length * elementSize]
        ret<Int>()                                      // return 5 + obj.length * elementSize; stack: []
        visitMaxs(3, 3)
    }

    private val JVMPrimitive.writeArraySafeMethodName get() = "write${javaName.capitalize()}ArraySafe"

    override fun MethodVisitor.writeNotNull() {
        writeSafe<Byte> { visitLdcInsn(klass.groBufTypeCode.value) }                     // this.writeByteSafe(result, index, typeCode]; stack: []

        loadObj()                                                                        // stack: [obj]
        visitInsn(Opcodes.ARRAYLENGTH)                                                   // stack: [obj.length]
        visitLdcInsn(elementJvmPrimitiveType.size)                                       // stack: [obj.length, elementSize]
        visitInsn(Opcodes.IMUL)                                                          // stack: [obj.length * elementSize]
        val lengthSlot = 3
        saveToSlot<Int>(lengthSlot)                                                      // length = obj.length * elementSize
        writeSafe<Int> { loadSlot<Int>(lengthSlot) }                                     // this.writeIntSafe(result, index, length]; stack: []

        loadThis()                                                                       // stack: [this]
        loadResult()                                                                     // stack: [this, result]
        loadIndex<WriteContext>()                                                        // stack: [this, result, index]
        loadObj()                                                                        // stack: [this, result, index, obj]
        callVirtual(classType, elementJvmPrimitiveType.writeArraySafeMethodName,
                listOf(ByteArray::class.java, Int::class.java, klass), Void::class.java) // this.writeArray(result, index, obj); stack: []
        increaseIndexBy<WriteContext> { loadSlot<Int>(lengthSlot) }                      // index += length; stack: []

        ret<Void>()
        visitMaxs(4, 4)
    }

    private val JVMPrimitive.readArraySafeMethodName get() = "read${javaName.capitalize()}ArraySafe"

    override fun MethodVisitor.readNotNull() {
        assertTypeCode(klass.groBufTypeCode)
        readSafe<Int>()                                                                  // stack: [*(int*)data[index] => length]
        visitInsn(Opcodes.DUP)                                                           // stack: [length, length]
        val lengthSlot = 3
        saveToSlot<Int>(lengthSlot)                                                      // stack: [length]
        visitLdcInsn(elementJvmPrimitiveType.size)                                       // stack: [length, elementSize]
        visitInsn(Opcodes.IDIV)                                                          // stack: [length / elementSize => arrayLength]
        visitIntInsn(Opcodes.NEWARRAY, elementJvmPrimitiveType.typeAsArrayElement)       // stack: [new <elementType>Array[arrayLength] => result]
        val resultSlot = 4
        saveToSlot<Any>(resultSlot)                                                      // stack: []

        loadThis()                                                                       // stack: [this]
        loadSlot<Any>(resultSlot)                                                        // stack: [this, result]
        loadIndex<ReadContext>()                                                         // stack: [this, result, index]
        loadData()                                                                       // stack: [this, result, index, data]
        callVirtual(classType, elementJvmPrimitiveType.readArraySafeMethodName,
                listOf(klass, Int::class.java, ByteArray::class.java), Void::class.java) // this.readArray(result, index, obj); stack: []
        increaseIndexBy<ReadContext> { loadSlot<Int>(lengthSlot) }                       // index += length; stack: []

        loadSlot<Any>(resultSlot)
        ret(klass)
        visitMaxs(4, 5)
    }
}