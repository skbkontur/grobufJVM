package grobuf.serializers

import grobuf.*
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.lang.reflect.Type

internal class EnumSerializerBuilder(classLoader: DynamicClassesLoader,
                                     fragmentSerializerCollection: FragmentSerializerCollection,
                                     type: Type)
    : FragmentSerializerBuilderBase(classLoader, fragmentSerializerCollection, type) {

    @Suppress("UNCHECKED_CAST")
    val values = (klass.enumConstants!! as Array<Enum<*>>)
            .map { it to HashCalculator.calcHash(it.name) }
            .sortedBy { it.second }
    val valuesField = defineField("values", Array<Any?>::class.jvmType,
            values.map { it.first }.toTypedArray())
    val hashCodesField = defineField("hashCodes", LongArray::class.jvmType,
            values.sortedBy { it.first.ordinal }
                  .map { it.second }
                  .toLongArray()
    )

    override fun MethodVisitor.countSizeNotNull(): Int {
        visitLdcInsn(1 /* typeCode */ + 8 /* hashCode */)
        ret<Int>()
        return 1
    }

    override fun MethodVisitor.writeNotNull(): Int {
        writeSafe<Byte> { visitLdcInsn(GroBufTypeCode.Enum.value) } // this.writeByteSafe(result, index, Object]; stack: []
        writeSafe<Long> {
            loadField(hashCodesField)                               // stack: [this.hashCodes]
            loadObj()                                               // stack: [this.hashCodes, obj]
            callVirtual0<Int>(klass.jvmType, "ordinal")             // stack: [this.hashCodes, obj.ordinal()]
            visitInsn(Opcodes.LALOAD)                               // stack: [this.hashCodes[obj.ordinal()]]
        }                                                           // this.writeLongSafe(result, index, this.hashCodes[obj.ordinal()]; stack: []
        ret<Void>()
        return 5
    }

    override fun MethodVisitor.readNotNull(): Int {
        assertTypeCode(GroBufTypeCode.Enum)
        readSafe<Long>()                // stack: [*(long*)data[index]]
        val hashCode = declareLocal<Long>()
        saveToLocal(hashCode)           // hashCode = *(long*)data[index]; stack: []
        val defaultLabel = Label()
        loadField(valuesField)          // stack: [this.values]
        genSwitch(values.map { it.second }, hashCode.slot, defaultLabel) {
            visitLdcInsn(it)            // stack: [this.values, index]
            visitInsn(Opcodes.AALOAD)   // stack: [this.values[index]]
            cast(Any::class.jvmType, klass.jvmType) // stack: [(type)this.value[index]]
            ret(klass)                  // return (type)this.values[index]; stack: []
        }

        visitLabel(defaultLabel)        // stack: [this.values]
        visitInsn(Opcodes.POP)          // stack: []
        loadDefault(klass)              // stack: [null]
        ret(klass)                      // return null; stack: []
        return 3
    }
}