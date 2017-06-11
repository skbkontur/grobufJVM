package grobuf.serializers

import grobuf.*
import org.objectweb.asm.MethodVisitor

internal class PrimitivesSerializerBuilder(fragmentSerializerCollection: FragmentSerializerCollection, klass: Class<*>)
    : FragmentSerializerBuilderBase(fragmentSerializerCollection, klass) {

    private val jvmPrimitiveType = klass.jvmPrimitiveType!!

    override fun MethodVisitor.countSizeNotNull() {
        visitLdcInsn(1 /* typeCode */ + jvmPrimitiveType.size)
        ret<Int>()
        visitMaxs(1, 3)
    }

    override fun MethodVisitor.writeNotNull() {
        writeSafe<Byte> { visitLdcInsn(klass.groBufTypeCode.value) } // this.writeByteSafe(result, index, typeCode]; stack: []
        writeSafe(klass) { loadObj() }                               // this.write<type>Safe(result, index, obj); stack: []
        ret<Void>()                                                  // return; stack: []
        visitMaxs(4, 3)
    }

    override fun MethodVisitor.readNotNull() {
        // TODO: Coercion between primitives.
        readSafe(klass) // stack: [this.read<type>Safe(result, index)]
        ret(klass)      // return this.read<type>Safe(result, index); stack: []
        visitMaxs(3, 3)
    }
}