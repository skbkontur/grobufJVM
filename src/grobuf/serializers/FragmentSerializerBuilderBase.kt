package grobuf.serializers

import grobuf.*
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal abstract class FragmentSerializerBuilderBase(protected val fragmentSerializerCollection: FragmentSerializerCollection,
                                                      protected val klass: Class<*>)
    : ClassBuilder<FragmentSerializer<*>>(
        className      = "${klass.jvmType.toJVMIdentifier()}_Serializer",
        superClassType = FragmentSerializer::class.jvmType) {

    override fun buildBody() {
        buildCountMethod()
        buildWriteMethod()
        buildReadMethod()
    }

    protected fun MethodVisitor.loadObj() {
        loadSlot(klass, 2)
    }

    protected fun MethodVisitor.loadContext() {
        loadSlot<Any>(1)
    }

    //---------- Writing -------------------------------------------------------//

    protected fun MethodVisitor.loadResult() {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, WriteContext::class.jvmType, "result", ByteArray::class.jvmSignature)
    }

    protected inline fun <reified T> MethodVisitor.loadIndex() {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, T::class.jvmType, "index", Int::class.jvmSignature)
    }

    protected inline fun <reified T> MethodVisitor.increaseIndexBy(value: Int) {
        loadContext()
        loadIndex<T>()
        visitLdcInsn(value)
        visitInsn(Opcodes.IADD)
        visitFieldInsn(Opcodes.PUTFIELD, T::class.jvmType, "index", Int::class.jvmSignature)
    }

    protected inline fun <reified T> MethodVisitor.increaseIndexBy(valueLoader: MethodVisitor.() -> Unit) {
        loadContext()
        loadIndex<T>()
        valueLoader()
        visitInsn(Opcodes.IADD)
        visitFieldInsn(Opcodes.PUTFIELD, T::class.jvmType, "index", Int::class.jvmSignature)
    }

    protected fun MethodVisitor.writeSafe(klass: Class<*>, valueLoader: MethodVisitor.() -> Unit) {
        val jvmPrimitive = klass.jvmPrimitiveType ?: error("Expected a primitive but was $klass")
        loadThis()                                                                       // stack: [this]
        loadResult()                                                                     // stack: [this, result]
        loadIndex<WriteContext>()                                                        // stack: [this, result, index]
        valueLoader()                                                                    // stack: [this, result, index, value]
        callVirtual(className, "write${jvmPrimitive.name}Safe",
                listOf(ByteArray::class.java, Int::class.java, klass), Void::class.java) // this.write<type>Safe(result, index, value); stack: []
        increaseIndexBy<WriteContext>(jvmPrimitive.size)                                 // index += type.size; stack: []
    }

    protected inline fun <reified T> MethodVisitor.writeSafe(noinline valueLoader: MethodVisitor.() -> Unit) {
        val jvmPrimitive = T::class.jvmPrimitiveType ?: error("Expected a primitive but was ${T::class}")
        loadThis()                                                                        // stack: [this]
        loadResult()                                                                      // stack: [this, result]
        loadIndex<WriteContext>()                                                         // stack: [this, result, index]
        valueLoader()                                                                     // stack: [this, result, index, value]
        callVirtual3<ByteArray, Int, T, Void>(className, "write${jvmPrimitive.name}Safe") // this.write<type>Safe(result, index, value); stack: []
        increaseIndexBy<WriteContext>(jvmPrimitive.size)                                  // index += type.size; stack: []
    }

    protected fun MethodVisitor.writeLength(startSlot: Int) {
        loadThis()                                                               // stack: [this]
        loadResult()                                                             // stack: [this, result]
        loadSlot<Int>(startSlot)                                                 // stack: [this, result, start]
        loadIndex<WriteContext>()                                                // stack: [this, result, start, index]
        loadSlot<Int>(startSlot)                                                 // stack: [this, result, start, index, start]
        visitLdcInsn(4)                                                          // stack: [this, result, start, index, start, 4]
        visitInsn(Opcodes.IADD)                                                  // stack: [this, result, start, index, start + 4]
        visitInsn(Opcodes.ISUB)                                                  // stack: [this, result, start, index - start - 4 => length]
        callVirtual3<ByteArray, Int, Int, Void>(className, "writeIntUnsafe")     // this.writeIntUnsafe(result, start, length); stack: []
    }

    private fun buildCountMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "countSize",
                listOf(WriteContext::class.java, klass), Int::class.java).run {
            if (klass.isReference) {
                loadObj()                                      // stack: [obj]
                val notNullLabel = Label()
                visitJumpInsn(Opcodes.IFNONNULL, notNullLabel) // if (obj != null) goto notNull; stack: []
                visitLdcInsn(1)                                // stack: [1]
                ret<Int>()                                     // return 1; stack: []
                visitLabel(notNullLabel)
            }
            countSizeNotNull()
            visitEnd()
        }
        if (klass != Any::class.java) {
            buildBridge(
                    name          = "countSize",
                    argumentTypes = listOf(
                            WriteContext::class.java.notErased(),
                            klass.erased()
                    ),
                    returnType    = Int::class.java.notErased()
            )
        }
    }

    protected abstract fun MethodVisitor.countSizeNotNull()

    private fun buildWriteMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "write",
                        listOf(WriteContext::class.java, klass), Void::class.java).run {
            if (klass.isReference) {
                loadObj()                                                    // stack: [obj]
                val notNullLabel = Label()
                visitJumpInsn(Opcodes.IFNONNULL, notNullLabel)               // if (obj != null) goto notNull; stack: []
                writeSafe<Byte> { visitLdcInsn(GroBufTypeCode.Empty.value) } // this.writeByteSafe(context.result, context.index, Empty); stack: []
                ret<Void>()                                                  // return; stack: []
                visitLabel(notNullLabel)
            }
            writeNotNull()
            visitEnd()
        }
        if (klass != Any::class.java) {
            buildBridge(
                    name          = "write",
                    argumentTypes = listOf(
                            WriteContext::class.java.notErased(),
                            klass.erased()
                    ),
                    returnType    = Void::class.java.notErased()
            )
        }
    }

    protected abstract fun MethodVisitor.writeNotNull()

    //---------- Reading -----------------------------------------------------------//

    protected fun MethodVisitor.loadData() {
        loadContext()
        visitFieldInsn(Opcodes.GETFIELD, ReadContext::class.jvmType, "data", ByteArray::class.jvmSignature)
    }

    protected fun MethodVisitor.readSafe(klass: Class<*>) {
        val jvmPrimitive = klass.jvmPrimitiveType ?: error("Expected a primitive but was $klass")
        loadThis()                                                     // stack: [this]
        loadData()                                                     // stack: [this, data]
        loadIndex<ReadContext>()                                       // stack: [this, data, index]
        callVirtual(className, "read${jvmPrimitive.name}Safe",
                listOf(ByteArray::class.java, Int::class.java), klass) // stack: [this.read<type>Safe(result, index)]
        increaseIndexBy<ReadContext>(jvmPrimitive.size)                // index += type.size; stack: [this.read<type>Safe(result, index)]
    }

    protected inline fun <reified T> MethodVisitor.readSafe() {
        val jvmPrimitive = T::class.jvmPrimitiveType ?: error("Expected a primitive but was ${T::class}")
        loadThis()                                                                 // stack: [this]
        loadData()                                                                 // stack: [this, data]
        loadIndex<ReadContext>()                                                   // stack: [this, data, index]
        callVirtual2<ByteArray, Int, T>(className, "read${jvmPrimitive.name}Safe") // stack: [this.read<type>Safe(result, index)]
        increaseIndexBy<ReadContext>(jvmPrimitive.size)                            // index += type.size; stack: [this.read<type>Safe(result, index)]
    }

    protected fun MethodVisitor.loadTypeCode() {
        loadSlot<Int>(2)
    }

    protected fun MethodVisitor.loadDefault(klass: Class<*>) {
        when (klass.jvmPrimitiveType) {
            JVMPrimitive.Byte,
            JVMPrimitive.Short,
            JVMPrimitive.Int,
            JVMPrimitive.Char,
            JVMPrimitive.Boolean -> visitLdcInsn(0)
            JVMPrimitive.Long -> visitLdcInsn(0L)
            JVMPrimitive.Float -> visitLdcInsn(0.0f)
            JVMPrimitive.Double -> visitLdcInsn(0.0)
            else -> visitInsn(Opcodes.ACONST_NULL)
        }
    }

    protected fun MethodVisitor.assertTypeCode(expectedTypeCode: GroBufTypeCode) {
        loadTypeCode()                                               // stack: [typeCode]
        visitLdcInsn(expectedTypeCode.value)                         // stack: [typeCode, expectedTypeCode]
        val okLabel = Label()
        visitJumpInsn(Opcodes.IF_ICMPEQ, okLabel)                    // if (typeCode == expectedTypeCode) goto ok; stack: []
        loadThis()                                                   // stack: [this]
        loadTypeCode()                                               // stack: [this, typeCode]
        loadContext()                                                // stack: [this, typeCode, context]
        callVirtual2<Int, ReadContext, Void>(className, "skipValue") // this.skipValue(typeCode, context)]
        loadDefault(klass)
        ret(klass)

        visitLabel(okLabel)
    }

    private fun buildReadMethod() {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, "read",
                listOf(ReadContext::class.java), klass).run {
            readSafe<Byte>()                                    // stack: [this.readByteSafe(context.data, context.index) => typeCode]
            visitInsn(Opcodes.DUP)                              // stack: [typeCode, typeCode]
            saveToSlot<Int>(2)                                  // slot_2 = typeCode; stack: [typeCode]
            val notEmptyLabel = Label()
            visitLdcInsn(GroBufTypeCode.Empty.value)            // stack: [typeCode, Empty]
            visitJumpInsn(Opcodes.IF_ICMPNE, notEmptyLabel)     // if (type != Empty) goto notEmpty; stack: []

            loadDefault(klass)
            ret(klass)                                          // return default(type); stack: []

            visitLabel(notEmptyLabel)
            loadThis()                                          // stack: [this]
            loadTypeCode()                                      // stack: [this, typeCode]
            callVirtual1<Int, Void>(className, "checkTypeCode") // this.checkTypeCode(typeCode); stack: []

            readNotNull()
            visitEnd()
        }
        if (klass != Any::class.java) {
            buildBridge(
                    name          = "read",
                    argumentTypes = listOf(
                            ReadContext::class.java.notErased()
                    ),
                    returnType    = klass.erased()
            )
        }
    }

    protected abstract fun MethodVisitor.readNotNull()

    //---------- Bridges building ---------------------------------------------------------//

    private class PossiblyErasedType(val klass: Class<*>, val erased: Boolean)

    private fun Class<*>.erased() = PossiblyErasedType(this, true)

    private fun Class<*>.notErased() = PossiblyErasedType(this, false)

    private val PossiblyErasedType.actualType get() = if (erased) Any::class.java else klass

    private fun buildBridge(name: String, argumentTypes: List<PossiblyErasedType>, returnType: PossiblyErasedType) {
        classWriter.defineMethod(Opcodes.ACC_PUBLIC, name,
                argumentTypes.map { it.actualType }, returnType.actualType).run {
            loadThis()
            argumentTypes.forEachIndexed { index, argumentType ->
                loadSlot(argumentType.actualType, index + 1)
                if (argumentType.erased)
                    castObjectTo(argumentType.klass.jvmType)
            }
            callVirtual(className, name, argumentTypes.map { it.klass }, returnType.klass)
            if (returnType.erased)
                castToObject(returnType.klass.jvmType)
            ret(returnType.actualType)
            visitMaxs(argumentTypes.size + 1, argumentTypes.size + 1)
            visitEnd()
        }
    }
}