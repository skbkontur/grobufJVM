package grobuf.serializers

import grobuf.*
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal class ArraySerializerBuilder(classLoader: DynamicClassesLoader,
                                      fragmentSerializerCollection: FragmentSerializerCollection,
                                      klass: Class<*>)
    : FragmentSerializerBuilderBase(classLoader, fragmentSerializerCollection, klass) {

    private val elementType = klass.componentType!!
    private val elementSerializerType = fragmentSerializerCollection.getFragmentSerializerType(elementType)
    private val elementSerializerField = defineField("elementSerializer", elementSerializerType, null, true)

    override fun MethodVisitor.countSizeNotNull() {
        visitLdcInsn(1 /* typeCode */ + 4 /* data length */ + 4 /* array length */) // stack: [9 => size]
        loadObj()                                                                   // stack: [size, obj]
        visitInsn(Opcodes.ARRAYLENGTH)                                              // stack: [size, obj.length]
        visitInsn(Opcodes.DUP)                                                      // stack: [size, obj.length, obj.length]
        val lengthSlot = 3
        saveToSlot<Int>(lengthSlot)                                                 // length = obj.length; stack: [size, length]
        val doneLabel = Label()
        visitJumpInsn(Opcodes.IFEQ, doneLabel)                                      // if (length == 0) goto done; stack: [size]

        val indexSlot = 4
        visitLdcInsn(0)                                                             // stack: [size, 0]
        saveToSlot<Int>(indexSlot)                                                  // index = 0; stack: [size]
        val loopStartLabel = Label()
        visitLabel(loopStartLabel)
        loadField(elementSerializerField)                                           // stack: [size, elementSerializer]
        loadContext()                                                               // stack: [size, elementSerializer, context]
        loadObj()                                                                   // stack: [size, elementSerializer, context, obj]
        loadSlot<Int>(indexSlot)                                                    // stack: [size, elementSerializer, context, obj, index]
        visitInsn(Opcodes.AALOAD)                                                   // stack: [size, elementSerializer, context, obj[index]]
        callVirtual(elementSerializerType, "countSize",
                listOf(WriteContext::class.java, elementType), Int::class.java)     // stack: [size, elementSerializer.countSize(context, obj[index])]
        visitInsn(Opcodes.IADD)                                                     // stack: [size + elementSerializer.countSize(context, obj[index]) => size]

        loadSlot<Int>(indexSlot)                                                    // stack: [size, index]
        visitLdcInsn(1)                                                             // stack: [size, index, 1]
        visitInsn(Opcodes.IADD)                                                     // stack: [size, index + 1]
        visitInsn(Opcodes.DUP)                                                      // stack: [size, index + 1, index + 1]
        saveToSlot<Int>(indexSlot)                                                  // index = index + 1; stack: [size, index]
        loadSlot<Int>(lengthSlot)                                                   // stack: [size, index, length]
        visitJumpInsn(Opcodes.IF_ICMPLT, loopStartLabel)                            // if (index < length) goto loopStart; stack: [size]

        visitLabel(doneLabel)
        ret<Int>()                                                                  // return size; stack: []
        visitMaxs(5, 5)
    }

    override fun MethodVisitor.writeNotNull() {
        writeSafe<Byte> { visitLdcInsn(GroBufTypeCode.Array.value) }

        val startSlot = 3
        loadIndex<WriteContext>()                                                   // stack: [context.index]
        saveToSlot<Int>(startSlot)                                                  // start = context.index; stack: []
        increaseIndexBy<WriteContext>(4)                                            // context.index += 4; stack: []

        loadObj()                                                                   // stack: [obj]
        visitInsn(Opcodes.ARRAYLENGTH)                                              // stack: [obj.length]
        visitInsn(Opcodes.DUP)                                                      // stack: [obj.length, obj.length]
        val lengthSlot = 4
        saveToSlot<Int>(lengthSlot)                                                 // length = obj.length; stack: [length]
        writeSafe<Int> { loadSlot<Int>(lengthSlot) }                                // writeIntSafe(length); stack: [length]
        val doneLabel = Label()
        visitJumpInsn(Opcodes.IFEQ, doneLabel)                                      // if (length == 0) goto done; stack: []

        val indexSlot = 5
        visitLdcInsn(0)                                                             // stack: [0]
        saveToSlot<Int>(indexSlot)                                                  // index = 0; stack: []
        val loopStartLabel = Label()
        visitLabel(loopStartLabel)
        loadField(elementSerializerField)                                           // stack: [elementSerializer]
        loadContext()                                                               // stack: [elementSerializer, context]
        loadObj()                                                                   // stack: [elementSerializer, context, obj]
        loadSlot<Int>(indexSlot)                                                    // stack: [elementSerializer, context, obj, index]
        visitInsn(Opcodes.AALOAD)                                                   // stack: [elementSerializer, context, obj[index]]
        callVirtual(elementSerializerType, "write",
                listOf(WriteContext::class.java, elementType), Void::class.java)    // elementSerializer.write(context, obj[index]); stack: []

        loadSlot<Int>(indexSlot)                                                    // stack: [index]
        visitLdcInsn(1)                                                             // stack: [index, 1]
        visitInsn(Opcodes.IADD)                                                     // stack: [index + 1]
        visitInsn(Opcodes.DUP)                                                      // stack: [index + 1, index + 1]
        saveToSlot<Int>(indexSlot)                                                  // index = index + 1; stack: [index]
        loadSlot<Int>(lengthSlot)                                                   // stack: [index, length]
        visitJumpInsn(Opcodes.IF_ICMPLT, loopStartLabel)                            // if (index < length) goto loopStart; stack: []

        visitLabel(doneLabel)
        writeLength(startSlot)
        ret<Void>()
        visitMaxs(6, 6)
    }

    override fun MethodVisitor.readNotNull() {
        assertTypeCode(GroBufTypeCode.Array)

        readSafe<Int>()                                            // stack: [data length]
        loadIndex<ReadContext>()                                   // stack: [data length, context.index]
        val endSlot = 3
        visitInsn(Opcodes.IADD)                                    // stack: [data length + context.index]
        saveToSlot<Int>(endSlot)                                   // end = context.index + data length; stack: []
        readSafe<Int>()                                            // stack: [length]
        val lengthSlot = 4
        visitInsn(Opcodes.DUP)                                     // stack: [length, length]
        saveToSlot<Int>(lengthSlot)                                // stack: [length]
        visitTypeInsn(Opcodes.ANEWARRAY, elementType.jvmType.name) // stack: [new elementType[length] => result]
        loadSlot<Int>(lengthSlot)
        val doneLabel = Label()
        visitJumpInsn(Opcodes.IFEQ, doneLabel)                     // if (length == 0) goto done; stack: [result]

        val indexSlot = 5
        visitLdcInsn(0)                                            // stack: [result, 0]
        saveToSlot<Int>(indexSlot)                                 // index = 0; stack: [result]
        val loopStartLabel = Label()
        visitLabel(loopStartLabel)
        visitInsn(Opcodes.DUP)                                     // stack: [result, result]
        loadSlot<Int>(indexSlot)                                   // stack: [result, result, index]
        loadField(elementSerializerField)                          // stack: [result, result, index, elementSerializer]
        loadContext()                                              // stack: [result, result, index, elementSerializer, context]
        callVirtual(elementSerializerType, "read",
                listOf(ReadContext::class.java), elementType)      // stack: [result, result, index, elementSerializer.read(context)]
        visitInsn(Opcodes.AASTORE)                                 // result[index] = elementSerializer.read(context); stack: [result]

        loadSlot<Int>(indexSlot)                                   // stack: [result, index]
        visitLdcInsn(1)                                            // stack: [result, index, 1]
        visitInsn(Opcodes.IADD)                                    // stack: [result, index + 1]
        visitInsn(Opcodes.DUP)                                     // stack: [result, index + 1, index + 1]
        saveToSlot<Int>(indexSlot)                                 // index = index + 1; stack: [result, index]
        loadSlot<Int>(lengthSlot)                                  // stack: [result, index, length]
        visitJumpInsn(Opcodes.IF_ICMPLT, loopStartLabel)           // if (index < length) goto loopStart; stack: [result]

        visitLabel(doneLabel)
        loadSlot<Int>(endSlot)                                     // stack: [result, end]
        loadIndex<ReadContext>()                                   // stack: [result, end, context.index]
        val badDataLabel = Label()
        visitJumpInsn(Opcodes.IF_ICMPNE, badDataLabel)             // if (end != context.index) goto badData; stack: [result]
        ret(klass)

        visitLabel(badDataLabel)
        loadThis()
        callVirtual0<Void>(classType, "throwBadDataLengthError")
        ret(klass)

        visitMaxs(5, 6)
    }
}