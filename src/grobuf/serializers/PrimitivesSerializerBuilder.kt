package grobuf.serializers

import grobuf.*
import org.objectweb.asm.MethodVisitor

internal class PrimitivesSerializerBuilder(classLoader: DynamicClassesLoader,
                                           fragmentSerializerCollection: FragmentSerializerCollection,
                                           klass: Class<*>)
    : FragmentSerializerBuilderBase(classLoader, fragmentSerializerCollection, klass) {

    private val jvmPrimitiveType = klass.jvmPrimitiveType!!

    override fun MethodVisitor.countSizeNotNull(): Int {
        visitLdcInsn(1 /* typeCode */ + jvmPrimitiveType.size)
        ret<Int>()
        return 1
    }

    override fun MethodVisitor.writeNotNull(): Int {
        writeSafe<Byte> { visitLdcInsn(klass.groBufTypeCode.value) } // this.writeByteSafe(result, index, typeCode]; stack: []
        writeSafe(klass) { loadObj() }                               // this.write<type>Safe(result, index, obj); stack: []
        ret<Void>()                                                  // return; stack: []
        return 4
    }

    override fun MethodVisitor.readNotNull(): Int {
        // TODO: Coercion between primitives.
        readSafe(klass) // stack: [this.read<type>Safe(result, index)]
        ret(klass)      // return this.read<type>Safe(result, index); stack: []
        return 3
    }
}