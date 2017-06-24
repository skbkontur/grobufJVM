package grobuf.serializers

import grobuf.*
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.lang.reflect.Modifier
import java.lang.reflect.Type

internal class TupleSerializerBuilder(classLoader: DynamicClassesLoader,
                                      fragmentSerializerCollection: FragmentSerializerCollection,
                                      type: Type)
    : FragmentSerializerBuilderBase(classLoader, fragmentSerializerCollection, type) {

    override fun MethodVisitor.countSizeNotNull(): Int {
        visitLdcInsn(1 /* typeCode */ + 4 /* data length*/)                     // stack: [size]
        appropriateFields.forEach {// index, field ->
            val fieldType = it.genericType.substitute(getTypeArgumentsFor(it.declaringClass))
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(fieldType)
            loadField(getSerializerField(fieldSerializerType))                  // stack: [size, fieldSerializer]
            loadContext()                                                       // stack: [size, fieldSerializer, context]
            loadObj()                                                           // stack: [size, fieldSerializer, context, obj]
            visitFieldInsn(Opcodes.GETFIELD, klass.jvmType.name,
                    it.name, it.type.jvmType.signature)                         // stack: [size, fieldSerializer, context, obj.field]
            cast(it.type.jvmType, fieldType.jvmType)
            callVirtual(fieldSerializerType, "countSize",
                    listOf(WriteContext::class.java, fieldType), Int::class.java) // stack: [size, fieldSerializer.countSize(context, obj.field)]
            visitInsn(Opcodes.IADD)                                             // stack: [size + fieldSerializer.countSize(context, obj.field) => size]
        }
        ret<Int>()                                                              // return size; stack: []
        return 4
    }

    override fun MethodVisitor.writeNotNull(): Int {
        writeSafe<Byte> { visitLdcInsn(GroBufTypeCode.Tuple.value) }             // this.writeByteSafe(result, index, Tuple]; stack: []
        loadIndex<WriteContext>()                                                // stack: [index]
        val start = declareLocal<Int>()
        saveToLocal(start)                                                       // start = index; stack: []
        increaseIndexBy<WriteContext>(4)                                         // index += 4; stack: []
        appropriateFields.forEach {
            val fieldType = it.genericType.substitute(getTypeArgumentsFor(it.declaringClass))
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(fieldType)
            loadField(getSerializerField(fieldSerializerType))                   // stack: [fieldSerializer]
            loadContext()                                                        // stack: [fieldSerializer, context]
            loadObj()                                                            // stack: [fieldSerializer, context, obj]
            visitFieldInsn(Opcodes.GETFIELD, klass.jvmType.name,
                    it.name, it.type.jvmType.signature)                          // stack: [fieldSerializer, context, obj.field]
            cast(it.type.jvmType, fieldType.jvmType)
            callVirtual(fieldSerializerType, "write",
                    listOf(WriteContext::class.java, fieldType), Void::class.java) // fieldSerializer.write(context, obj.field); stack: []
        }

        writeLength(start)

        ret<Void>()
        return 6
    }

    override fun MethodVisitor.readNotNull(): Int {
        assertTypeCode(GroBufTypeCode.Tuple)

        increaseIndexBy<ReadContext>(4)                             // skip data length: index += 4; stack: []

        visitTypeInsn(Opcodes.NEW, klass.jvmType.name)              // stack: [new klass() => result]
        visitInsn(Opcodes.DUP)                                      // stack: [result, result]

        appropriateFields.forEach {
            val fieldType = it.genericType.substitute(getTypeArgumentsFor(it.declaringClass))
            val fieldSerializerType = fragmentSerializerCollection.getFragmentSerializerType(fieldType)
            loadField(getSerializerField(fieldSerializerType))      // stack: [result, result, args, fieldSerializer]
            loadContext()                                           // stack: [result, result, args, fieldSerializer, context]
            callVirtual(fieldSerializerType, "read",
                    listOf(ReadContext::class.java), fieldType)     // stack: [result, result, args, fieldSerializer.read(context) => args]
            cast(fieldType.jvmType, it.type.jvmType)
        }

        val constructor = klass.constructors.single()
        assert(constructor.parameterCount == appropriateFields.size,
                { "A tuple must has exactly one constructor accepting as arguments all the tuple's public fields" })
        ctorCall(klass.jvmType, appropriateFields.map { it.type }) // result.ctor(args); stack: [result]

        ret(klass)                                                 // return result; stack: []
        return 3 + appropriateFields.size
    }

    private val appropriateFields by lazy {
        klass.fields.filter { Modifier.isPublic(it.modifiers) }
                    .filterNot { Modifier.isStatic(it.modifiers) }
    }

    private fun getSerializerField(type: JVMType) = serializerFields.getOrPut(type) {
        defineField("${type.name.toJVMIdentifier()}_serializer", type, null, true)
    }

    private val serializerFields = mutableMapOf<JVMType, Int>()
}