package grobuf.serializers

import grobuf.*
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.lang.reflect.Modifier

internal class ClassSerializerBuilder(fragmentSerializerCollection: FragmentSerializerCollection, klass: Class<*>)
    : FragmentSerializerBuilderBase(fragmentSerializerCollection, klass) {

    private val klassField by lazy { defineField("klass", klass::class.jvmType, klass) }

    override fun MethodVisitor.countSizeNotNull() {
        visitLdcInsn(1 /* typeCode */ + 4 /* length */)                         // stack: [5 => size]
        klass.appropriateFields.forEach {
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(it.type)
            val fieldVisitedLabel = Label()
            val fieldSlot = 3
            if (it.type.isReference) {
                loadObj()                                                       // stack: [size, obj]
                visitFieldInsn(Opcodes.GETFIELD, klass.jvmType.name,
                        it.name, it.type.jvmType.signature)                     // stack: [size, obj.field]
                visitInsn(Opcodes.DUP)                                          // stack: [size, obj.field, obj.field]
                saveToSlot<Any>(fieldSlot)                                      // field = obj.field; stack: [size, obj.field]
                visitJumpInsn(Opcodes.IFNULL, fieldVisitedLabel)                // if (obj.field == null) goto fieldVisited; stack: [size]
            }
            loadField(getSerializerField(fieldSerializerType))                  // stack: [size, fieldSerializer]
            loadContext()                                                       // stack: [size, fieldSerializer, context]
            if (it.type.isReference)
                loadSlot<Any>(fieldSlot)                                        // stack: [size, fieldSerializer, context, obj.field]
            else {
                loadObj()                                                       // stack: [size, fieldSerializer, context, obj]
                visitFieldInsn(Opcodes.GETFIELD, klass.jvmType.name,
                        it.name, it.type.jvmType.signature)                     // stack: [size, fieldSerializer, context, obj.field]
            }
            callVirtual(fieldSerializerType, "countSize",
                    listOf(WriteContext::class.java, it.type), Int::class.java) // stack: [size, fieldSerializer.countSize(context, obj.field)]
            visitInsn(Opcodes.IADD)                                             // stack: [size + fieldSerializer.countSize(context, obj.field) => size]
            visitLdcInsn(8)                                                     // stack: [size, 8]
            visitInsn(Opcodes.IADD)                                             // stack: [size + 8 => size]
            visitLabel(fieldVisitedLabel)
        }
        ret<Int>()                                                              // return size; stack: []
        visitMaxs(4, 4)
    }

    override fun MethodVisitor.writeNotNull() {
        writeSafe<Byte> { visitLdcInsn(GroBufTypeCode.Object.value) }            // this.writeByteSafe(result, index, Object]; stack: []
        loadIndex<WriteContext>()                                                // stack: [index]
        val startSlot = 3
        saveToSlot<Int>(startSlot)                                               // start = index; stack: []
        increaseIndexBy<WriteContext>(4)                                         // index += 4; stack: []
        klass.appropriateFields.forEach {
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(it.type)
            val fieldVisitedLabel = Label()
            val fieldSlot = 4
            if (it.type.isReference) {
                loadObj()                                                        // stack: [obj]
                visitFieldInsn(Opcodes.GETFIELD, klass.jvmType.name,
                        it.name, it.type.jvmType.signature)                      // stack: [obj.field]
                visitInsn(Opcodes.DUP)                                           // stack: [obj.field, obj.field]
                saveToSlot<Any>(fieldSlot)                                       // field = obj.field; stack: [obj.field]
                visitJumpInsn(Opcodes.IFNULL, fieldVisitedLabel)                 // if (obj.field == null) goto fieldVisited; stack: []
            }
            writeSafe<Long> { visitLdcInsn(HashCalculator.calcHash(it.name)) }   // writeLongSafe(calcHash(fieldName)); stack: []
            loadField(getSerializerField(fieldSerializerType))                   // stack: [fieldSerializer]
            loadContext()                                                        // stack: [fieldSerializer, context]
            if (it.type.isReference)
                loadSlot<Any>(fieldSlot)                                         // stack: [fieldSerializer, context, obj.field]
            else {
                loadObj()                                                        // stack: [fieldSerializer, context, obj]
                visitFieldInsn(Opcodes.GETFIELD, klass.jvmType.name,
                        it.name, it.type.jvmType.signature)                      // stack: [fieldSerializer, context, obj.field]
            }
            callVirtual(fieldSerializerType, "write",
                    listOf(WriteContext::class.java, it.type), Void::class.java) // fieldSerializer.write(context, obj.field); stack: []
            visitLabel(fieldVisitedLabel)
        }

        writeLength(startSlot)

        ret<Void>()
        visitMaxs(6, 5)
    }

    override fun MethodVisitor.readNotNull() {
        val fields = klass.appropriateFields
                .map { it to HashCalculator.calcHash(it.name) }
                .sortedBy { it.second }
        assertTypeCode(GroBufTypeCode.Object)

        val parameterlessConstructor = klass.constructors.firstOrNull { it.parameterCount == 0 }
        if (parameterlessConstructor != null) {
            visitTypeInsn(Opcodes.NEW, klass.jvmType.name)           // stack: [new klass() => result]
            visitInsn(Opcodes.DUP)                                   // stack: [result, result]
            ctorCall0(klass.jvmType)                                 // inst.<init>(); stack: [result]
        } else {
            loadThis()                                               // stack: [this]
            loadField(klassField)                                    // stack: [this, klass]
            callVirtual1<Class<*>, Any>(classType, "createInstance") // stack: [this.createInstance(klass)]
            castObjectTo(klass.jvmType)                              // stack: [(klass)this.createInstance(klass) => result]
        }

        readSafe<Int>()                                              // stack: [result, length]
        visitInsn(Opcodes.DUP)                                       // stack: [result, length, length]
        val emptyLabel = Label()
        visitJumpInsn(Opcodes.IFEQ, emptyLabel)                      // if (length == 0) goto empty; stack: [result, length]
        loadIndex<ReadContext>()                                     // stack: [result, length, index]
        visitInsn(Opcodes.IADD)                                      // stack: [result, length + index]
        val endSlot = 3
        saveToSlot<Int>(endSlot)                                     // end = length + index; stack: [result]

        val loopStartLabel = Label()
        val loopEndLabel = Label()
        visitLabel(loopStartLabel)
        readSafe<Long>()                                             // stack: [*(long*)data[index]]
        val hashCodeSlot = 4
        saveToSlot<Long>(hashCodeSlot)                               // hashCode = *(long*)data[index]; stack: []
        val defaultLabel = Label()
        genSwitch(fields.map { it.second }, hashCodeSlot, defaultLabel) {
            val field = fields[it].first
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(field.type)
            visitInsn(Opcodes.DUP)                                   // stack: [result, result]
            loadField(getSerializerField(fieldSerializerType))       // stack: [result, result, fieldSerializer]
            loadContext()                                            // stack: [result, result, fieldSerializer, context]
            callVirtual(fieldSerializerType, "read",
                    listOf(ReadContext::class.java), field.type)     // stack: [result, result, fieldSerializer.read(context)]
            visitFieldInsn(Opcodes.PUTFIELD, klass.jvmType.name,
                    field.name, field.type.jvmType.signature)        // result.field = fieldSerializer.read(context); stack: [result]
            visitJumpInsn(Opcodes.GOTO, loopEndLabel)
        }

        visitLabel(defaultLabel)
        loadThis()                                                   // stack: [result, this]
        readSafe<Byte>()                                             // stack: [result, this, *(byte*)data[index] => fieldTypeCode]
        loadContext()                                                // stack: [result, this, fieldTypeCode, context]
        callVirtual2<Int, ReadContext, Void>(classType, "skipValue") // this.skipValue(fieldTypeCode, context); stack: [result]

        visitLabel(loopEndLabel)
        loadIndex<ReadContext>()                                     // stack: [result, index]
        loadSlot<Int>(endSlot)                                       // stack: [result, index, end]
        visitJumpInsn(Opcodes.IF_ICMPLT, loopStartLabel)             // if (index < end) goto loopStart; stack: [result]

        loadSlot<Int>(endSlot)                                       // stack: [result, end]
        loadIndex<ReadContext>()                                     // stack: [result, end, context.index]
        val badDataLabel = Label()
        visitJumpInsn(Opcodes.IF_ICMPNE, badDataLabel)               // if (end != context.index) goto badData; stack: [result]
        ret(klass)

        visitLabel(badDataLabel)
        loadThis()
        callVirtual0<Void>(classType, "throwBadDataLengthError")
        ret(klass)

        visitLabel(emptyLabel)                                       // stack: [result, 0]
        visitInsn(Opcodes.POP)                                       // stack: [result]
        ret(klass)                                                   // return result; stack: []
        visitMaxs(4, 5)
    }

    private val Class<*>.appropriateFields: List<java.lang.reflect.Field>
        get() = // TODO: DataMembersExtractor
            fields.filter { Modifier.isPublic(it.modifiers) }
                  .filterNot { Modifier.isStatic(it.modifiers) }
                  .filterNot { Modifier.isFinal(it.modifiers) }

    private fun getSerializerField(type: JVMType) = serializerFields.getOrPut(type) {
        defineField("${type.name.toJVMIdentifier()}_serializer", type, null, true)
    }

    private val serializerFields = mutableMapOf<JVMType, Int>()
}