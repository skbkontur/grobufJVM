package grobuf.serializers

import grobuf.*
import org.objectweb.asm.MethodVisitor
import java.lang.reflect.Type

internal class BoxesSerializerBuilder(classLoader: DynamicClassesLoader,
                                      fragmentSerializerCollection: FragmentSerializerCollection,
                                      type: Type)
    : FragmentSerializerBuilderBase(classLoader, fragmentSerializerCollection, type) {

    private val klassJVMType = klass.jvmType
    private val jvmPrimitiveType = enumValues<JVMPrimitive>().first { it.boxType == klassJVMType }

    override fun MethodVisitor.countSizeNotNull(): Int {
        visitLdcInsn(1 /* typeCode */ + jvmPrimitiveType.size)
        ret<Int>()
        return 1
    }

    override fun MethodVisitor.writeNotNull(): Int {
        writeSafe<Byte> { visitLdcInsn(klass.groBufTypeCode.value) }     // this.writeByteSafe(result, index, typeCode]; stack: []
        writeSafe(jvmPrimitiveType.klass) {
            loadObj()                                                    // stack: [obj]
            callVirtual(klassJVMType, jvmPrimitiveType.unboxMethodName,
                    emptyList(), jvmPrimitiveType.jvmType)               // stack: [obj.unbox()]
        }                                                                // this.write<type>Safe(result, index, obj.unbox()); stack: []
        ret<Void>()                                                      // return; stack: []
        return 4
    }

    override fun MethodVisitor.readNotNull(): Int {
        // TODO: Coercion between primitives.
        readSafe(jvmPrimitiveType.klass)                   // stack: [this.read<type>Safe(result, index)]
        cast(jvmPrimitiveType.jvmType, Any::class.jvmType) // stack: [this.read<type>Sage(result, index).box()]
        ret(klass)                                         // return this.read<type>Sage(result, index).box(); stack: []
        return 3
    }
}