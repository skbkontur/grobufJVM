package grobuf.serializers

import grobuf.*
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.lang.reflect.Type

internal class PrimitivesSerializerBuilder(classLoader: DynamicClassesLoader,
                                           fragmentSerializerCollection: FragmentSerializerCollection,
                                           type: Type)
    : FragmentSerializerBuilderBase(classLoader, fragmentSerializerCollection, type) {

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
        val doCoercionLabel = Label()
        loadTypeCode()                                    // stack: [typeCode]
        visitLdcInsn(klass.groBufTypeCode.value)          // stack: [typeCode, expectedTypeCode]
        visitJumpInsn(Opcodes.IF_ICMPNE, doCoercionLabel) // if (typeCode != expectedTypeCode) goto doCoercion; stack: []

        readSafe(klass)                                   // stack: [this.read<type>Safe(result, index)]
        ret(klass)                                        // return this.read<type>Safe(result, index); stack: []

        visitLabel(doCoercionLabel)
        val primitives = enumValues<JVMPrimitive>()
                .filterNot { it == JVMPrimitive.VOID || it.klass == klass }
                .map { it to (it.klass.groBufTypeCode.value.toInt() and 0xFF) }
                .sortedBy { it.second }
        val defaultLabel = Label()
        val labels = primitives.map { Label() }.toTypedArray()
        loadTypeCode()                                    // stack: [typeCode]
        visitLookupSwitchInsn(defaultLabel,
                primitives.map { it.second }.toIntArray(),
                labels)                                   // switch(typeCode); stack: []
        primitives.forEachIndexed { index, (primitive) ->
            visitLabel(labels[index])
            readSafe(primitive.klass)                     // stack: [value]
            coerce(primitive, jvmPrimitiveType)           // stack: [(klass)value]
            ret(klass)
        }

        visitLabel(defaultLabel)
        loadDefault(klass)
        ret(klass)
        return 3
    }
}