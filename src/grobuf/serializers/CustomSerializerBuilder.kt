package grobuf.serializers

import grobuf.*
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.lang.reflect.Type

internal class CustomSerializerBuilder(classLoader: DynamicClassesLoader,
                                       fragmentSerializerCollection: FragmentSerializerCollection,
                                       type: Type,
                                       serializer: FragmentSerializer<Any>)
    : FragmentSerializerBuilderBase(classLoader, fragmentSerializerCollection, type) {

    private val serializerType = serializer::class.java.jvmType
    private val serializerField = defineField("serializer", serializerType, serializer)

    override fun MethodVisitor.countSizeNotNull(): Int {
        visitLdcInsn(1 /* typeCode */ + 4 /* data length */)              // stack: [5 => size]
        loadField(serializerField)                                        // stack: [size, serializer]
        loadContext()                                                     // stack: [size, serializer, context]
        loadObj()                                                         // stack: [size, serializer, context, obj]
        callVirtual2<WriteContext, Any, Int>(serializerType, "countSize") // stack: [size, serializer.countSize(context, obj[index])]
        visitInsn(Opcodes.IADD)                                           // stack: [size + serializer.countSize(context, obj[index]) => size]
        ret<Int>()
        return 4
    }

    override fun MethodVisitor.writeNotNull(): Int {
        writeSafe<Byte> { visitLdcInsn(GroBufTypeCode.CustomData.value) }

        val start = declareLocal<Int>()
        loadIndex<WriteContext>()                                      // stack: [context.index]
        saveToLocal(start)                                             // start = context.index; stack: []
        increaseIndexBy<WriteContext>(4)                               // context.index += 4; stack: []

        loadField(serializerField)                                     // stack: [serializer]
        loadContext()                                                  // stack: [serializer, context]
        loadObj()                                                      // stack: [serializer, context, obj]
        callVirtual2<WriteContext, Any, Void>(serializerType, "write") // serializer.write(context, obj); stack: []

        writeLength(start)
        ret<Void>()
        return 6
    }

    override fun MethodVisitor.readNotNull(): Int {
        assertTypeCode(GroBufTypeCode.CustomData)

        readSafe<Int>()                                        // stack: [data length]
        loadIndex<ReadContext>()                               // stack: [data length, context.index]
        val end = declareLocal<Int>()
        visitInsn(Opcodes.IADD)                                // stack: [data length + context.index]
        saveToLocal(end)                                       // end = context.index + data length; stack: []

        loadField(serializerField)                             // stack: [serializer]
        loadContext()                                          // stack: [serializer, context]
        callVirtual1<ReadContext, Any>(serializerType, "read") // stack: [serializer.read(context)]
        cast(Any::class.jvmType, type.jvmType)                 // stack: [(type)serializer.read(context) => result]

        loadLocal(end)                                         // stack: [result, end]
        loadIndex<ReadContext>()                               // stack: [result, end, context.index]
        val badDataLabel = Label()
        visitJumpInsn(Opcodes.IF_ICMPNE, badDataLabel)         // if (end != context.index) goto badData; stack: [result]
        ret(klass)

        visitLabel(badDataLabel)
        throwDataCorruptedException("Bad data length")

        return 3
    }
}