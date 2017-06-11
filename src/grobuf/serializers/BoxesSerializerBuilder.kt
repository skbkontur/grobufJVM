package grobuf.serializers

import grobuf.*
import org.objectweb.asm.MethodVisitor

internal class BoxesSerializerBuilder(classLoader: DynamicClassesLoader,
                                      fragmentSerializerCollection: FragmentSerializerCollection,
                                      klass: Class<*>)
    : FragmentSerializerBuilderBase(classLoader, fragmentSerializerCollection, klass) {

    private val klassJVMType = klass.jvmType
    private val jvmPrimitiveType = enumValues<JVMPrimitive>().first { it.boxType == klassJVMType }

    override fun MethodVisitor.countSizeNotNull() {
        visitLdcInsn(1 /* typeCode */ + jvmPrimitiveType.size)
        ret<Int>()
        visitMaxs(1, 3)
    }

    override fun MethodVisitor.writeNotNull() {
        writeSafe<Byte> { visitLdcInsn(klass.groBufTypeCode.value) }     // this.writeByteSafe(result, index, typeCode]; stack: []
        writeSafe(jvmPrimitiveType.klass) {
            loadObj()                                                    // stack: [obj]
            callVirtual(klassJVMType, jvmPrimitiveType.unboxMethodName,
                    emptyList(), jvmPrimitiveType.jvmType)               // stack: [obj.unbox()]
        }                                                                // this.write<type>Safe(result, index, obj.unbox()); stack: []
        ret<Void>()                                                      // return; stack: []
        visitMaxs(4, 3)
    }

    override fun MethodVisitor.readNotNull() {
        // TODO: Coercion between primitives.
        readSafe(jvmPrimitiveType.klass)       // stack: [this.read<type>Safe(result, index)]
        castToObject(jvmPrimitiveType.jvmType) // stack: [this.read<type>Sage(result, index).box()]
        ret(klass)                             // return this.read<type>Sage(result, index).box(); stack: []
        visitMaxs(3, 3)
    }
}